# 简单动态字符串（SDS）

结构体定义

```c
struct sdshdr{
    // 记录buf数组中已使用的字节的数量
    // 等于SDS所保存字符串的长度
    int len;
    // buf中未使用的字节数量
    int free;
    // 字节数组 用于保存字符串
    char buf[];
}
```

 ![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/SDS%E7%A4%BA%E4%BE%8B%E5%9B%BE.png)

* free：没有空余空间
* len：字符串的长度
* buf：字符串

C字符串以空字符结尾。

## SDS与C字符串对比的有点

C语言使用长度为N+1的字符数组来表示长度为N的字符串，并且字符数组的最后一个元素总是空字符'\0'。

### 获取字符串长度的复杂度低

C语言是O(N)。

 ![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/C%E8%AF%AD%E8%A8%80%E8%AE%A1%E7%AE%97%E5%AD%97%E7%AC%A6%E4%B8%B2%E9%95%BF%E5%BA%A6%E7%9A%84%E6%96%B9%E5%BC%8F.png)



**SDS在len属性中记录了SDS本身的长度，所以获取一个SDS长度的复杂度仅为O（1）**



### 杜绝缓冲区溢出

缓冲区溢出，就是自己没分配足够的内存，把别的字符串给修改了。

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/C%E8%AF%AD%E8%A8%80%E5%AD%97%E7%AC%A6%E4%B8%B2%E7%9A%84%E7%BC%93%E5%86%B2%E5%8C%BA%E6%BA%A2%E5%87%BA%E9%97%AE%E9%A2%98.png)

当SDS API需要对SDS进行修改时，**API会先检查SDS的空间是否满足修改所需的要求，如果不满足的话，API会自动将SDS的空间扩展至执行修改所需的大小**，然后才执行实际的修改操作，所以使用SDS既不需要手动修改SDS的空间大小，也不会出现缓冲区溢出问题。

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/SDS%E6%8B%BC%E6%8E%A5%E5%AD%97%E7%AC%A6%E4%B8%B2%E7%9A%84%E6%89%A9%E5%AE%B9%E6%93%8D%E4%BD%9C.png)

### 减少修改字符串时带来的内存重分配次数

通过**未使用空间**，SDS实现了**空间预分配**和**惰性空间释放**两种优化策略

#### 1. 空间预分配

1. 如果对SDS进行修改之后，**SDS的长度（也即是len属性的值）将小于1MB**，那么程序分配和len属性同样大小的未使用空间，这时SDS len属性的值将和free属性的值相同。举个例子，如果进行修改之后，SDS的len将变成13字节，那么程序也会分配13字节的未使用空间，SDS的buf数组的实际长度将变成13+13+1=27字节（额外的一字节用于保存空字符）
2. 如果对SDS进行修改之后，SDS的长度将大于等于1MB，**那么程序会分配1MB的未使用空间**。举个例子，如果进行修改之后，SDS的len将变成30MB，那么程序会分配1MB的未使用空间，SDS的buf数组的实际长度将为30MB+1MB+1byte

通过空间预分配策略，Redis可以减少连续执行**字符串增长操作**所需的内存重分配次数。

#### 2. 惰性空间释放

惰性空间释放用于优化SDS的**字符串缩短操作**。

当SDS的API需要缩短SDS保存的字符串时，**程序并不立即使用内存重分配来回收缩短后多出来的字节**，而是**使用free属性将这些字节的数量记录起来，并等待将来使用**。

### 二进制安全

C字符串中的字符必须符合某种编码（比如ASCII），并且**除了字符串的末尾之外，字符串里面不能包含空字符**，否则最先被程序读入的空字符将被误认为是字符串结尾，这些限制使得C字符串只能保存文本数据，而不能保存像图片、音频、视频、压缩文件这样的二进制数据。

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E4%BF%9D%E5%AD%98%E4%BA%86%E7%89%B9%E6%AE%8A%E6%95%B0%E6%8D%AE%E6%A0%BC%E5%BC%8F%E7%9A%84SDS.png)

