[toc]



# Java面向对象设计

## Java类/接口设计

### 通用设计

#### 类/接口名

+ 模式：（形容词）+名词
+ 举例：
  + 单名词：java.lang.String
  + 双名词：java.util.ArrayList
    + Array
    + List
  + 形容词+名词：java.util.LinkedList

#### 可访问性

##### 四种修饰符

+ public 

+ protected   子类和当前包

  > **不能用于修饰最外层类**

+ 默认（default）  当前包

+ private 

  > **不能用于修饰最外层类，可修饰内置类，内置类其实就是一种特殊的类成员**

> 可用于修饰最外层类的修饰符只有public和（default）
>
> ```java
> public class A{
>     
> }
> class B{
>     
> }
> ```

+ public

  + 开放API场景

    > `java.lang.String`

+ （默认）

  + 仅在当前package使用，属于私有API

    > `java.io.FileSystem`
    >
    > ```java
    > abstract class FileSystem {
    >     
    > }
    > class WinNTFileSystem extends FileSystem {
    >     
    > }
    > ```

#### 可继承性

+ final    -- `java.lang.String`

  > final不具备继承性，仅用于**实现类**，**不能与abstract关键字修饰类**
  >
  > final是最终的意思，不能被扩展，所以它不能修饰抽象类

  **String为何是final类**？

+ 非final

  > 最常见的设计类手段，可继承性依赖于**可访问性**

### 具体类设计

#### 常见场景

+ 功能组件
  + HashMap
+ 接口/抽象类实现
  + HashMap <- AbstractMap <- Map
+ 数据对象
  + POJO  getter/setter
+ 工具辅助
  + Utils
  + Helper

#### 命名模式

+ 前缀
  + Default
  + Generic
    + GenericBeanDefinition
  + Common
  + Base
+ 后缀
  + Impl

### 抽象类设计

#### 常见场景

+ 接口通用实现（模板模式）	
  + AbstractList
+ 状态/行为继承
+ 工具类
  + CollectionUtils

**抽象类是介于具体类和接口之间的，Java8接口有默认实现，可以替代抽象类**

#### 常见模式

+ 抽象程度介于类和接口之间，Java8+ 可以完全用接口替代
  + 但是接口不可以有`static`方法，static是类方法，是面向过程；而接口是多态的体现，是面向对象，所以不能有static方法
+ Asbtract/Base前缀
  + `java.util.AbstractCollection`
  + `javax.sql.rowset.BaseRowSet`

### 内置类设计

#### 常见场景

+ 临时数据存储类 :	`java.lang.ThreadLocal.ThreadMap<WeakReference>`
  + 数据不希望外部访问
+ 特殊用途的API实现 :  `java.util.Collections.UnmodifiableCollection`
+ Builder模式（接口） ： `java.util.stream.Streams.Builder`

**内部类什么时候用public 什么时候用private 什么时候用static**

+ 需要公开的情况 public static合用
+ private 私有

### 接口设计

#### 常见场景

+ 上下游系统（组件）通讯契约
  + API
  + RPC
+ 常量定义
+ 标记接口
  + Serializable
  + Cloneable
  + EventListener
  + AutoCloseable

#### 常见模式

+ 无状态（Stateless）

+ 完全抽象（< Java 8）

  > 全部是抽象方法

  ```java
  public interface Comparable<T> {
      public int compareTo(T o);
  }
  ```

+ 局部抽象 (Java8 +)

  > 接口有default实现
  >
  > 如 Spring 5.x中的`BeanPostProcessor`

+ 单一抽象（函数式接口）

## 枚举设计

### “枚举类“

#### 场景：

Java枚举（enum）引入之前的模拟枚举实现类

#### 模式

+ 成员用常量表示，并且类型为当前类型
+ 常用关键字final修饰类
  + 不让继承
  + 非抽象，枚举类是具体的
+ 非public构造器

```java
final class Counting {
    public static final Counting ONE = new Counting();
    public static final Counting TWO = new Counting();
    public static final Counting THREE = new Counting();
    public static final Counting FOUR = new Counting();
    public static final Counting FIVE = new Counting();


    private Counting() {

    }

}
```

### Java枚举
我们定义一个枚举：

```java
enum CountingEnum {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE;
}
```

通过javap命令反编译看一下：

