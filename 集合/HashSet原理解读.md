[toc]

HashSet是Set接口的实现类，Set表示不一定有顺序但一定不重复的容器接口

+ 没有重复元素

+ 没有顺序

  > `TreeSet`是有序的

+ 底层基于HashMap，添加、删除元素操作效率很高

+ HashSet 非线程安全，多线程场景下使用可能会出问题

# 构造方法

```java
   public HashSet() {
        map = new HashMap<>();
    }
   public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }
```

可以看到`HashSet`底层是`HashMap`

> `Math.max ((int) (c.size ()/.75f) + 1, 16)`，就是对 HashMap 的容量进行了计算，翻译成中文就是 取括号中两个数的最大值（期望的值 / 0.75+1，默认值 16），从计算中，我们可以看出 HashSet 的实现者对 HashMap 的底层实现是非常清楚的，主要体现在两个方面：
>
> 和 16 比较大小的意思是说，如果给定 HashMap 初始容量小于 16 ，就按照 HashMap 默认的 16 初始化好了，如果大于 16，就按照给定值初始化。
> HashMap 扩容的伐值的计算公式是：Map 的容量 * 0.75f，一旦达到阀值就会扩容，此处用 (int) (c.size ()/.75f) + 1 来表示初始化的值，这样使我们期望的大小值正好比扩容的阀值还大 1，就不会扩容，符合 HashMap 扩容的公式。

# 内部原理

```java
 // 把 HashMap 组合进来，key 是 Hashset 的 key，value 是下面的 PRESENT 
 private transient HashMap<E,Object> map;

  // Dummy value to associate with an Object in the backing Map
  private static final Object PRESENT = new Object();
  
```

> 其他原理就是一些HashMap的操作，不再列举出来分析
>
> ```java
>   public boolean add(E e) {
>         return map.put(e, PRESENT)==null;
>     }
> ```

# 总结

1. HashSet是采用组合方式，内部存放了一个HashMap，利用HashMap来高效进行元素的新增、删除和插入等操作

2. HashSet使用了默认的共享对象作为HashMap 的共享Value

3. 为什么HashSet是无序的，因为它底层的HashMap是无序的

   > 自我的理解