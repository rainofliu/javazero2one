[toc]

本文记录学习Redis `hash`(**字典**)的笔记。

# 命令

`hset key field value` 

```shell
127.0.0.1:6379> hset user:1 name ajin
(integer) 1
127.0.0.1:6379> hget user:1 name
"ajin"
```

```java
127.0.0.1:6379> hset user:1 age 18
(integer) 1
127.0.0.1:6379> hdel user:1 age
(integer) 1
127.0.0.1:6379>
```

> 删除 age这个field对应的value

# 内部编码

+ `ziplist`（**压缩列表**）：当哈希类型元素个数小于hash-max-ziplist-entries 配置（默认512个）、同时所有值都小于hash-max-ziplist-value配置（默认64 字节）时，Redis会使用ziplist作为哈希的内部实现，ziplist使用更加紧凑的 结构实现多个元素的连续存储，所以在节省内存方面比hashtable更加优秀。 hashtable（哈希表）：当哈希类型无法满足ziplist的条件时，Redis会使 用hashtable作为哈希的内部实现，因为此时ziplist的读写效率会下降，而 hashtable的读写时间复杂度为O（1）。
+ `hashtable`（**哈希表**）：当哈希类型无法满足ziplist的条件时，Redis会使 用hashtable作为哈希的内部实现，因为此时ziplist的读写效率会下降，而 hashtable的读写时间复杂度为O（1）。

# 使用场景

1. 缓存用户信息
   + 如果使用多个string 浪费key value（空间浪费）
   + 用一个string（反序列化后对应一个完整的Java对象）来缓存用户信息，则会浪费内存空间，导致string过大，甚至会引起网络IO获取value时间过长的情况
   + 和MySQL这样的关系型数据库相比，hash可以自定义要保存的字段值(field and field value) 
     + 关系型数据库  如果要加一个字段，则该表中的每一行记录都会包含这个字段，而Redis的hash类型则不是这样 keyA保存的用户信息中可以包含age  但是不包含city等字段（hash想存储用户的哪些信息就存储哪些信息）

# 底层实现

> 这里我们先介绍了hash的底层实现之一  —— **哈希表**。

## 哈希表

`dict.h/dictht `

```c
/*
 * 哈希表
 *
 * 每个字典都使用两个哈希表，从而实现渐进式 rehash 。
 */
typedef struct dictht {
    
    // 哈希表数组
    dictEntry **table;

    // 哈希表大小
    unsigned long size;
    
    // 哈希表大小掩码，用于计算索引值
    // 总是等于 size - 1
    unsigned long sizemask;

    // 该哈希表已有节点的数量
    unsigned long used;

} dictht;

```

## 哈希表结点

```c
/*
 * 哈希表节点
 */
typedef struct dictEntry {
    
    // 键
    void *key;

    // 值
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
    } v;

    // 指向下个哈希表节点，形成链表
    struct dictEntry *next;

} dictEntry;
```

## 字典

```c
/*
 * 字典
 */
typedef struct dict {

    // 类型特定函数
    dictType *type;

    // 私有数据
    void *privdata;

    // 哈希表
    dictht ht[2];

    // rehash 索引
    // 当 rehash 不在进行时，值为 -1
    int rehashidx; /* rehashing not in progress if rehashidx == -1 */

    // 目前正在运行的安全迭代器的数量
    int iterators; /* number of iterators currently running */

} dict;
```

+ 一个字典对应两个哈希表，每个哈希表中存着多个哈希表结点

## 哈希冲突

我们知道在哈希的世界里，产生哈希冲突是很正常的事情，那么Redis是如何处理哈希冲突的呢？

> Redis使用**链地址法**来解决哈希冲突，每个哈希表结点都有一个next指针，多个哈希表结点可以通过next指针构成一个单向链表，这样就解决了哈希冲突（和`HashMap`很像的）

## rehash

我们知道HashMap中有一个属性叫负载因子loadFactor，当元素个数达到loadFactor*哈希表的大小时，就会扩容，扩容就会进行rehash，因为哈希表的容量扩大了，我们需要对每个元素进行hash运算，确定元素保存在新的哈希表中的位置。这样就是rehash的过程。

**同样在Redis中的哈希表也要去面对rehash。**

