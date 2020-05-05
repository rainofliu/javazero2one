[toc]

LinkedList是双向链表的数据结构。

# 基本结构

```java
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
    transient int size = 0;

    /**
     * Pointer to first node.
     * Invariant: (first == null && last == null) ||
     *            (first.prev == null && first.item != null)
     */
    transient Node<E> first;

    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) ||
     *            (last.next == null && last.item != null)
     */
    transient Node<E> last;
    ...
     
    private static class Node<E> {
        E item; // 节点值
        Node<E> next; // 指向的下一个节点
        Node<E> prev; // 指向的前一个节点

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
}
```

# 追加(新增)

## 从尾节点开始追加

```java
 /**
     * Links e as last element.
     */
    void linkLast(E e) {
        // 获取尾节点
        final Node<E> l = last;
        // 创建新的节点
        // l 是新节点的前一个节点，当前值是尾节点值
        // e 表示当前新增节点，当前新增节点后一个节点是 null
        final Node<E> newNode = new Node<>(l, e, null);
        // 将新增节点设置为尾节点
        last = newNode;
        //如果链表为空（l 是尾节点，尾节点为空，链表即空），头部和尾部是同一个节点，都是新建的节点
        if (l == null)
            first = newNode;
        else
            //否则把前尾节点的下一个节点，指向当前尾节点。
            l.next = newNode;
        size++;
        // 记录修改次数
        modCount++;
    }
```

## 从头部追加

```java
   /**
     * Links e as first element.
     */
    private void linkFirst(E e) {
        // 暂存头结点
        final Node<E> f = first;
        // f: 前头结点
        // 新建节点，前一个节点指向null，e 是新建节点，f 是新建节点的下一个节点，目前值是头节点的值
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        // 头节点为空，就是链表为空，头尾节点是一个节点
        if (f == null)
            last = newNode;
        else
            //上一个头节点的前一个节点指向当前节点
            f.prev = newNode;
        size++;
        modCount++;
    }
```

> 头部追加节点和尾部追加节点非常类似，只是前者是移动头节点的 prev 指向，后者是移动尾节点的 next 指向。

# 节点删除

## 从头部删除

```java
 public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
 }
 private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        // 获取头结点的值
        final E element = f.item;
        // 获取头结点的下一个节点
        final Node<E> next = f.next;
        // 将头结点的值设置为null
        f.item = null;
        // 头结点的下一个节点设置为null
        f.next = null; // help GC
        // 头结点的下一个节点作为新的头结点
        first = next;
        //如果 next 为空，表明链表为空
        if (next == null)
            last = null;
        else
            // 链表不为空，头节点的前一个节点指向 null
            next.prev = null;
        //修改链表大小和版本
        size--;
        modCount++;
        return element;
  }
```

> 可以看到LinkedList的新增和删除方法都比较简单

# 节点查询

```java
  public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
   }
   // 根据链表索引位置查询节点
   Node<E> node(int index) {
        // assert isElementIndex(index);
		// 如果 index 处于队列的前半部分，从头开始找，size >> 1 是 size 除以 2 的意思。
        if (index < (size >> 1)) {
            Node<E> x = first;
            // 直到 for 循环到 index 的前一个 node 停止
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else { // 如果 index 处于队列的后半部分，从尾开始找
            Node<E> x = last;
            // 直到 for 循环到 index 的后一个 node 停止
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
```

> 从源码中我们可以发现，`LinkedList` 并没有采用从头循环到尾的做法，而是采取了简单二分法，首先看看 index 是在链表的前半部分，还是后半部分。如果是前半部分，就从头开始寻找，反之亦然。通过这种方式，使循环的次数至少降低了一半，提高了查找的性能，这种思想值得我们借鉴。

# 迭代器

## 迭代器的创建

```java
 public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
 }
```

## 迭代原理

```java
  // 双向迭代器  
  private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned; //上一次执行 next() 或者 previous() 方法时的节点位置
        private Node<E> next; // 下一个节点
        private int nextIndex;// 下一个节点的位置
        // expectedModCount：期望版本号；modCount：目前最新版本号
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            // 下一个节点
            next = (index == size) ? null : node(index);
            // 下一个节点的位置
            nextIndex = index;
        }
		// 判断有没有下一个元素
        public boolean hasNext() {
            return nextIndex < size;
        }
		// 获取下一个元素
        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();
			// next 是当前节点，在上一次执行 next() 方法时被赋值的。
            // 第一次执行时，是在初始化迭代器的时候，next 被赋值的
            lastReturned = next;
            // next 是下一个节点了，为下次迭代做准备
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }
		// 如果上次节点索引位置大于 0，就还有节点可以迭代
        public boolean hasPrevious() {
            return nextIndex > 0;
        }
        // 取前一个节点
        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();
			// next 为空场景：1:说明是第一次迭代，取尾节点(last);2:上一次操作把尾节点删除掉了
    		// next 不为空场景：说明已经发生过迭代了，直接取前一个节点即可(next.prev)
            // 相当于lastReturned = next;
            // next = (next == null) ? last : next.prev;
            lastReturned = next = (next == null) ? last : next.prev;
             // 索引位置变化
            nextIndex--;
            return lastReturned.item;
        }
        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();
			
            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

       ...

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
```

# **迭代器删除**

```java
public void remove() {
    checkForComodification();
    // lastReturned 是本次迭代需要删除的值，分以下空和非空两种情况：
    // lastReturned 为空，说明调用者没有主动执行过 next() 或者 previous()，直接报错
    // lastReturned 不为空，是在上次执行 next() 或者 previos()方法时赋的值
    if (lastReturned == null)
        throw new IllegalStateException();
    Node<E> lastNext = lastReturned.next;
    //删除当前节点
    unlink(lastReturned);
    // next == lastReturned 的场景分析：从尾到头递归顺序，并且是第一次迭代，并且要删除最后一个元素的情况下
    // 这种情况下，previous() 方法里面设置了 lastReturned = next = last,所以 next 和 lastReturned会相等
    if (next == lastReturned)
        // 这时候 lastReturned 是尾节点，lastNext 是 null，所以 next 也是 null，这样在 previous() 执行时，发现 next 是 null，就会把尾节点赋值给 next
        next = lastNext;
    else
        nextIndex--;
    lastReturned = null;
    expectedModCount++;
}
```