而使用SDS来保存之前提到的特殊数据格式就没有任何问题，因为**SDS使用len属性的值**而不是空字符来**判断字符串是否结束。**

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/C%E5%AD%97%E7%AC%A6%E4%B8%B2%E5%92%8CSDS%E4%B9%8B%E9%97%B4%E7%9A%84%E5%8C%BA%E5%88%AB.png)



# 链表

## 实现

### 链表节点

```c
typedef struct listNode {
    // 前置节点
    struct listNode * prev;
    // 后置节点
    struct listNode * next;
    //节点的值
    void * value;
}listNode;
```

### 链表

```c
typedef struct list {
    //
    表头节点
    listNode * head;
    //
    表尾节点
    listNode * tail;
    //
    链表所包含的节点数量
    unsigned long len;
    //
    节点值复制函数
    void *(*dup)(void *ptr);
    //
    节点值释放函数
    void (*free)(void *ptr);
    //
    节点值对比函数
    int (*match)(void *ptr,void *key);
} list;
```

 ![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E9%93%BE%E8%A1%A8%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84%E5%9B%BE.png)

双向链表

## 特性

* 双端：链表节点带有prev和next指针，获取某个节点的前置节点和后置节点的复杂度都是O（1）
* 无环：表头节点的prev指针和表尾节点的next指针都指向NULL，对链表的访问以NULL为终点。
* 带表头指针和表尾指针：通过list结构的head指针和tail指针，程序获取链表的表头节点和表尾节点的复杂度为O（1）
* 带链表长度计数器：程序使用list结构的len属性来对list持有的链表节点进行计数，程序获取链表中节点数量的复杂度为O（1）

# 字典

键值对就是用哈希表来保存的。全局哈希表。

## 结构

### 字典

```c
typedef struct dictht {
    //哈希表数组
    dictEntry **table;
    //哈希表大小
    unsigned long size;
    //哈希表大小掩码，用于计算索引值
    //总是等于size-1
    unsigned long sizemask;
    //该哈希表已有节点的数量
    unsigned long used;
} dictht;
```

### 哈希表节点

```c
typedef struct dictEntry {
    //键
    void *key;
    //值
    union{
        void *val;
        uint64_tu64;
        int64_ts64;
    } v;
    //指向下个哈希表节点，形成链表
    struct dictEntry *next;
} dictEntry;
```

* key属性保存着键值对中的键

* v属性则保存着键值对中的值
  * 键值对的值可以是**一个指针**，或者是一个uint64_t整数，又或者是一个int64_t整数

* next属性是指向另一个哈希表节点的指针，这个指针可以将多个哈希值相同的键值对连接在一次，以此来解决键冲突问题

### 哈希表

```c
typedef struct dict {
    //类型特定函数
    dictType *type;
    //私有数据
    void *privdata;
    //哈希表
    dictht ht[2];
    // rehash索引
    //当rehash不在进行时，值为-1
    in trehashidx; /* rehashing not in progress if rehashidx == -1 */
} dict;
```

#### 图示

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E5%AD%97%E5%85%B8%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84.png)

## 哈希算法

当要将一个新的键值对添加到字典里面时，程序需要先根据键值对的键计算出哈希值和索引值，然后再根据索引值，将包含新键值对的哈希表节点放到哈希表数组的指定索引上面。

```c
#使用字典设置的哈希函数，计算键key的哈希值
hash = dict-＞type-＞hashFunction(key);
#使用哈希表的sizemask属性和哈希值，计算出索引值
#根据情况不同，ht[x]可以是ht[0]或者ht[1]
index = hash & dict-＞ht[x].sizemask;
```

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E5%AD%97%E5%85%B8%E6%B7%BB%E5%8A%A0%E9%A2%84%E7%AE%97.png)

## 哈希冲突

链地址法

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E9%93%BE%E8%A1%A8%E8%A7%A3%E5%86%B3%E5%93%88%E5%B8%8C%E5%86%B2%E7%AA%81.png)

## rehash

1）为字典的ht[1]哈希表分配空间，这个哈希表的空间大小取决于要执行的操作，以及ht[0]当前包含的键值对数量（也即是ht[0].used属性的值）：