> 随着操作的不断执行,哈希表保存的键值对会逐渐地增多或者减少,为了让哈希表的负载因子（load factor）维持在一个合理的范围之内,当哈希表保存的键值对数量太多或者太少时,程序需要**对哈希表的大小进行相应的扩展或者收缩**。

注意，Redis这里有收缩的过程，`HashMap`中是没有的。

### 哈希表的扩展与收缩

当以下条件中的任意一个被满足时,程序会自动开始对哈希表执行扩展操作:

1. 服务器目前没有在执行`BGSAVE`命令或者`BGREWRITEAOF`命令,并且哈希表的负载因子大于等于1。

2. 服务器目前正在执行 `BGSAVE`命令或者`BGREWRITEAOF`命令,并且哈希表的负载因子大于等于5。
   
   > 其中哈希表的负载因子可以通过公式:
   > #负载因子=哈希表巳保存节点数量/哈希表大小
   > load_factor = ht[0].used / ht[0].size
   > 计算得出。
   > 例如,对于一个大小为4,包含4个键值对的哈希表来说,这个哈希表的负载因子为:load_factor=4/4=1
   > '例如,对于一个大小为512,包含 256个键值对的哈希表来说,这个哈希表的负载因子为:
   > load_factor= 256/512= 0.5
   > 根据BGSAVE命令或BGREWRITEAOF命令是否正在执行,服务器执行扩展操作所需的负载因子并不相同,这是因为在执行BGSAVE命令或BGREWRITEAOF命令的过程中,Redis需要创建当前服务器进程的子进程,而大多数操作系统都采用写时复制（copy-on-write）技术来优化子进程的使用效率,所以在子进程存在期间,服务器会提高执行扩展操作所需的负载因子,从而尽可能地避免在子进程存在期间进行哈希表扩展操作,这可以避免不必要的内存写人操作,最大限度地节约内存。
   > 另一方面,当哈希表的负载因子小于0.1时,程序自动开始对哈希表执行收缩操作。
## 渐进式 rehash

   上一节说过,扩展或收缩哈希表需要将ht[0]里面的所有键值对rehash 到ht[1]里面,但是,这个rehash动作并不是一次性、集中式地完成的,而是分多次、渐进式地完成的。
   这样做的原因在于,如果 ht[0]里只保存着四个键值对,那么服务器可以在瞬间就将这些键值对全部 rehash 到 ht[1;但是**,如果哈希表里保存的键值对数量不是四个,而是四百万、四千万甚至四亿个键值对,那么要一次性将这些键值对全部rehash到ht[1]的话, 庞大的计算量可能会导致服务器在一段时间内停止服务**。

> 因此,为了避免rehash 对服务器性能造成影响,服务器不是一次性将ht【0]里面的所有键值对全部rehash到ht[1】,而是分多次、渐进式地将nt[0]里面的键值对慢慢地rehash 到ht[1]。

### 渐进式rehash 执行期间的哈希表操作

因为在进行渐进式 rehash的过程中,**字典会同时使用ht[0]和ht[1]两个哈希表,** 

> 所以在渐进式rehash 进行期间,字典的删除（delete）、查找（find）、更新（update）等操作会在两个哈希表上进行。例如,要在字典里面查找一个键的话,程序会先在ht[0]里面进行查找,如果没找到的话,就会继续到ht[1]里面进行查找,诸如此类。
> **另外,在渐进式rehash执行期间,新添加到字典的键值对一律会被保存到ht[1〕里面, 而ht[0]则不再进行任何添加操作,这一措施保证了ht[0〕包含的键值对数量会只减不增,并随着 rehash操作的执行而最终变成空表。**

# 回顾总结

1. 字典被广泛用于实现 Redis 的各种功能,其中包括数据库和哈希键。
2. Redis中的字典使用哈希表作为底层实现,**每个字典带有两个哈希表,一个平时使用,另一个仅在进行 rehash 时使用。**
3. 当字典被用作数据库的底层实现,或者哈希键的底层实现时,Redis使用MurmurHash2 算法来计算键的哈希值
4. 哈希表使用**链地址法**来解决键冲突,被分配到同一个索引上的多个键值对会连接成一个**单向链表**。
5. 在对哈希表**进行扩展或者收缩操作时**,程序需要将现有哈希表包含的所有键值对rehash到新哈希表里面,并且**这个rehash 过程并不是一次性地完成的,而是渐进式地完成的。**

# 参考内容

1. 《Redis开发与运维》
2. 《Redis设计与实现第二版》