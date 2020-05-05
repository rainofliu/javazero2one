[toc]



`LinkedList`实现了`java.util.Deque`接口，该接口是一个双端队列，而`java.util.Deque`又实现了`java.util.Queue`接口(队列)

`java.util.ArrayDeque`基于**数组实现了双端队列**，我们来看一下它的实现原理

# 构造器

```java
  public ArrayDeque() {
        elements = new Object[16];
    }
   public ArrayDeque(int numElements) {
        allocateElements(numElements);
    }
   public ArrayDeque(Collection<? extends E> c) {
        allocateElements(c.size());
        addAll(c);
    }
```

# 实现原理

```java
transient Object[] elements; // 存储元素的数组
transient int head; // 
transient int tail;
```

## 循环数组

循环数组是逻辑上的概念，循环是指数组结尾之后可以接着从数组头开始，数组的长度、第一个和最后一个元素都与head和tail这两个变量