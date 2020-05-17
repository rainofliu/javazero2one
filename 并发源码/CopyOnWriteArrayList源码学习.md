[toc]

`java.util.concurrent.CopyOnWriteArrayList`

+ 线程安全，多线程环境下可以直接使用，无需加锁
+ 通过锁+数组拷贝+volatile关键字保证了线程安全
+ 每次数组操作，都会把数组拷贝一份出来，在新数组上进行操作，操作成功后再赋值回去

# 基本介绍

+ `CopyOnWriteArrayList`是线程安全的，因为操作都是在新拷贝数组上进行的
+ 数组的拷贝虽然有一定的成本，但往往比一般的替代方案效率高
+ 迭代过程中，不会影响原来的数组，也不会抛出`java.util.ConcurrentModificationException`

> 从整体架构上来说，`CopyOnWriteArrayList `数据结构和 `ArrayList` 是一致的，底层是个数组，只不过 `CopyOnWriteArrayList` 在对数组进行操作的时候，基本会分四步走：

1. 加锁；
2. 从原数组中拷贝出新数组；
3. 在新数组上进行操作，并把新数组赋值给数组容器；
4. 解锁。

> 除了加锁之外，`CopyOnWriteArrayList` 的底层数组还被 `volatile` 关键字修饰，意思是一旦数组被修改，其它线程立马能够感知到，代码如下：
>
> ```java
> private transient volatile Object[] array;
> ```
>
> 整体上来说，`CopyOnWriteArrayList` 就是利用锁 + 数组拷贝 +`volatile` 关键字保证了 List 的线程安全。

# 新增

## 新增到数组尾部

```java
 public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        // 加锁
        lock.lock();
        try {
            // 得到原来的数组
            Object[] elements = getArray();
            // 获取原数组长度
            int len = elements.length;
            // 拷贝新数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            // 给新数组赋值
            newElements[len] = e;
            // 替换原来的数组
            setArray(newElements);
            return true;
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
```

+ 加锁：保证同一时刻只有一个线程能够对同一数组进行add操作
+ 拷贝出新数组
  + 既然已经加锁了，为什么不在原数组上操作呢
    + `volatile`关键字修饰的是数组，**如果我们简单的在原来数组上修改其中某几个元素的值，是无法触发可见性的，我们必须通过修改数组的内存地址才行**，也就说要对数组进行重新赋值才行。
    + 在新的数组上进行拷贝，对老数组没有任何影响，只有新数组完全拷贝完成之后，外部才能访问到，降低了在赋值过程中，老数组数据变动的影响。

## 指定位置添加

```java
 public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        // 加锁
        lock.lock();
        try { 
            // 获取原数组
            Object[] elements = getArray();
            // 原数组大小
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ", Size: "+len);
            Object[] newElements;
             // 如果要插入的位置在数组的中间，就需要拷贝 2 次
             // 第一次从 0 拷贝到 index。
             // 第二次从 index+1 拷贝到末尾。
            int numMoved = len - index;
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + 1);
            else {
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1,
                                 numMoved);
            }
            // index 索引位置的值是空的，直接赋值即可。
            newElements[index] = element;
            // 把新数组的值赋值给数组的容器中
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }
```

> 拷贝两次是需要将数组的index位置设置为null

## 小结

从 add 系列方法可以看出，`CopyOnWriteArrayList` 通过加锁 + 数组拷贝+ volatile 来保证了线程安全，每一个要素都有着其独特的含义：

1. 加锁：保证同一时刻数组只能被一个线程操作；
2. 数组拷贝：保证数组的内存地址被修改，修改后触发 volatile 的可见性，其它线程可以立马知道数组已经被修改；
3. volatile：值被修改后，其它线程能够立马感知最新值。

> 3 个要素缺一不可，比如说我们只使用 1 和 3 ，去掉 2，这样当我们修改数组中某个值时，并不会触发 volatile 的可见特性的，只有当数组内存地址被修改后，才能触发把最新值通知给其他线程的特性。

# 删除

## 删除指定位置元素

```java
 public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 获取旧的值
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            // 如果要删除的数据正好是数组的尾部，直接删除
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                 // 如果删除的数据在数组的中间，分三步走
                // 1. 设置新数组的长度减一，因为是减少一个元素
                // 2. 从 0 拷贝到数组新位置
                // 3. 从新位置拷贝到数组尾部
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index,
                                 numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }
```

## 批量删除

```java
 public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 说明数组有值，数组无值直接返回 false
            if (len != 0) {
                // temp array holds those elements we know we want to keep
                // newlen 表示新数组的索引位置，新数组中存在不包含在 c 中的元素
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                     // 不包含在 c 中的元素，从 0 开始放到新数组中
                    Object element = elements[i];
                    if (!c.contains(element))
                        temp[newlen++] = element;
                }
                // 拷贝新数组，变相的删除了不包含在 c 中的元素
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
```

> 我们并不会直接对数组中的元素进行挨个删除，而是先对数组中的值进行循环判断，把我们不需要删除的数据放到临时数组中，最后临时数组中的数据就是我们不需要删除的数据。

> 所以我们在需要删除多个元素的时候，最好都使用这种批量删除的思想，而不是采用在 for 循环中使用单个删除的方法，**单个删除的话，在每次删除的时候都会进行一次数组拷贝(删除最后一个元素时不会拷贝)，很消耗性能**，也耗时，会导致加锁时间太长，并发大的情况下，会造成大量请求在等待锁，这也会占用一定的内存。

# indexOf

```java
  public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }
```

# 迭代

> 迭代器拿到的是对象数组的一个快照

```java
 public Iterator<E> iterator() {
      return new COWIterator<E>(getArray(), 0);
  }
  static final class COWIterator<E> implements ListIterator<E> {
        /** Snapshot of the array */
        private final Object[] snapshot;
        /** Index of element to be returned by subsequent call to next.  */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }
      ...
  }   
```

# 总结

1. `CopyOnWriteArrayList`是用了一把全部锁保证线程安全性，volatile关键字和数组拷贝保证了内存可见性。

2. 该集合适合读多写少的情况，读不加锁，写加锁，缺点是内存开销很大（数组的拷贝）
3. 和`java.util.Collections#synchronizedList(java.util.List<T>)`相比，`CopyOnWriteArrayList`读不加锁，写时可以读，在读多写少的情境下 性能较高，前者读是不可写，写时不可读，性能会差一些