* 如果执行的是扩展操作，那么**ht[1]的大小为第一个大于等于ht[0].used*2的2 n**（2的n次方幂）；
* 如果执行的是收缩操作，那么ht[1]的大小为第一个大于等于ht[0].used的2 n。

2）将保存在ht[0]中的所有键值对rehash到ht[1]上面：rehash指的是重新计算键的哈希值和索引值，然后将键值对放置到ht[1]哈希表的指定位置上。

 <img src="https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/rehash%E8%BF%81%E7%A7%BB.png" style="zoom: 50%;" />

3）当ht[0]包含的所有键值对都迁移到了ht[1]之后（ht[0]变为空表），释放ht[0]，**将ht[1]设置为ht[0]，并在ht[1]新创建一个空白哈希表，为下一次rehash做准备。**

 <img src="https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E5%AE%8C%E6%88%90rehash%E4%B9%8B%E5%90%8E.png" style="zoom:50%;" />

## 哈希表的扩展与收缩

当以下条件中的任意一个被满足时，程序会自动开始对哈希表执行扩展操作：

1）服务器目前没有在执行BGSAVE命令或者BGREWRITEAOF命令，并且哈希表的**负载因子**大于等于1。

2）服务器目前正在执行BGSAVE命令或者BGREWRITEAOF命令，并且哈希表的负载因子大于等于5。



> 根据BGSAVE命令或BGREWRITEAOF命令是否正在执行，服务器执行扩展操作所需的负载因子并不相同，这是因为在执行BGSAVE命令或BGREWRITEAOF命令的过程中，**Redis需要创建当前服务器进程的子进程，而大多数操作系统都采用写时复制（copy-on-write）技术**来优化子进程的使用效率，所以在子进程存在期间，服务器会提高执行扩展操作所需的负载因子，从而尽可能地避免在子进程存在期间进行哈希表扩展操作，这可以**避免不必要的内存写入操作**，最大限度地节约内存

### 负载因子

```c
load_factor = ht[0].used / ht[0].size;
```

负载因子 = 哈希表节点个数/哈希表大小

<u>当哈希表的负载因子小于0.1时，程序自动开始对哈希表执行收缩操作</u>。

## 渐进式rehash

基本策略：rehash动作并不是一次性、集中式地完成的，而是分多次、渐进式地完成的。

基本步骤：

1. 为ht[1]分配空间，让字典同时持有ht[0]和ht[1]两个哈希表。
2. 在字典中维持一个**索引计数器变量rehashidx**，并将它的值设置为0，表示rehash工作正式开始。
3. 在rehash进行期间，每次对字典执行添加、删除、查找或者更新操作时，程序除了执行指定的操作以外，还会顺带将ht[0]哈希表在rehashidx索引上的所有键值对rehash到ht[1]，当rehash工作完成之后，**程序将rehashidx属性的值增一**。
4. 随着字典操作的不断执行，最终在某个时间点上，ht[0]的所有键值对都会被rehash至ht[1]，**这时程序将rehashidx属性的值设为-1，表示rehash操作已完成**。



在rehash期间：

字典的删除（delete）、查找（find）、更新（update）等操作会在两个哈希表上进行。例如，要在字典里面查找一个键的话，**程序会先在ht[0]里面进行查找，如果没找到的话，就会继续到ht[1]里面进行查找**，诸如此类。

注意：在渐进式rehash执行期间，**新添加到字典的键值对一律会被保存到ht[1]里面**，而ht[0]则不再进行任何添加操作，这一措施保证了ht[0]包含的键值对数量会只减不增，并随着rehash操作的执行而最终变成空表



# 跳表（skiplist）

**有序数据结构**，它通过在每个节点中维持多个指向其他节点的指针，从而达到快速访问节点的目的。

Redis只在两个地方用到了跳跃表，一个是实现**有序集合键（Sorted Set）**，另一个是在**集群节点中用作内部数据结构**

