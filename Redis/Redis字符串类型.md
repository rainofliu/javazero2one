[toc]



# 基本介绍

Redis 数据库里面的每个键值对（key-value pair）都是由对象（object）组成的,其中:

+ 数据库键总是一个字符串对象（string object）;
+ 而数据库键的值则可以是**字符串对象、列表对象（list object）、哈希对象（hash object）、集合对象（set object）、有序集合对象（sorted set object）这五种对象中的其中一种。**

> 字符串类型是Redis最基础的数据结构。

首先键都是字符串类型，而且 其他几种数据结构都是在字符串类型基础上构建的

字符串类型的实际值可以为：

+ 简单字符串
+ 复杂字符串
  + JSON
  + XML
+ 数字
  + 整数
  + 浮点数
+ 二进制
  + 图片
  + 视频
  + 音频

> 单个字符串不能超过512MB

# 命令

## 设置值

`set key value [ex seconds] [px milliseconds] [nx|xx] `

set命令有几个选项： 

+ `ex seconds`：为键设置秒级过期时间。 
+ `px milliseconds`：为键设置毫秒级过期时间。 
+ `nx`：键必须不存在，才可以设置成功，用于添加。 
  + `setnx`
    + 可以用来实现分布式锁

+ `xx`：与nx相反，键必须存在，才可以设置成功，用于更新
  + `setxx`

## 批量设置值

+ `mset key value [key value ...]` 

```shell
127.0.0.1:6379> mset a 1 b 2 c 3
OK
127.0.0.1:6379> mget a b c
1) "1"
2) "2"
3) "3"
```

> 批量操作 可以通过一次网络连接连续发送多条命令，我们知道在分布式场景中，网络是不可靠的，批量操作可以节省多次网络IO时间，提升性能。
>
> + 联想到MyBatis中，批量插入操作 比常规多次执行插入  性能要高

## 计数

+ incr key

  > incr命令用于对值做自增操作，返回结果分为三种情况： 

  + 值不是整数，返回错误。 

    ```shell
    > set not-number hello
    OK
    127.0.0.1:6379> incr not-number
    (error) ERR value is not an integer or out of range
    127.0.0.1:6379>
    ```

  + 值是整数，返回自增后的结果。 

  + 键不存在，按照值为0自增，返回结果为1。

    ```shell
    127.0.0.1:6379> incr not
    (integer) 1
    127.0.0.1:6379> get not
    "1"
    ```

    > 这里not 这个key是不存在的

  > 除了`incr`命令，Redis提供了`decr`（自减）、`incrby`（自增指定数字）、 
  >
  > `decrby`（自减指定数字）、`incrbyfloat`（自增浮点数）： 

**很多存储系统和编程语言内部使用CAS机制实现计数功能，会有一定的 CPU开销**，但在Redis中完全不存在这个问题，因为Redis是单线程架构，任 何命令到了Redis服务端都要顺序执行

> 单线程架构的优点

# 内部编码

字符串类型的内部编码有3种： 

1. int：8个字节的长整型。 

   ```shell
   127.0.0.1:6379> set key 6379
   OK
   127.0.0.1:6379> object encoding key
   "int"
   ```

2. embstr：小于等于39个字节的字符串。

   ```shell
   127.0.0.1:6379> set key "hello,world"
   OK
   127.0.0.1:6379> object encoding key
   "embstr"
   ```

3. raw：大于39个字节的字符串。 

> Redis会根据当前值的类型和长度决定使用哪种内部编码实现。

# 典型使用场景

## 缓存

+ Redis基于内存操作，天然支撑高并发，可以用来作为缓存层，大大缓解了DB层的压力
  + MySQL扛不住高并发

> 设计合理的键名

## 计数

+ 使用Redis作为视频播放数计数

## 分布式Session

一个分布式Web服务将用户的Session信息（例如用户登录信息）保存在各自服务器中，这样会造成一个问题，出于负载均衡的考虑，分布式服务会将用户的访问均衡到不同服务器上，

+ 可以使用Redis将用户的Session进行集中管理，在这种模式下**只要保证Redis是高可用和扩展性**的，每次用户 

  更新或者查询登录信息都直接从Redis中集中获取

## 限速

很多应用出于安全的考虑，会在每次进行登录时，让用户输入手机验证 码，从而确定是否是用户本人。但是为了短信接口不被频繁访问，会限制用 户每分钟获取验证码的频率，例如一分钟不能超过5次，如

> 手机验证码接口访问评率的限制，防止接口被盗刷

# 字符串内部结构 -SDS

Redis 中的字符串是可以修改的字符串，在内存中它是以字节数组的形式存在的。

**我们知道 C 语言里面的字符串标准形式是以 NULL 作为结束符，但是在 Redis 里面字符串不是这么表示的。因为要获取 NULL 结尾的字符串的长度使用的是 `strlen` 标准库函数，这个函数的算法复杂度是 O(n)，它需要对字节数组进行遍历扫描，作为单线程的 Redis 表示承受不起。**

