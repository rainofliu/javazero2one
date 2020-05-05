

[toc]



ArrayList是一个动态数组容器类。

# 基本结构

```java
  transient Object[] elementData; // 这里不设置为private是方便内部类访问
  // 数组元素个数，没有使用volatile修饰，不是线程安全的
  private int size;
  // 默认初始容量
  private static final int DEFAULT_CAPACITY = 10;
  // AbstractList中的属性,代表当前数组被修改的版本次数，数组结构有变动，就会+1
 protected transient int modCount = 0;
```

![ArrayList结构图](https://liutianruo-2019-go-go-go.oss-cn-shanghai.aliyuncs.com/images/ArrayList结构图示1.jpg)

# 类注释

1. ArrayList允许添加null值，会自动扩容
2. 是非线程安全的，多线程情况下，推荐使用线程安全类`Collections.synchronizedList`
3. 增强for循环，或者使用迭代器迭代过程，如果数组大小被改变（如remove）操作，会快速失败，抛出异常

# 初始化

ArrayList有三种初始化方式：1. 无参数直接初始化  2. 指定大小初始化  3. 指定初始数据初始化

```java
 public ArrayList() {
     // 无参数初始化 空数组
      this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
 }
// 指定初始数据初始化
 public ArrayList(Collection<? extends E> c) {
        // elementData默认为null
        elementData = c.toArray();
        //如果给定的集合（c）数据有值
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            //如果集合元素类型不是 Object 类型，我们会转成 Object
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // replace with empty array.
            // 给定集合（c）无值，则默认空数组
            this.elementData = EMPTY_ELEMENTDATA;
        }
  }
  // 指定初始容量初始化
  public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
   }
```

+ ArrayList无参数初始化，默认创建空数组，而不是容量为10的数组，第一次add操作时会扩容至10

# 新增与扩容

新增其实就是往数组中添加元素

+ 如果需要扩容，则扩容
+ 否则直接赋值

```java
public boolean add(E e) {
      // 确保数组大小是否足够，如果不够，则扩容
      ensureCapacityInternal(size + 1);  // Increments modCount!!
      // 直接赋值，非线程安全
      elementData[size++] = e;
      return true;
 }
```

## 扩容

```java
 private void ensureCapacityInternal(int minCapacity) {
      ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
 }
 private void ensureExplicitCapacity(int minCapacity) {
        // 记录数组被修改
        modCount++;
		
        // overflow-conscious code
        // 如果我们期望的最小容量大于目前数组的长度，那么就扩容
        if (minCapacity - elementData.length > 0)
            // 扩容
            grow(minCapacity);
 }
  // 计算期望的最小容量
 private static int calculateCapacity(Object[] elementData, int minCapacity) {
        // 如果是空数组（没有设置初始化大小），设置需要的容量
       //如果初始化数组大小时，有给定初始值，以给定的大小为准，不走 if 逻辑
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        return minCapacity;
 }
 // 扩容 
  private void grow(int minCapacity) { // 期望的最小容量
        // overflow-conscious code
        // 旧的数组长度
        int oldCapacity = elementData.length;
        // oldCapacity >> 1  oldCapacity除以2
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        // 如果扩容后的值 < 我们的期望值，扩容后的值就等于我们的期望值
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
       // 如果扩容后的值 > jvm 所能分配的数组的最大值，那么就用 Integer 的最大值
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        // 通过复制进行扩容
        elementData = Arrays.copyOf(elementData, newCapacity);
  }
```

> + 扩容并不是翻倍，而是1.5倍
> + 新增时没有对元素校验，所以元素可以为空

### 扩容的本质

`Arrays.copyOf`

```java
 public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
  }
public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
```

> 本质就是创建新数组（容量一般为旧的数组的1.5倍），然后将旧数组元素拷贝给新数组

# 删除

```java
   public E remove(int index) {
        rangeCheck(index);
		// 记录数组修改次数
        modCount++;
       // 获取旧值
        E oldValue = elementData(index);
		// 数组元素需要移动
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        // 不再引用该数组元素（Garbage）
        elementData[--size] = null; // clear to let GC do its work
        // 返回删除的值
        return oldValue;
    }

 public boolean remove(Object o) {
        // 如果删除null元素，则按照数组顺序删除第一个null
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            // 不是null，只要equals比较为true则删除（按照顺序删除第一个相等的）
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
   }

  private void fastRemove(int index) {
        // 记录数组被修改次数
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // clear to let GC do its work
    }
```

+ 需要关注数组元素对应的equals方法实现

# 迭代器

```java
 private class Itr implements Iterator<E> {
        // 迭代过程中，下一个元素的位置，最开始为0
        int cursor;       // index of next element to return
        // 新增场景：表示上一次迭代场景中，索引的位置  删除场景：-1
        int lastRet = -1; // index of last element returned; -1 if no such
        // expectedModCount 表示迭代过程中，期望的版本号；modCount 表示数组实际的版本号。
        int expectedModCount = modCount;

        Itr() {}
		// 还有没有值可以被迭代
        public boolean hasNext() {
            //cursor 表示下一个元素的位置，size 表示实际大小，如果两者相等，说明已经没有元素可以迭代了，如果不等，说明还可以迭代
            return cursor != size;
        }
		// 如果还有值可以被迭代，获取迭代值
        @SuppressWarnings("unchecked")
        public E next() {
             //迭代过程中，判断版本号有无被修改，有被修改，抛 ConcurrentModificationException 异常
            checkForComodification();
            // 本次迭代过程中元素的位置
            int i = cursor;
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            // 下一个迭代时 迭代元素的位置
            cursor = i + 1;
            // 返回元素值
            return (E) elementData[lastRet = i];
        }

        public void remove() {
            // 上此次迭代的位置 如果重复删除，则抛出异常
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                // 删除元素
                ArrayList.this.remove(lastRet);
                // 修改下一次的迭代位置，因为删除操作会影响元素下表（元素会移动）
                cursor = lastRet;
                // 表示元素被删除，防止重复删除
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        ...
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
     ...
 }
```