[参考文章](https://mp.weixin.qq.com/s?__biz=MzA4NTg1MjM0Mg==&mid=2657261425&idx=1&sn=d840079ea35875a8c8e02d9b3e44cf95&scene=21#wechat_redirect)

# 整数(intset)

整数集合（intset）是集合键的底层实现之一，**当一个集合只包含整数值元素，并且这个集合的元素数量不多时，Redis就会使用整数集合作为集合键的底层实现**

```c
redis＞ SADD numbers 1 3 5 7 9
(integer) 5
redis＞ OBJECT ENCODING numbers
"intset"
```

intset可以保存类型为int16_t、int32_t或者int64_t的整数值，并且保证集合中不会出现重复元素

## 实现

结构体：

```c
typedef struct intset {
    //编码方式
    uint32_t encoding;
    //集合包含的元素数量
    uint32_t length;
    //保存元素的数组
    int8_t contents[];
} intset;
```

* contents数组虽然是声明int8_t，但是并不会保存任何int8_t类型的元素

  > 各个项在数组中**按值的大小从小到大有序地排列**，并且数组中不包含任何重复项。

* length： 集合中的元素数量

* encoding

  * INTSET_ENC_INT16：  contents  是int16_t类型的数组
  * INTSET_ENC_INT32： contents  是int32_t类型的数组
  * INTSET_ENC_INT64： contents  是int64_t类型的数组

## 升级

每当我们要将一个新元素添加到整数集合里面，并且**新元素的类型比整数集合现有所有元素的类型都要长**时，整数集合需要先进行升级（upgrade），然后才能将新元素添加到整数集合里面。

1. 根据新元素的类型，扩展整数集合底层数组的空间大小，并为新元素分配空间。
2. 将底层数组现有的所有元素都转换成与新元素相同的类型，并将类型转换后的元素放置到正确的位上，而且在放置元素的过程中，需要继续维持底层数组的有序性质不变。
3. 将新元素添加到底层数组里面。



# 压缩列表

压缩列表（ziplist）是**列表键**和**哈希键**的底层实现之一。

* 当一个列表键只包含少量列表项，并且**每个列表项要么就是小整数值，要么就是长度比较短的字符串**，那么Redis就会使用压缩列表来做列表键的底层实现。
* 当一个哈希键只包含少量键值对，比且**每个键值对的键和值**要么就是小整数值，要么就是长度比较短的字符串，那么Redis就会使用压缩列表来做哈希键的底层实现。

## 压缩列表的构成

压缩列表是Redis为了节约内存而开发的，是由一系列特殊编码的连续内存块组成的顺序型数据结构。

一个压缩列表可以包含**任意多个节点（entry）**，每个节点可以保存一个字节数组或者一个整数值。

* zlbytes: 长度4字节，记录压缩列表的总长度
* zltail: 4字节，压缩列表表尾节点距离起始节点之间有多少字节，作为一个偏移量的存在，知道了起始节点位置，就可以根据这个字段获取到表尾节点的位置。
* zllen： 压缩列表包含的节点数量
* entry1...entryN： 压缩列表的各个节点信息
* zlend： 特殊字符，用于标记压缩列表的末端

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E5%8E%8B%E7%BC%A9%E5%88%97%E8%A1%A8%E7%9A%84%E7%A4%BA%E4%BE%8B.png)

## 压缩列表节点的构成

每个压缩列表节点可以保存一个字节数组或者一个整数值。

每个压缩列表节点都由previous_entry_length、encoding、content三个部分组成。

### previous_entry_length

记录压缩列表节点的前一个节点的长度，用字节来表示。

作用：**程序可以通过指针运算，根据当前节点的起始地址来计算出前一个节点的起始地址**

应用：压缩列表的从表尾向表头遍历操作就是使用这一原理实现的。

### encoding

节点的encoding属性记录了**节点的content属性所保存数据的类型以及长度**

![](https://shishancoder.oss-cn-nanjing.aliyuncs.com/architect/redis/%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_20231118172806.png)



### content



# quicklist





# 对象





# 参考资料

1. [《Redis设计与实现》](https://weread.qq.com/web/bookDetail/d35323e0597db0d35bd957b)
2. [Redis系列文章——合集](https://mp.weixin.qq.com/s?__biz=MzA4NTg1MjM0Mg==&mid=509777776&idx=1&sn=e56f24bdf2de7e25515fe9f25ef57557&mpshare=1&scene=1&srcid=1010HdkIxon3icsWNmTyecI6#rd)