> Redis使用了一种名为**简单动态字符串（simple dynamic string -> SDS）**的抽象类型，并将`SDS`作为Redis的默认字符串表示

**在Redis数据库里面，包含字符串值的键值对在底层都是由SDS来实现的**

```shell
127.0.0.1:6379> set msg "hello,world"
OK
```

> 执行上面的命令后，Redis将在数据库中创建一个新的键值对（值是字符串类型）
>
> + 键是一个字符串对象，对象的底层实现是一个保存着字符串“msg”的`SDS`
> + 值也是一个字符串对象，对象的底层实现是一个保存着字符串“hello,world”的`SDS`

除了用来保存字符串中的值以外，SDS还用作缓冲区（buffer）：

+ AOF缓冲区

  > AOF是Redis的一种持久化技术

+ 客户端状态中的缓冲区

## 源码解读

`sds.h`

```c
#ifndef __SDS_H
#define __SDS_H

/*
 * 最大预分配长度
 */
#define SDS_MAX_PREALLOC (1024*1024)

#include <sys/types.h>
#include <stdarg.h>

/*
 * 类型别名，用于指向 sdshdr 的 buf 属性
 */
typedef char *sds;

/*
 * 保存字符串对象的结构
 */
struct sdshdr {
    
    // buf 中已占用空间的长度
    // 也就是SDS所保存字符串的长度
    int len;

    // buf 中剩余可用空间的长度
    int free;

    // 数据空间
    char buf[];
};

/*
 * 返回 sds 实际保存的字符串的长度
 *
 * T = O(1)
 */
static inline size_t sdslen(const sds s) {
    struct sdshdr *sh = (void*)(s-(sizeof(struct sdshdr)));
    return sh->len;
}

/*
 * 返回 sds 可用空间的长度
 *
 * T = O(1)
 */
static inline size_t sdsavail(const sds s) {
    struct sdshdr *sh = (void*)(s-(sizeof(struct sdshdr)));
    return sh->free;
}

sds sdsnewlen(const void *init, size_t initlen);
sds sdsnew(const char *init);
sds sdsempty(void);
size_t sdslen(const sds s);
sds sdsdup(const sds s);
void sdsfree(sds s);
size_t sdsavail(const sds s);
sds sdsgrowzero(sds s, size_t len);
sds sdscatlen(sds s, const void *t, size_t len);
sds sdscat(sds s, const char *t);
sds sdscatsds(sds s, const sds t);
sds sdscpylen(sds s, const char *t, size_t len);
sds sdscpy(sds s, const char *t);

sds sdscatvprintf(sds s, const char *fmt, va_list ap);
#ifdef __GNUC__
sds sdscatprintf(sds s, const char *fmt, ...)
    __attribute__((format(printf, 2, 3)));
#else
sds sdscatprintf(sds s, const char *fmt, ...);
#endif

sds sdscatfmt(sds s, char const *fmt, ...);
sds sdstrim(sds s, const char *cset);
void sdsrange(sds s, int start, int end);
void sdsupdatelen(sds s);
void sdsclear(sds s);
int sdscmp(const sds s1, const sds s2);
sds *sdssplitlen(const char *s, int len, const char *sep, int seplen, int *count);
void sdsfreesplitres(sds *tokens, int count);
void sdstolower(sds s);
void sdstoupper(sds s);
sds sdsfromlonglong(long long value);
sds sdscatrepr(sds s, const char *p, size_t len);
sds *sdssplitargs(const char *line, int *argc);
sds sdsmapchars(sds s, const char *from, const char *to, size_t setlen);
sds sdsjoin(char **argv, int argc, char *sep);

/* Low level functions exposed to the user API */
sds sdsMakeRoomFor(sds s, size_t addlen);
void sdsIncrLen(sds s, int incr);
sds sdsRemoveFreeSpace(sds s);
size_t sdsAllocSize(sds s);

#endif
```

> 我们可以近似地将SDS理解成Java中的`ArrayList`，基于动态数组。

![](https://liutianruo-2019-go-go-go.oss-cn-shanghai.aliyuncs.com/images/SDS原理1.png)

​				**C字符串和 SDS 之间的区别**

| SDS                                         | C字符串                                    |
| ------------------------------------------- | ------------------------------------------ |
| 获取字符串长度的复杂度为O(1）               | 获取字符串长度的复杂度为O(M)               |
| API是安全的,不会造成缓冲区溢出              | API是不安全的,可能会造成缓冲区溢出         |
| 修改字符串长度N 次最多需要执行N次内存重分配 | 修改字符串长度N次必然需要执行N次内存重分配 |
| 可以保存文本或者二进制数据                  | 只能保存文本数据                           |
| 可以使用一部分<string.h>库中的函数          | 可以使用所有<string.h>库中的函数           |









