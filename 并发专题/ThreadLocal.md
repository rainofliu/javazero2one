[toc]

ThreadLocal作为线程本地变量（每个线程都独享一份），有效的实现了线程安全。

# 构造

```java
/**
     * Creates a thread local variable.
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }
```

> 我们看到ThreadLocal构造器上的注释说可以通过withInitial方法来初始化。

```java
   public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }
```

```java
static final ThreadLocal<String> booleanValue = ThreadLocal.withInitial(() -> "hello, its me!");
```

```java
   static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }
```

> 这里是函数式编程的写法，将`Supplier`作为构造器函数传入，然后在ThreadLocal.get方法中会调用`SuppliedThreadLocal#initialValue`方法来获取`ThreadLocal`的初始值。
>
> 这就是函数式编程的思想，传入一串代码（一个函数）作为入参，并且按需执行（懒加载）。
>
> 对于`Supplier`来说，它更像一个容器，容纳了一个返回值，这个返回值可以固定，也可以变化，只有调用get方法才能直接获取到返回值。

# get方法

获取当前线程实际存储的对象

```java
 public T get() {
     // 获取当前线程
        Thread t = Thread.currentThread();
     // 获取ThreadLocalMap
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            // 获取entry
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                // 获取value
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                // 将结果返回
                return result;
            }
        }
        // 设置初始化值
        return setInitialValue();
    }
```

## `getMap`

```java
 ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
   }
```

`ThreadLocalMap`是Thread类的成员变量

`ThreadLocal.ThreadLocalMap threadLocals = null;`

+ 如果get方法是第一次执行，并且没有执行set方法，则此时的`ThreadLocalMap`为空

  + `setInitialValue`

    ```java
     private T setInitialValue() {
            T value = initialValue();
            Thread t = Thread.currentThread();
            ThreadLocalMap map = getMap(t);
         // map==null
            if (map != null)
                map.set(this, value);
            else
                createMap(t, value);
            return value;
        }
    ```

    + `initialValue`

      ```java
      // 默认的方法返回null，我们一般都会覆写该方法，否则会出现第一次get到null的情况（空指针）
      protected T initialValue() {
              return null;
          }
      ```

      ```java
      // SupplierThreadLocal
      @Override
              protected T initialValue() {
                  return supplier.get();
              }
      ```

  + `createMap`

    ```java
    void createMap(Thread t, T firstValue) {
            t.threadLocals = new ThreadLocalMap(this, firstValue);
       }
      private static final int INITIAL_CAPACITY = 16;
     // 构造函数
     ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
                table = new Entry[INITIAL_CAPACITY]; // 16
         		// hash值和15 &运算 ，获取数组下标
                int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
                // 创建数组元素 新建Map存入第一个元素不用考虑hash冲突
                table[i] = new Entry(firstKey, firstValue);
                // 设置size
                size = 1;
                // 设置扩容的阈值
                setThreshold(INITIAL_CAPACITY);
            }
     /**
             * Set the resize threshold to maintain at worst a 2/3 load factor.
             */
            private void setThreshold(int len) {
                threshold = len * 2 / 3;
            }
    ```

  + `java.lang.ThreadLocal.ThreadLocalMap#set`

    ```java
      private void set(ThreadLocal<?> key, Object value) {
    
                // We don't use a fast path as with get() because it is at
                // least as common to use set() to create new entries as
                // it is to replace existing ones, in which case, a fast
                // path would fail more often than not.
          
    			// 获取Entry 数组
                Entry[] tab = table;
                // 获取数组长度
                int len = tab.length;
                // 计算出数组下标
                int i = key.threadLocalHashCode & (len-1);
    				
                // 从计算出的下标 往后顺序遍历Entry[]中的元素
          
                for (Entry e = tab[i];
                     e != null;
                     e = tab[i = nextIndex(i, len)]) {
                    
                    // 获取key : ThreadLocal
                    ThreadLocal<?> k = e.get();
    				
                    // 将之前存的k和将要存储的key比较，如果是同一对象，则替换value
                    if (k == key) {
                        e.value = value;
                        return;
                    }
    				
                    // 这个位置之前没有存储元素
                    if (k == null) {
                        replaceStaleEntry(key, value, i);
                        return;
                    }
                }
          
    			// 保存元素
                tab[i] = new Entry(key, value);
                int sz = ++size;
          		
                if (!cleanSomeSlots(i, sz) && sz >= threshold)
                    // 扩容
                    rehash();
     }
       private void rehash() {
                expungeStaleEntries();
    
                // Use lower threshold for doubling to avoid hysteresis
                if (size >= threshold - threshold / 4)
                    resize();
      }
    ```

    

# 缺点

> 子线程无法获取父线程的ThreadLocal参数，`InheritableThreadLocal`解决了这个问题

## `InheritableThreadLocal`

新建一个线程会调用下面的init方法，其中对

```java
private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
     // 创建我这个线程的线程 是我的父线程
     Thread parent = currentThread();   
    ....
         // 初始化inheritableThreadLocals
        // inheritThreadLocals :默认 true
        if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
    
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;

        /* Set thread ID */
        tid = nextThreadID();
    }
```

```java
 static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
     return new ThreadLocalMap(parentMap);
 }
```

