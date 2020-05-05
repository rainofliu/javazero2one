# TreeMap

+ 底层为红黑树
+ 利用 `Comparator`实现了key的排序

# LinkedHashMap

+ 按照插入排序
+ 本身继承了`HashMap`
+ 按照插入顺序进行访问
+ 实现了访问最少最先删除功能，其目的是把很久都没有访问的 key 自动删除。

## 链表结构

```java
   /**
     * The head (eldest) of the doubly linked list.
     */
    transient LinkedHashMap.Entry<K,V> head; // 链表头

    /**
     * The tail (youngest) of the doubly linked list.
     */
    transient LinkedHashMap.Entry<K,V> tail; // 链表尾

    // 继承 Node，为数组的每个元素增加了 before 和 after 属性
  static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
      
   // 控制两种访问模式的字段，默认 false
   // true 按照访问顺序，会把经常访问的 key 放到队尾
   // false 按照插入顺序提供访问
   final boolean accessOrder;   
}
```

> 从上述 Map 新增的属性可以看到，LinkedHashMap 的数据结构很像是把` LinkedList` 的每个元素换成了 `HashMap` 的` Node`，像是两者的结合体，也正是因为增加了这些结构，从而能把 Map 的元素都串联起来，形成一个链表，而链表就可以保证顺序了，就可以维护元素插入进来的顺序。

## 按照顺序新增

`LinkedHashMap` 初始化时，默认 accessOrder 为 false，就是会按照插入顺序提供访问，插入方法使用的是父类 `HashMap` 的 put 方法，不过覆写了 put 方法执行中调用的 newNode/newTreeNode 和 afterNodeAccess 方法。

```java
// 新增节点，并追加到链表的尾部
Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    // 新增节点
    LinkedHashMap.Entry<K,V> p =
        new LinkedHashMap.Entry<K,V>(hash, key, value, e);
    // 追加到链表的尾部
    linkNodeLast(p);
    return p;
}
// link at the end of list
private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
    LinkedHashMap.Entry<K,V> last = tail;
    // 新增节点等于位节点
    tail = p;
    // last 为空，说明链表为空，首尾节点相等
    if (last == null)
        head = p;
    // 链表有数据，直接建立新增节点和上个尾节点之间的前后关系即可
    else {
        p.before = last;
        last.after = p;
    }
}
```

## 按插入顺序访问

LinkedHashMap 只提供了**单向访问**，即按照插入的顺序从头到尾进行访问，不能像 LinkedList 那样可以双向访问。

我们主要通过迭代器进行访问，迭代器初始化的时候，默认从头节点开始访问，在迭代的过程中，不断访问当前节点的 after 节点即可

```java
// 初始化时，默认从头节点开始访问
LinkedHashIterator() {
    // 头节点作为第一个访问的节点
    next = head;
    expectedModCount = modCount;
    current = null;
}

final LinkedHashMap.Entry<K,V> nextNode() {
    LinkedHashMap.Entry<K,V> e = next;
    if (modCount != expectedModCount)// 校验
        throw new ConcurrentModificationException();
    if (e == null)
        throw new NoSuchElementException();
    current = e;
    next = e.after; // 通过链表的 after 结构，找到下一个迭代的节点
    return e;
}
```

## 访问最少删除策略

可以通过重写下面的方法来实现

```java
  protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }
```

## 元素被转移到队列尾部

```java
public V get(Object key) {
    Node<K,V> e;
    // 调用 HashMap  get 方法
    if ((e = getNode(hash(key), key)) == null)
        return null;
    // 如果设置了 LRU 策略
    if (accessOrder)
    // 这个方法把当前 key 移动到队尾
        afterNodeAccess(e);
    return e.value;
}
```

> 就是当前key已经被获取了，就将其放在队列尾部，表明是常用元素

## 删除策略

`LinkedHashMap` 本身是没有 put 方法实现的，调用的是 `HashMap` 的 put 方法，但 `LinkedHashMap` 实现了 put 方法中的调用 afterNodeInsertion 方法，这个方式实现了删除，

```java
// 删除很少被访问的元素，被 HashMap 的 put 方法所调用
void afterNodeInsertion(boolean evict) { 
    // 得到元素头节点
    LinkedHashMap.Entry<K,V> first;
    // removeEldestEntry 来控制删除策略，如果队列不为空，并且删除策略允许删除的情况下，删除头节点
    if (evict && (first = head) != null && removeEldestEntry(first)) {
        K key = first.key;
        // removeNode 删除头节点
        removeNode(hash(key), key, null, false, true);
    }
}
```

