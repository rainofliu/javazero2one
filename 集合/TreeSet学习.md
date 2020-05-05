[toc]

之前学习了`HashSet`，我们知道它实现了`Set`接口，并且是没有顺序的，没有顺序的原因是`HashMap`无序。

`TreeSet`底层是`TreeMap`，继承了`TreeMap`key能够排序的功能，迭代的时候也能按照key的排序顺序来迭代

# 构造方法

```java
  TreeSet(NavigableMap<E,Object> m) {
        this.m = m;
   }
   public TreeSet() {
        this(new TreeMap<E,Object>());
    }  
   public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<>(comparator));
    }
   public TreeSet(Collection<? extends E> c) {
        this();
        addAll(c);
   }
```

# 操作方法

## add

```java
  public boolean add(E e) {
        return m.put(e, PRESENT)==null;
    }
```

> `HashSet`也是这么写的

## 迭代

```java
  // 直接获取map中key的迭代器
  public Iterator<E> descendingIterator() {
        return m.descendingKeySet().iterator();
  }
```

> 直接使用`TreeMap`的底层能力

```java
// NavigableSet 接口，定义了迭代的一些规范，和一些取值的特殊方法
// TreeSet 实现了该方法，也就是说 TreeSet 本身已经定义了迭代的规范
public interface NavigableSet<E> extends SortedSet<E> {
    Iterator<E> iterator();
    E lower(E e);
}  
// m.navigableKeySet() 是 TreeMap 写了一个子类实现了 NavigableSet
// 接口，实现了 TreeSet 定义的迭代规范
public Iterator<E> iterator() {
    return m.navigableKeySet().iterator();
}
```



> 方案 1 和 2 的调用关系，都是 `TreeSet` 调用` TreeMap`，但功能的实现关系完全相反，第一种是功能的定义和实现都在 `TreeMap`，`TreeSet` 只是简单的调用而已，第二种 `TreeSet` 把接口定义出来后，让 `TreeMap `去实现内部逻辑，`TreeSet `负责接口定义，`TreeMap` 负责具体实现，这样的话因为接口是 `TreeSet` 定义的，所以实现一定是` TreeSet` 最想要的，`TreeSet` 甚至都不用包装，可以直接把返回值吐出去都行。

> 思路 2 主要适用于复杂场景，比如说迭代场景，TreeSet 的场景复杂，比如要能从头开始迭代，比如要能取第一个值，比如要能取最后一个值，再加上 TreeMap 底层结构比较复杂，TreeSet 可能并不清楚 TreeMap 底层的复杂逻辑，这时候让 TreeSet 来实现如此复杂的场景逻辑，TreeSet 就搞不定了，不如接口让 TreeSet 来定义，让 TreeMap 去负责实现，TreeMap 对底层的复杂结构非常清楚，实现起来既准确又简单。