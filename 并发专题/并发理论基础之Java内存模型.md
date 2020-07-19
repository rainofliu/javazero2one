[toc]

Java内存模型是用来解决可见性和有序性的。

# 什么是Java内存模型

## 按需禁用

导致可见性的原因是CPU缓存，导致有序性的原因是编译优化，那解决可见性、有序性最直接的办法就是**禁用缓存和编译优化**。

但是我们这样做之后，程序的性能会受到很大的影响。

**合理的方案应该是按需禁用缓存以及编译优化**。那么，如何做到“按需禁用”呢？对于并发程序，何时禁用缓存以及编译优化只有程序员知道，那所谓**“按需禁用”其实就是指按照程序员的要求来禁用**。

> 程序员需要禁用缓存和编译优化时，就按照程序员的需要 来禁用。
>
> **为了解决可见性和有序性问题，只需要提供给程序员按需禁用缓存和编译优化的方法即可。**

## Java内存模型

Java 内存模型是个很复杂的规范，可以从不同的视角来解读，站在我们这些程序员的视角，本质上可以理解为，**Java 内存模型规范了 JVM 如何提供按需禁用缓存和编译优化**的方法。具体来说，这些方法包括 `volatile`、`synchronized` 和 `final` 三个关键字，以及六项 `Happens-Before` 规则。

### 使用`volatile`的困惑

`volatile` 关键字并不是 Java 语言的特产，古老的 C 语言里也有，它最原始的意义就是**禁用 CPU 缓存**。

例如，我们声明一个 `volatile` 变量 `volatile int x = 0`，它表达的是：告诉编译器，对这个变量的读写，不能使用 CPU 缓存，必须从内存中读取或者写入。

> Java 内存模型在 1.5 版本对 volatile 语义进行了增强。怎么增强的呢？答案是一项 `Happens-Before` 规则。

### Happens-Before 规则

> 语义：**前面一个操作的结果对后续操作是可见的**
>
> 就像有心灵感应的两个人，虽然远隔千里，一个人心之所想，另一个人都看得到。

**Happens-Before 约束了编译器的优化行为，虽允许编译器优化，但是要求编译器优化后一定遵守 Happens-Before 规则。**

#### 1. 程序的顺序性规则

​	指在一个线程中，按照程序顺序，前面的操作 Happens-Before 于后续的任意操作

> 一个线程中，**程序前面对某个变量的修改一定是对后续操作可见的**。
>
> 下面的代码中，一个线程执行时，`x=42`  **Happens-Before** `v=true`

```java
class VolatileExample {

    int x = 0;
    volatile boolean v = false;

    public void writer() {
      x = 42;
      v = true;
    }

    public void reader() {
      if (v == true) {      // 这里x会是多少呢？    }  }}

      }
 }
```

#### 2. `volatile`变量规则

**指对一个 volatile 变量的写操作， Happens-Before 于后续对这个 volatile 变量的读操作。**

#### 3. 传递性

**A Happens-Before B，且 B Happens-Before C，那么 A Happens-Before C。**

```java
class VolatileExample {

    int x = 0;
    volatile boolean v = false;

    public void writer() {
      x = 42;
      v = true;
    }

    public void reader() {
      if (v == true) {      // 这里x会是多少呢？    

      }
 }
```

> “x=42” Happens-Before 写变量 “v=true” ，这是规则 1 的内容；写变量“v=true” Happens-Before 读变量 “v=true”，这是规则 2 的内容 。再根据这个传递性规则，我们得到结果：“x=42” Happens-Before 读变量“v=true”。这意味着什么呢？如果线程 B 读到了“v=true”，那么线程 A 设置的“x=42”对线程 B 是可见的。也就是说，线程 B 能看到 “x == 42” 

#### 4. 管程中锁的规则

> **对一个锁的解锁 Happens-Before 于后续对这个锁的加锁**

管程是一种通用的同步原语，在 Java 中指的就是 synchronized，synchronized 是 Java 里对管程的实现。

管程中的锁在 Java 里是隐式实现的，例如下面的代码，在进入同步块之前，会自动加锁，而在代码块执行完会自动释放锁，加锁以及释放锁都是**编译器帮我们实现**的

```java
synchronized (this) { //此处自动加锁
  // x是共享变量,初始值=10
  if (this.x < 12) {
    this.x = 12; 
  }  
} //此处自动解锁
```

#### 5. 线程start()规则

**它是指主线程 A 启动子线程 B 后，子线程 B 能够看到主线程在启动子线程 B 前的操作。**

> 如果线程 A 调用线程 B 的 start() 方法（即在线程 A 中启动线程 B），那么该 start() 操作 Happens-Before 于线程 B 中的任意操作

```java
Thread B = new Thread(()->{
  // 主线程调用B.start()之前
  // 所有对共享变量的修改，此处皆可见
  // 此例中，var==77
});
// 此处对共享变量var修改
var = 77;
// 主线程启动子线程
B.start();
```

#### 6. 线程join规则

**它是指主线程 A 等待子线程 B 完成（主线程 A 通过调用子线程 B 的 join() 方法实现），当子线程 B 完成后（主线程 A 中 join() 方法返回），主线程能够看到子线程的操作。当然所谓的“看到”，指的是对共享变量的操作**

> 如果在线程 A 中，调用线程 B 的 join() 并成功返回，那么线程 B 中的任意操作 Happens-Before 于该 join() 操作的返回

```java
Thread B = new Thread(()->{
  // 此处对共享变量var修改
  var = 66;
});
// 例如此处对共享变量修改，
// 则这个修改结果对线程B可见
// 主线程启动子线程
B.start();
B.join()
// 子线程所有对共享变量的修改
// 在主线程调用B.join()之后皆可见
// 此例中，var==66
```

### final关键字

final 修饰变量时，初衷是告诉编译器：这个变量生而不变，可以随便优化。

[具体参考](http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html#finalWrong)

# 总结

Java 的内存模型是并发编程领域的一次重要创新，之后 C++、C#、Golang 等高级语言都开始支持内存模型。Java 内存模型里面，最晦涩的部分就是 Happens-Before 规则了，Happens-Before 规则最初是在一篇叫做 **Time, Clocks, and the Ordering of Events in a Distributed System** 的论文中提出来的，在这篇论文中，Happens-Before 的语义是一种因果关系。在现实世界里，如果 A 事件是导致 B 事件的起因，那么 A 事件一定是先于（Happens-Before）B 事件发生的，这个就是 Happens-Before 语义的现实理解。

------

**在 Java 语言里面，Happens-Before 的语义本质上是一种可见性，A Happens-Before B 意味着 A 事件对 B 事件来说是可见的，无论 A 事件和 B 事件是否发生在同一个线程里。例如 A 事件发生在线程 1 上，B 事件发生在线程 2 上，Happens-Before 规则保证线程 2 上也能看到 A 事件的发生。**