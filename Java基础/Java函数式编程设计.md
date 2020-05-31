[toc]

# 理解`@FunctionalInterface`

1. 只能标注接口，不可标记类

2. `@FunctionalInterface`非必选的，可写可不写，编译器会识别我们的接口是否为函数式接口

3. 被`@FunctionalInterface`标记的接口中只可以拥有一个抽象方法

   > 排除**接口的默认方法**、**申明中覆盖Object的公开抽象方法**的统计

## 函数式接口的类型

+ 提供类型   -- `Supplier<T>`
+ 消费类型   --  `Consumer<T>`
+ 转换类型   --  `Function<T,R>`
+ 断定类型   -- `Predicate<T>`
+ 隐藏类型   --`Action`

> 基本上属于数据流转

### 提供类型：`Supplier<T>`

```java
@FunctionalInterface
public interface Supplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get();
}
```

### 消费类型 :`Consumer<T>`

```java
@FunctionalInterface
public interface Consumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t);

    
    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}
```

# 函数式接口设计

## `Supplier<T>` 接⼝定义

+ 基本特点：只出不进
+ 编程范式：作为方法/构造函数、返回值
+ 使用场景：数据来源，代码代替接口

> `Map.computeIfAbsent`

```java
  default V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);
                return newValue;
            }
        }

        return v;
    }
```

```java
public class SupplierDesignDemo {

    public static void main(String[] args) {
        echo("Hello,World"); // 固定的参数

        echo(() -> {
            sleep(1000L);
            return "Labmda,HelloWorld";
        });
        // 可以延迟加载 这种方式更灵活
        echo(SupplierDesignDemo::getMessage); // 变化的实现
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getMessage() {
        sleep(1000L);
        return "Labmda,HelloWorld";
    }

    public static void echo(String message) { // 拉的模式
        System.out.println(message);
    }

    public static void echo(Supplier<String> message) { // 推的模式
        System.out.println(message.get());
    }

}
```

我们参看一下Spring Framework中`Supplier`的设计

```java
//org.springframework.beans.factory.ObjectProvider
default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
}
```

## `Consumer<T> `接⼝设计

+ 基本特点：只进不出
+ 编程范式：作为方法/构造函数
+ 使用场景：执行 Callback

## `Function<T,R>` 接⼝设计

+ 基本特点：有进有出
+ 编程范式：作为方法/构造函数
+ 使用场景：类型转换、业务处理等

`Stream`

```java
<R> Stream<R> map(Function<? super T, ? extends R> mapper);
```

## `Predicate<T>` 接⼝设计

+ 基本特点：`boolean` 类型判断

+ 编程范式：作为方法/构造参数

+ 使用场景：过滤、对象比较等函数式

# `Stream` API设计

## 基本操作

+ 转换：`Stream#map(Function)` 

+ 过滤：`Stream#filter(Predicate)` 

+ 排序：
  + `Stream#sorted()` 
  + `Stream#sorted(Comparator)`

## 类型

+ 串行 Stream（默认类型） 

+ 并行 Stream 
  + 转换并行 Stream：`Stream#parallel()` 
  + 是否为并行 Stream：`Stream#isParallel()`

## 高级操作

+ Collect 操作

  ```java
   public static void main(String[] args) {
          List<Integer> values = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toList());
          // class java.util.ArrayList
          System.out.println(values.getClass());
          values = Stream.of(1, 2, 3, 4, 5).collect(LinkedList::new, List::add, List::addAll);
          // class java.util.LinkedList
          System.out.println(values.getClass());
      }
  ```

+ 分组操作

+  聚合操作

+ flatMap 操作

+ reduce 操作