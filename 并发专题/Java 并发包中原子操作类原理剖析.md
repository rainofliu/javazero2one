[toc]

本文记录学习Java并发包中原子操作类的原理剖析。

> JUC 包提供了一系列的原子性操作类，这些类都是使用非阻塞算法 CAS 现的 ，相比使用锁 实现原子性操作这在性能上有很大提高。

+ 原子操作类（`java.util.concurrent.atomic`）使用的是无锁操作，性能相比较加锁操作更佳
+ 原子操作类的能力有限，只能说对变量的操作等少数场景有效

# `AtomicLong`

```java
public class AtomicLong extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 1927816293512124184L;

    // setup to use Unsafe.compareAndSwapLong for updates
    // 获取Unsafe实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 存放value的偏移量
    private static final long valueOffset;

    /**
     * Records whether the underlying JVM supports lockless
     * compareAndSwap for longs. While the Unsafe.compareAndSwapLong
     * method works in either case, some constructions should be
     * handled at Java level to avoid locking user-visible locks.
     */
    // 判断JVM是否支持Long型的CAS操作
    static final boolean VM_SUPPORTS_LONG_CAS = VMSupportsCS8();
    /**
     * Returns whether underlying JVM supports lockless CompareAndSet
     * for longs. Called only once and cached in VM_SUPPORTS_LONG_CAS.
     */
    private static native boolean VMSupportsCS8();
    
    

    static {
        try {
            // 获取value在AtomicLong中的偏移量
            valueOffset = unsafe.objectFieldOffset
                (AtomicLong.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }
	// 实际变量值 volatile保证多线程场景下的内存可见性
    private volatile long value;
    
    /**
     * Creates a new AtomicLong with the given initial value.
     *
     * @param initialValue the initial value
     */
    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    /**
     * Creates a new AtomicLong with initial value {@code 0}.
     */
    public AtomicLong() {
    }
    ...
}    
```

## `Unsafe`

```java
 public static void main(String[] args) {
    AtomicLong atomicLong = new AtomicLong();
    atomicLong.getAndIncrement();
  }
```

上面是一个很简单的Demo，将`AtomicLong`中的value+1并获取

```java
 /**
     * Atomically increments by one the current value.
     *
     * @return the previous value
     */
    public final long getAndIncrement() {
        return unsafe.getAndAddLong(this, valueOffset, 1L);
    }
```

> 我们看到`AtomicLong#getAndIncrement`方法底层是调用了`Unsafe#getAndAddLong`方法来实现的

### `sun.misc.Unsafe#getAndAddLong`

```java
 public final long getAndAddLong(Object var1, long var2, long var4) {
    long var6;
    do {
      var6 = this.getLongVolatile(var1, var2);
    } while(!this.compareAndSwapLong(var1, var2, var6, var6 + var4));

    return var6;
  }
 public final native boolean compareAndSwapLong(Object var1, long var2, long var4, long var6);
```

> + var1  ： `AtomicLong` 对象
> + var2  :   `valueOffset` 
>   + value变量在`AtomicLong`中的偏移量
> + var4  :  要设置的var2的值

# `LongAdder`

这里Java 8 中新增的原子操作类

前面讲过 AtomicLong 通过 CAS 提供了非阻塞的原子性操作，相比使用阻塞算法的同步器来说它的性能己经很好了，但是 JDK 开发组并不满足于此 使用 AtomicLong 时，**在高并发大量线程会同时去竞争更新 同一个原子变量，但是由于同时只有 一个线程的CAS 操作会成功（CPU指令保证），这就造成了大量线程竞争失败后，会通过无限循环不断进行自旋尝试CAS 操作， 这会白白浪费 CPU的资源。**

> 失败线程会去循环 CAS，高并发场景下性能会受到一定的损耗。

如何解决这个问题呢？可以将一个变量分解为多个变量，让同样多的线程去竞争多个资源



+ 使用`LongAdder`时,则是在内部维护多个`Cell`变量,每个`Cell`里面有一个初始值为0的long型变量,这样,在同等并发量的情况下,争夺单个变量更新操作的线程量会减少,这变相地减少了争夺共享资源的并发量。

+ 另外,多个线程在争夺同一个`Cell`原子变量时如果失败了,它并不是在当前Cell变量上一直自旋CAS重试,而是尝试在其他Cell的变量上进行CAS尝试,这个改变增加了当前线程重试CAS成功的可能性。最后,在获取`LongAdder`当前值时,是把所有Cell变量的value值累加后再加上base返回的。

> ​	`LongAdder` 维护了一个延迟初始化的原子性更新数组（默认情况下Cell数组是null）和一个基值变量 base。**由于Cells占用的内存是相对比较大的,所以一开始并不创建它, 而是在需要时创建,也就是惰性加载。**
> ​	当一开始判断 Cell数组是null并且并发线程较少时,所有的累加操作都是对 base变量进行的。保持Cell数组的大小为2的N次方,在初始化时Cell数组中的Cell元素个数为2,数组里面的变量实体是Cell类型。Cell类型是AtomicLong的一个改进,用来减少缓存的争用,也就是解决**伪共享问题**。
> ​       对于大多数孤立的多个原子操作进行字节填充是浪费的,因为原子性操作都是无规律地分散在内存中的（也就是说多个原子性变量的内存地址是不连续的）,多个原子变量被放入同一个缓存行的可能性很小。但是原子性数组元素的内存地址是连续的,所以数组内的多个元素能经常共享缓存行,因此这里使用`@sun.misc.Contendede`注解对Cell类进行字节填充,这防止了数组中多个元素共享一个缓存行,在性能上是一个提升。