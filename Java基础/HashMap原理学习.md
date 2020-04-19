[toc]

![](https://liutianruo-2019-go-go-go.oss-cn-shanghai.aliyuncs.com/images/HashMap原理（JDK1.8）.png)

# HashMap

> 本文基于Java 8 来分析`HashMap`

+ `HashMap`允许null值，不同于`HashTable` ，非线程安全

+ load factor(影响因子）默认为0.75，是**均衡了时间和空间损耗计算出来的值**，较高的值会减少空间开销（**扩容减少，数组大小增长速度变慢**），但增加了**查找成本（hash冲突增加，链表长度变长）**

  > 不扩容的条件：数组容量 > 需要数组大小/load factor

+ 如果有很多数据存储到`HashMap`中，建议`HashMap`的容量一开始就设置足够的大小，防止不断扩容造成的性能开销

+ `HashMap`是非线程安全的，我们可以在`HashMap`操作时手动加入锁，或者通过`Collections.synchronizedMap`来实现线程安全

  > 其实就是内部上了一个Object锁
  >
  > ```java
  >  private static class SynchronizedMap<K,V>
  >         implements Map<K,V>, Serializable {
  > 	  // 包装的Map
  > 	  private final Map<K,V> m;     // Backing Map
  >       // 被锁的对象 
  >       final Object   mutex;        // Object on which to synchronize
  > 
  >       SynchronizedMap(Map<K,V> m) {
  >             this.m = Objects.requireNonNull(m);
  >             mutex = this;
  >       }
  >        public int size() {
  >             synchronized (mutex) {return m.size();}
  >         }
  >         public boolean isEmpty() {
  >             synchronized (mutex) {return m.isEmpty();}
  >         }
  >         public boolean containsKey(Object key) {
  >             synchronized (mutex) {return m.containsKey(key);}
  >         }
  >         public boolean containsValue(Object value) {
  >             synchronized (mutex) {return m.containsValue(value);}
  >         }
  >         public V get(Object key) {
  >             synchronized (mutex) {return m.get(key);}
  >         }
  > 
  >         public V put(K key, V value) {
  >             synchronized (mutex) {return m.put(key, value);}
  >         }
  >         public V remove(Object key) {
  >             synchronized (mutex) {return m.remove(key);}
  >         }
  >         public void putAll(Map<? extends K, ? extends V> map) {
  >             synchronized (mutex) {m.putAll(map);}
  >         }
  >         public void clear() {
  >             synchronized (mutex) {m.clear();}
  >         }
  >      ...
  >  }
  > ```

+ 在迭代过程中，如果`HashMap`的结构被修改，会迅速失败

+ HashMap的底层是对象数组 + 链表 + 红黑树。

## Map

```java
public interface Map<K,V> {
    int size();  // 查看Map中键值对的个数
    boolean isEmpty(); // Map是否为空
    boolean containsKey(Object key); // 查看是否包含某个键
    boolean containsValue(Object value); // 查看是否包含某个值
    V get(Object key); // 根据键获取值，如果没获取到，返回null
    V put(K key, V value); // 保存键值对，如果键在Map中已经存在，则会覆盖原来的值
    V remove(Object key); // 根据键删除键值对，返回key原来对应的值，如果不存在，返回null
    void putAll(Map<? extends K, ? extends V> m);// 保存m中所有键值对到Map中
    void clear();// 清空Map中所有的键值对
    Set<K> keySet(); // 获取Map中key的集合
    Collection<V> values();// 获取Map中所有value的集合
    Set<Map.Entry<K, V>> entrySet();// 获取Map中所有键值对
    // 内置接口，表示一条键值对
    interface Entry<K,V> {
        K getKey();//键值对的键
        V getValue(); // 键值对的值
        V setValue(V value);// 设置值
        boolean equals(Object o);
        int hashCode();
        ...
    }
    
    boolean equals(Object o);
    int hashCode();
    // since jdk1.8 获取默认的value，如果通过key成功获取到value，且value不为空，则返回获取的value；
    // 否则返回defaultValue
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return (((v = get(key)) != null) || containsKey(key))
            ? v
            : defaultValue;
    }
```

> HashMap实现了Map接口

## 属性

```java
// 默认的初始容量：16
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
// 最大容量 1073741824
static final int MAXIMUM_CAPACITY = 1 << 30;
// 默认负载因子
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// bin(桶)容量大于等于8时，链表转化成红黑树
static final int TREEIFY_THRESHOLD = 8;
// bin(桶)容量小于等于6时，红黑树转化成链表
static final int UNTREEIFY_THRESHOLD = 6;
// 容量最小64时才会转化成红黑树
static final int MIN_TREEIFY_CAPACITY = 64;
// 用于fail-fast的，记录HashMap结构发生变化（数量变化或rehash）的数目
transient int modCount;

// HashMap的实际大小，可能不准确（拿到这个值的时候，这个值又发生了变化）
transient int size;
// 扩容的门槛，如果初始化时，给定数组大小的话，通过tableSizeFor方法计算，永远接近于2的幂次方
// 比如你给定初始化大小 19，实际上初始化大小为 32，为 2 的 5 次方。
// 如果通过resize方法进行扩容后，大小=数组容量*0.75
int threshold;
// 存放数据的数组 hash表/hash桶
transient Node<K,V>[] table;

transient Set<Map.Entry<K,V>> entrySet;

// bin node 节点
static class Node<K,V> implements Map.Entry<K,V> {
    
// 红黑树的节点
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {    
```

## 构造方法

```java
  public HashMap(int initialCapacity, float loadFactor) {
        // 初始容量<0 抛出异常
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        // 如果初始容量超过默认的最大容量，将最大容量修改为当前初始容量
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        // 负载因子小于等于0或者非数字 抛出异常
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        // 设置负载因子
      	this.loadFactor = loadFactor;
        // 设置扩容的门槛
        this.threshold = tableSizeFor(initialCapacity);
  }
	
    /**
     ** 永远接近于2的幂次方
     ** 比如你给定初始化大小 19，实际上初始化大小为 32，为 2 的 5 次方。
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {
        // cap=16
        int n = cap - 1; // 15
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

  public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }
  public HashMap() {
        // 0.75
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
  }

  public HashMap(Map<? extends K, ? extends V> m) {
        // 0.75
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
   }
  /**
     * Implements Map.putAll and Map constructor
     *
     * @param m the map
     * @param evict false when initially constructing this map, else
     * true (relayed to method afterNodeInsertion).
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        // m的实际大小
        int s = m.size();
        if (s > 0) {
            // hash表
            if (table == null) { // pre-size
                float ft = ((float)s / loadFactor) + 1.0F;
                // MAXIMUM_CAPACITY: 1,073,741,824
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            else if (s > threshold)
                resize();
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }
```

## 数据结构

### Node

> 数组/hash表

```java
 /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash; // hash值
        final K key; // key
        V value; // value
        Node<K,V> next; //指向当前Node的下一个节点，构成单向链表

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }
		
        public final int hashCode() {
            // Objects.hashCode(key) 防止空指针
            // key hash值 和value hash值 进行“与运算”
            return Objects.hashCode(key)  ^ Objects.hashCode(value);
        }
		// 设置value，并返回oldValue
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
		// 判断Entry是否相等
        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                // key 和value都相等 返回true
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }
```

### TreeNode

> 红黑树

```java
 static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links 红黑树父节点
        TreeNode<K,V> left; // 左节点
        TreeNode<K,V> right; // 右节点
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

       
        //找到红黑树的根节点，根据根节点没有父节点来判断
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        //把给定的root放到根节点上去
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];//找到当前树的根节点
                //如果root不是根节点，就把root放到根节点上去，分成2步
                //1:解决root下面节点的问题
                //2:把root放到当前根节点first的左边去
                if (root != first) {
                    Node<K,V> rn;
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;
                    //下面的两个if是为了解决步骤1。
                    // 把root的next挂在自己prev的后面即可
                    // 就是把自己摘掉，后面一位和前面一位连接起来
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    //这个if解决了步骤2 把root当作根节点，并且设置prev为null
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        //根据hash和key查找红黑树中节点是否已经存在。策略如下
        //1：从根节点递归查找
        //2：根据 hashcode，比较查找节点，左边节点，右边节点之间的大小，查找节点大于左边节点，取左边节点，查找小于右边节点，取右边节点。
        //3：判断查找节点和 2 步取的节点是否相等，相等返回，不等重复 2，3 两步。
        //4：2，3不中的话,如果key实现了Comparable接口，使用compareTo进行比较大小，重复2
        //5:  判断节点位置时，需要关心一边节点为空的情况
        //6：1~5如果都没有命中，默认从右边开始往下递归
        //7：这样查找的好处就是比较快，最大的循环次数是树的最大深度
        //如果树比较平衡，查询还是很快的。
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            //得到当前红黑树
            TreeNode<K,V> p = this;
            do {
                int ph, dir; K pk;
                TreeNode<K,V> pl = p.left, pr = p.right, q;
                //1:如果key的hash值小于当前节点,取当前节点左边的节点
                if ((ph = p.hash) > h)
                    p = pl;
                //2:如果key的hash值大于当前节点,取当前节点右边的节点
                else if (ph < h)
                    p = pr;
                //2.1:这里没有判断相等的情况，因为相等时，p就是当前节点，不需要判断
                //3:如果key的hash值等于当前节点，直接返回当前节点，结束递归查找
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                //4:如果当前节点的左节点为空，说明左边已经查找完了，再去判断右节点
                else if (pl == null)
                    p = pr;
                //5:如果当前节点的右节点为空，说明右边已经查找完了，就再去判断左节点
                //4和5防止左右一边为空，提前退出的情况
                else if (pr == null)
                    p = pl;
                //6: 不采用hashcode的判断大小的话，可以选择compareTo自定义的判断方法
                //只需要自己实现key的Comparable就好了
                else if ((kc != null ||
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                //7:如果当前节点和key的hashcode相等，但是key的值不等，并且没有实现
                //comparable的话，只能用最简单的方法，先匹配右边，匹配不到匹配左边
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                //8:右节点找不到，再递归查找左节点
                else
                    p = pl;
                //如果p不为空需要一直递归循环
            } while (p != null);
            //如果找不到，返回null
            return null;
        }
```

> 先理解到这个程度

## 保存键值对

```java
 /**
     * 返回key之前对应的value值，如果key之前在HashMap中不存在，那么返回null
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
   
```

### hash算法（包含寻址算法的介绍）

```java
static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
 }
```

> 这里是对key取hash值，然后将hash值右移16位和原有hash值 进行 ^**抑或运算**

![](https://liutianruo-2019-go-go-go.oss-cn-shanghai.aliyuncs.com/images/位运算.png)

举例说明：

+ key的hash值：1111 1111 1111 1111 1111 1010 0111 1100

+ key的hash值右移16位： 0000 0000 0000 0000 1111 1111 1111 1111

+ ^操作得到的hash值： 1111 1111 1111 1111 0000 0101 1000 0011

  > 原有hash值的高低16位进行抑或操作，使得得到的hash值低16位上 具有了原有hash值高、低16位的特征，从而降低了hash冲突

1. 首先为什么不直接采用key对应的hash值，是为了要降低hash冲突，所以需要将hash值再处理一次；

2. 高低16位进行抑或操作，而是为了降低hash冲突

   > 我们最终处理拿到的hash值是要和数组容量大小-1的值进行**与运算**（寻址算法），在大部分情况下，数组容量大小对应的二进制在高16位上基本全为0，所以与运算 通常只考虑低16位就可以了，那么hash值的低16位如果不能和别的hash值区分开，就会很容易碰到烦人的hash冲突，然后又要解决hash冲突；因此我们需要对原有hash值的低16位进行一个特殊化的位运算，就是让原有hash值的高低16位参与抑或运算，让最终得到的hash值同时在低16位上同时具有高、低16位的特征，从而降低了与运算低16位上的hash冲突。

### putVal

#### 寻址算法

(n - 1) & hash就是寻址算法

我们在hash算法中强调为什么要对原hash值进行高低16位的抑或运算时，提到了寻址算法，寻址顾名思义就是寻找到数组中的地址，就是数组的下标。

在JDK1.8以前，寻址算法为：hash对n取模；但是考虑到取模运算的性能问题，所以JDK1.8对其进行了优化

(n - 1) & hash = hash%n （当且仅当n=2）

**谈到这里，不难得知 hash算法是为了降低hash冲突，而寻址算法是为了优化性能。**

#### 方法逻辑

```java
/**
 * hash : 通过hash算法最终得到的hash值
   key: 键
   value : 值
   onlyIfAbsent：false 表示即使 key 已经存在了，仍然会用新值覆盖原来的值，默认为 false
 */ 
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; 
     	Node<K,V> p; 
        int n, i; // n: 数组长度   i：数组的下标
        // 数组为空，需要resize方法初始化
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
    	// 如果当前索引位置为空，说明这个位置还没有存储数据，那么生成
        //  新的节点在当前位置上
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            // 这个分支处理hash冲突
            // e: 当前节点的临时变量
            Node<K,V> e; K k;
            // p: 数组上已经存储的值
            // k : p对应的key
            // 如果 key 的 hash 和值都相等，直接把当前下标位置的 Node 值赋值给临时变量
            // 这里先比较hash值，如果hash值相同，再通过equals方法比较，如果hash值不同，就没必要
            // 再比较了；hash值相同，两个key才有可能相等
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            else if (p instanceof TreeNode)
                // 如果是红黑树，使用红黑树的方式新增
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else { // 是个链表，把新节点放到链表的尾端
                // 自旋
                for (int binCount = 0; ; ++binCount) {
                    // 遍历链表
                    // e = p.next 表示从头开始，遍历链表
                    // p.next == null 表明 p 是链表的尾节点
                    if ((e = p.next) == null) {
                        // 把新节点放到链表的尾部
                        p.next = newNode(hash, key, value, null);
                        // 链表长度大于8时，链表转红黑树
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        // 退出循环
                        break;
                    }
                    // 在链表中，e的hash值和我要存的key value中key的hash值一样，就再用
                    // equals比较，如果还为true，说明这个e就是我之前存储过的，终止循环
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    // 更改循环的当前元素，使 p 在遍历过程中，一直往后移动。
                    p = e;
                }
            }
            // 要覆盖更新的节点位置已经找到
            if (e != null) { // existing mapping for key
                // 获取旧的value
                V oldValue = e.value;
                // 当 onlyIfAbsent 为 false 时，才会覆盖值 
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                // Callbacks to allow LinkedHashMap post-actions
                afterNodeAccess(e);
                // 返回旧的value
                return oldValue;
            }
        }
        // 记录HashMap的数据结构发生了一次变化
        ++modCount;
        // 如果HashMap的实际大小大于扩容的门槛，那么就扩容
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
}
```

#### 扩容

扩容会有一个rehash的过程，需要重新计算键值对在数组中的位置，比较耗费性能

```java
//初始化或者双倍扩容，如果是空的，按照初始容量进行初始化
//扩容是双倍扩容，要么还在原来索引位置
// 要么 是在index + oldCap(index ：原来索引位置)
final Node<K,V>[] resize() {
        // 旧的数组
        Node<K,V>[] oldTab = table;
        // 旧数组的容量 
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 旧数组扩容门槛
        int oldThr = threshold;
        // 初始化 新数组容量和新数组的扩容门槛
        int newCap, newThr = 0;
        if (oldCap > 0) {
            // 旧数组容量 >= 最大容量 ，不扩容
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                // 返回旧数组
                return oldTab;
            }
            // 设置新数组容量为原数组容量的2倍 
            // 如果新数组容量介于最大值和最小值之间，成功扩容设置新数组扩容门槛
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                // 设置新数组扩容门槛
                newThr = oldThr << 1; // double threshold
        }
    	
        else if (oldThr > 0) // initial capacity was placed in threshold
            // 新数组容量
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            // 初始化
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
    
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        // 设置threshold
        threshold = newThr;
    	
        @SuppressWarnings({"rawtypes","unchecked"})
            // 创建新数组
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    	// table引用变量指向新数组，旧数组相当于无人问津了，会被GC
        table = newTab;
        // 将旧数组的键值对交给新数组
        if (oldTab != null) {
            // 相当于 遍历旧数组
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                // 数组索引上有值
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    // 节点上只有一个值，直接计算索引位置并根据索引位置存储元素
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    // 红黑树
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        // loHead 表示老值,老值的意思是扩容后，该链表中计算出索引位置不变的元素
                        // hiHead 表示新值，新值的意思是扩容后，计算出索引位置发生变化的元素
                        // 举个例子，数组大小是 8 ，在数组索引位置是 1 的地方挂着两个值，两个值的 hashcode 是9和33。
                        // 当数组发生扩容时，新数组的大小是 16，此时 hashcode 是 33 的值计算出来的数组索引位置仍然是 1，我们称为老值
                        // hashcode 是 9 的值计算出来的数组索引位置是 9，就发生了变化，我们称为新值。
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            // (e.hash & oldCap) == 0 表示老值链
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            } 
                            // (e.hash & oldCap) != 0 表示新值链表
                            else { 
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                         // 老值链表赋值给原来的数组索引位置
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        // 新值链表赋值给原来的数组索引位置
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        // 返回扩容后的新数组
        return newTab;
    }
```

##### rehash

重新计算扩容后hash表元素的hash值，并根据hash值确定元素在数组中的节点位置

## 查找键值对

```java
 public V get(Object key) {
        Node<K,V> e;
        // 首先这里会根据key的原始hash值计算出一个新的hash值（高、低16位^运算）
        return (e = getNode(hash(key), key)) == null ? null : e.value;
  }
```

### getNode

```java
 // 1:根据hashcode,算出数组的索引，找到槽点
 // 2:槽点的key和查询的key相等，直接返回
 // 3:槽点没有next，返回null
 // 4:槽点有next，判断是红黑树还是链表
 // 5:红黑树调用find，链表不断循环
final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; // table
    	Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) { // 数组索引对应位置数据不为空
            if (first.hash == hash &&  // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first; // key和索引上第一个元素一样，直接返回第一个元素
            if ((e = first.next) != null) { // 找链表元素
                if (first instanceof TreeNode) // 红黑树
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do { 
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);// 往后遍历链表
            }
        }
        // 根据hashCode定位到的索引上，没有值，返回空
        return null;
 }
```

## 总结

对于HashMap基本原理基本就学习到这了，关于JDK1.7和JDK1.8的区别、红黑树的细节后续再进行理解

## 后续更新(TODO)

后面跟着https://itlemon.blog.csdn.net/article/details/104271481再理解一下

## **参考内容**

+ [面试官系统精讲Java源码及大厂真题](https://www.imooc.com/read/47)
+ [中华石杉—互联网Java工程师面试突击（第三季](https://apppukyptrl1086.pc.xiaoe-tech.com/detail/v_5ded2450c5b33_rvQSNDeT/3?from=p_5dd3ccd673073_9LnpmMju&type=6)
+ [一篇文章深入理解JDK8 HashMap](https://itlemon.blog.csdn.net/article/details/104271481)