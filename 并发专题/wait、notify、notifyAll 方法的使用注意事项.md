[toc]

# 为什么 wait 必须在 synchronized 保护的同步代码中使用？

1. 在使用 wait() 方法时，必须把 wait() 方法写在 `synchronized` 保护的 while 代码块中，并始终判断 执行条件是否满足 
2. 如果满足就往下继续执行，如果不满足就执行 wait() 方法，而在执行 wait() 方法之前，必须持有对象的 monitor 锁，也就是**通常所说的 synchronized 锁 为什么 wait 必须在 synchronized 保护的同步代码中使用** 

> wait method should always be used in a loop: 
>
> ```java
> synchronized (obj) { 
>     while (condition does not hold) 
>         obj.wait(); ... 
>         // Perform action appropriate to condition 
> }
> ```
>
> **This method should only be called by a thread that is the owner of this object's monitor.**

# 为什么 wait/notify/notifyAll 被 定义在 Object 类中，而 sleep 定 义在 Thread 类中？

**因为 Java 中每个对象都有一把称之为monitor 监视器的锁，由于每个对象都可以上锁，这就要求在对象头中有一个用来保存锁信息的位置，这个锁是对象级别的，而非线程级别的，wait/notify/notifyAll 也都是锁级别的操作，它们的锁属于对象所以把它们定义在 Object 类中是最合适，因为 0bject 类是所有对象的父类**

> 果把 wait/notify/notifyAll 方法定义在 Thread 类中，会带来很大的局限性
> 比如一个线程可能持有多把锁，以便实现相互配合的复杂逻辑，假设此时 wait 方法定义在 Thread类
> 如何实现让一个线程持有多把锁呢?又如何明确线程等待的是哪把锁呢?
>
> 然而我们是让当前线程去等待某个对象的锁。自然应该通过操作对象来实现，而不是操作线程

# wait/notify 和 sleep 方法的异同？

**相同点**：

1. 它们都可以让线程阻塞 

2. 它们都可以**响应 interrupt 中断**：**在等待的过程中如果收到中断信号，都可以进行响应， 并抛出 InterruptedException 异常** 

**不同点**：

1. wait 方法必须在 synchronized 保护的代码中使用，而 sleep 方法并没有这个要求 
2.  在同步代码中执行 sleep 方法时，**并不会释放 monitor 锁**，但**执行 wait 方法时会主动释放 monitor 锁**
3.  **sleep 方法中会要求必须定义一个时间，时间到期后会主动恢复**，而对于没有参数的 wait 方法而言，意味着永久等待，直到被中断或被唤醒才能恢复，它并不会主动恢复
4.  **wait/notify 是 Object 类的方法，而 sleep 是 Thread 类的方法**