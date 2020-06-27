[toc]

# Java集合框架总览

+ 集合接口

  + `Collection`

    + `List`

    + `Set`

      +  `SortedSet`

        > 如果您返回的`Set`是有序的，那您可以返回`SortedSet`

        + `java.util.NavigableSet`

      > List有序，可以通过坐标访问；Set元素不重复

    + `java.util.Queue`

      > 单向列表

    + `java.util.Deque`

      > 双向列表
      >
      > ```java
      > public class LinkedList<E>
      >     extends AbstractSequentialList<E>
      >     implements List<E>, Deque<E>, Cloneable, java.io.Serializable
      > ```

  + `Map`或其他接口（`Dictionary`）

> 根据Java语言规范，底层为数组的集合，容量不能达到Integer最大值；而底层为链表的结构，则没有该限制

```java
List<String> list=new ArrayList<>(Integer.MAX_VALUE);
```

> Exception in thread "main" java.lang.OutOfMemoryError: Requested array size exceeds VM limit
> 	at java.base/java.util.ArrayList.<init>(ArrayList.java:154)
> 	at com.ajin.deep.in.java.collection.IntergerMaxValueDemo.main(IntergerMaxValueDemo.java:13)
>
> 备注： java.base 是因为我们使用了 JDK11（JDK9中引入了模块化）

```java
public interface Collection<E> extends Iterable<E> {
 
    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this collection
     */
    // 返回当前Collection中的元素的个数，如果元素个数超过Integer最大值(2的31次方-1)
    // 则返回该最大值
   int size();
    
   boolean isEmpty();
    
   boolean contains(Object o);
    
   Iterator<E> iterator();
    
   Object[] toArray();
    
   <T> T[] toArray(T[] a);
    
   ....
}
```