> D:\gitRepository\organized-learning\deep-in-java\stage-1\lesson2\target\classes>javap com.ajin.deep.in.java.CountingEnum
> Compiled from "EnumClassDemo.java"

```java
final class com.ajin.deep.in.java.CountingEnum extends java.lang.Enum<com.ajin.deep.in.java.CountingEnum> {
    // 成员
  public static final com.ajin.deep.in.java.CountingEnum ONE;
  public static final com.ajin.deep.in.java.CountingEnum TWO;
  public static final com.ajin.deep.in.java.CountingEnum THREE;
  public static final com.ajin.deep.in.java.CountingEnum FOUR;
  public static final com.ajin.deep.in.java.CountingEnum FIVE;
    
  public static com.ajin.deep.in.java.CountingEnum[] values();
    
  public static com.ajin.deep.in.java.CountingEnum valueOf(java.lang.String);
    
  static {};
}
```

#### 基本特性

+ 类结构：强类型

  > CountingEnum就是它的强类型

  ```java
  final class com.ajin.deep.in.java.CountingEnum extends java.lang.Enum<com.ajin.deep.in.java.CountingEnum> {
     ...
  }
  ```

+ 继承`java.lang.Enum`

  ```java
  public abstract class Enum<E extends Enum<E>>
          implements Comparable<E>, Serializable {
      ...
      /**
       *  枚举常量的名称
       * The name of this enum constant, as declared in the enum declaration.
       * Most programmers should use the {@link #toString} method rather than
       * accessing this field.
       */
      private final String name;
      // 枚举值定义的位置
      private final int ordinal;
      ...
  }        
  ```

+ 不可显式地继承和被继承 ,但是可以实现接口

  > 枚举本身继承了`java.lang.Enum`
  >
  > + 枚举被final修饰，所以无法被继承
  > + 而且其继承了Enum类，根据java单一继承原则可知，枚举不可继承其他类

+ 枚举的`values()`方法其实是Java编译器做的字节码提升

  ```java
   public static com.ajin.deep.in.java.CountingEnum[] values();
      Code:
         0: getstatic     #1                  // Field $VALUES:[Lcom/ajin/deep/in/java/CountingEnum;
         3: invokevirtual #2                  // Method "[Lcom/ajin/deep/in/java/CountingEnum;".clone:()Ljava/lang/Object;
         6: checkcast     #3                  // class "[Lcom/ajin/deep/in/java/CountingEnum;"
         9: areturn
  ```

+ 枚举其实是final class

+ 枚举成员修饰符为public static final

