[toc]

# CMS垃圾回收器

CMS采用的是“标记清理算法”，会产生大量的内存碎片，常用语老年代的垃圾回收。

**CMS垃圾回收器采用的垃圾回收线程和系统工作线程尽量同时执行的模式来处理的。**

![](https://liutianruo-2019-go-go-go.oss-cn-shanghai.aliyuncs.com/images/CMS垃圾回收.png)

1. 初始标记 stop the world
2. 并发标记
3. 重新标记 stop the world
4. 并发清理

## CMS问题

+ CMS垃圾回收会消耗CPU资源

  + 并发标记和并发清理时系统工作线程和垃圾回收线程同时运行，垃圾回收线程会占用CPU资源

+ Concurrent Mode Failure问题

  + CMS会产生**浮动垃圾**

    + 并发清理阶段，Java程序是继续运行的，会在新生代创建一些新的对象，此时新生代触发了一次Minor GC，导致一些对象进入了老年代（Minor GC后Survivor区放不下存活对象）
      + 此时这些对象也没人引用他们，他们就成了浮动垃圾，但此时CMS是处理不了他们的

  + 因此为了保证CMS垃圾回收期间，还有一定的内存空间让一些对象进入老年代，一般会预留一些空间

    + `-XX:CMSInitiatingOccupancyFaction`设置老年代占用多少比率的时候触发CMS垃圾回收

  + 如果CMS垃圾回收期间，放入老年代的对象大小超过了老年代可用内存空间，就会发生Concurrent Mode Failure

    > 一边回收，一边往老年代放入对象 老年代空间不够了
    >
    > + 此时会自动使用Serial Old垃圾回收器替代CMS

+ 内存碎片问题

  + 标记—清理算法
    + 产生内存碎片，内存碎片导致后续对象进入老年代后找不到连续的内存空间了，触发GC
    + 太多的内存碎片，会触发频繁的Full GC
    + `-XX:+UseCMSInitiatingOccupancyOnly`
      + Full GC之后再进行Stop The World  ，进行内存碎片整理，把存活对象往一边挪动
      + `CMSFullGCsBeforeCompaction`
        + 执行多少次Full GC后再执行一次内存碎片整理，默认为0，即每一次Full GC都会进行一次内存整理

关于GC Roots：

> 方法的局部变量和类的静态变量是GC Roots，但是类的实例变量不是GC Roots。

## 思考题1

parnew+cms 的gc，如何调整jvm参数，保证只做young gc

+ 调大新生代大小
+ 调大survivor 区的大小
+ 调大新生代垃圾回收存活下来的对象跃迁进入老年代的参数

## 为什么老年代的垃圾回收这么慢呢

1. 老年代存活对象比新生代多很多，标记比较慢
2. CMS并发清理阶段，因为垃圾对象分布在不连续的内存空间，所以回收垃圾对象比较慢
3. 内存整理stop the world
4.  Concurrent Mode Failure出现时，会使用Serial Old垃圾回收器，单线程,Stop the world

## 触发老年代GC的时机

1. 老年代可用内存大小小于新生代全部对象大小，此时没开启内存担保参数（默认都是开启的）
2. 老年代可用内存大小小于历次进入老年代的对象大小，触发Full GC
3. 新生代Minor GC后存活对象 > Survivor区，进入老年代时，老年代空间不足
4. `XX:+UseCMSInitiatingOccupancyOnly`
   1. 如果老年代可用内存大小大于历次新生代GC后进入老年代的对象平均大小，但老年代使用内存空间超过了设置的比例，也会自动触发Full GC

# JVM调优的思考

> 让常驻对象尽快进入老年代
>
> 调大Survivor区域，减少Minor GC后因Survivor区域不够放直接进入老年代的情况