+ 枚举可以有抽象方法

  + 虽然是final类 ( 具体类），但仍然有抽象方法

  ```java
  enum CountingEnum implements Cloneable{  // 可以实现接口
      ONE(1) {
          @Override
          public String valueAsString() {
              return String.valueOf(getValue());
          }
      },
      TWO(2){
          @Override
          public String valueAsString() {
              return String.valueOf(getValue());
          }
      },
      THREE(3){
          @Override
          public String valueAsString() {
              return String.valueOf(getValue());
          }
      },
      FOUR(4){
          @Override
          public String valueAsString() {
              return String.valueOf(getValue());
          }
      },
      FIVE(5){
          @Override
          public String valueAsString() {
              return String.valueOf(getValue());
          }
      };
  
      private int value;
  
      CountingEnum(int value) {
          this.value = value;
      }
  
      public int getValue() {
          return value;
      }
  
      public void setValue(int value) {
          this.value = value;
      }
  
      public abstract String valueAsString();
  }
  ```

+ 枚举可以序列化，Enum类实现了Serializable接口

#### 成员设计

#### 构造器设计

#### 方法设计

> 关于成员设计、构造器设计和方法设计在基本特性中可以找到

## 泛型设计

since JAVA 5

> 枚举、泛型、反射、注解都是JDK1.5以上才有的

### 泛型使用场景

> 泛型就是**编译时**的辅助

+ 编译时**强类型**检查

  + Java是静态语言，编译后生成字节码，字节码由JVM解释执行或者JIT编译器二次编译
  + Java分为**编译时和运行时**，编译时存在强类型检查

  ```java
     // 没有泛型的情况下，无法进行强类型检查，有可能add String   也可能add Integer
     List list = new ArrayList();
     list.add("a");
     list.add(1);
  ```

+ 避免类型强转

  ```java
  private static void exchange(List a, List b) {
       a.addAll(b);
       // 这里 无法确定a中某个元素的类型，所以只能采取类型强转的方式来获取某个元素的值
       Integer value=(Integer)a.get(0);
  }
  ```

+ 实现通用算法

### 泛型类型

a generic type is a generic class or interface that is parameterized over types.

+ 调用泛型类型

+ 实例化泛型

+ Java7 Diamond语法

  `List<String> list=new ArrayList<>();`

+ 类型参数命名约定

  + E  : 表示集合类型 (Element)

  + V : 表示数值 (Value)

  + K : 表示键(Key)

    ```java
    public interface Map<K,V> {...}
    ```

  + T  :  表示类型

    > T    U  R

    ```java
    @FunctionalInterface
    public interface BiConsumer<T, U> {
        // 两个类型
    }
    @FunctionalInterface
    public interface BiFunction<T, U, R> {
        // 三个类型
    }
    ```

> 多思考设计上的问题

### 泛型有界类型参数

#### 单界限

> 给泛型参数设置单一界限，非我族类不能进来

```java
// extends 声明上限, E的上限类型是CharSequence
public static class Container<E extends CharSequence> {
        private E element;

        public Container(E e) {
            this.element = e;
        }
  }
  Container<String> a = new Container("String");
  // 编译错误，E只能是CharSequence和其子类
  Container<Integer> b = new Container("String");
  Container<StringBuffer> bufferContainer=new Container("1");
```

> Java泛型对象操作时，看申明对象**泛型参数类型**

#### 多界限

```java
   // 多界限绑定 extends 第一个类型可以是具体类(也可以是接口），第二个或更多参数类型是接口
    public static class Template<T extends C & I & I2> {

    }

    public static class TClass extends C implements I, I2 {

    }
```

#### 泛型方法和有界类型参数

```java
 public static <C extends Iterable<E>, E extends Serializable> void forEach(C source, Consumer<E> consumer) {
        for (E e : source) {
            consumer.accept(e);
        }
    }
```

### 泛型通配符设计

> 可参考Effective Java关于泛型部分的讲解

### 泛型类型擦写

Generics were introduced to the Java language to provide tighter type checks at compile time and to support generic programming. To implement generics, the Java compiler applies type erasure to: 
- Replace all type parameters in generic types with their bounds or `Object` if the type parameters are unbounded. 
  The produced bytecode, therefore, contains only ordinary classes, interfaces, and methods. 

- Insert type casts if necessary to preserve type safety. 

  > 运行时泛型被擦写为Object ,程序获取List中的参数时，需要将Object强转成String类型

  ```java
   List<String> list = new ArrayList<>();
   // String value =(String)list.get(0);
   String value = list.get(0);
  
    // 如下是反编译上述java代码对应的字节码所得
    List<String> list = new ArrayList();
    String value = (String)list.get(0);
  ```

- Generate bridge methods to preserve polymorphism（多态） in extended generic types. 

  **生成桥接方法以保留扩展泛型类型中的多态性。** ==???==

  > Type erasure ensures that no new classes are created for parameterized types; consequently, generics incur no runtime overhead
  >
  > 类型擦除确保不会为参数化类型创建新类;因此，泛型不会产生运行时开销

## 方法设计

### 方法返回类型设计

原则一： 返回类型需要抽象（强类型），除了Object

> 抽象返回类型的意义：**调用方（接收方）容易处理**
>
> 越具体，越难以通用。
>
> 如果返回结合类型，Collection优于List或Set；
>
> 如果不考虑写操作的话，Iterable优于Collection

原则二： 尽量返回Java集合框架内的接口，尽量避免数组

> + Collection和数组相比，拥有更多的操作方法，比如add
> + Collection接口返回时，可以限制只读，而数组不行
>   + 数组尽管长度不变，但是无法保证只读

原则三：确保你的集合返回接口只读

`Collections.unmodifiableCollection()`

原则四：如果需要非只读集合返回的话，那么确保返回快照

+ 如果需要返回快照，尽可能选择ArrayList

> 从优秀的源码慢慢模仿，形成自己的代码。

### 方法参数

参数命名需要让调用方容易看懂

> 多看英文文档，培养英语语感