# Java异常处理机制

**Java异常处理机制是抛出异常后退出程序，异常发生点后的代码都不会执行。**

+ `NullPointerException`   空指针
  + 如果访问了空对象的属性或方法 ，Java虚拟机会抛出`NullPointerException`异常
+ `NumberFormatException`
  + `Integer.parseInt`可能会抛出该异常

> 抛出异常，按照Java中的异常处理机制，会退出程序

**throw 和return关键字的简单比较**

+ throw  异常退出，return 正常退出
+ return返回位置是固定的，而throw后执行哪一行代码是不固定的（由异常处理机制动态决定）

1. 异常可以由Java虚拟机（系统）触发，也可以通过throw语句触发
2. 可以通过try catch 捕获异常并处理
   + catch住异常后，try语句内发生异常后的代码不会执行
   + 当catch处理异常后，程序会继续往下执行

## 异常类

+ `java.lang.Throwable`
  + `java.lang.Exception`
  + `java.lang.Error`

### `Throwable`

```java
 public Throwable() {
        fillInStackTrace();
 } 
 public Throwable(String message) {
        fillInStackTrace();
        detailMessage = message;
 }
 public Throwable(String message, Throwable cause) {
        fillInStackTrace();
        detailMessage = message;
        this.cause = cause;
 }	
 public Throwable(Throwable cause) {
        fillInStackTrace();
        detailMessage = (cause==null ? null : cause.toString());
        this.cause = cause;
  }
```

+ message  : 异常信息

+ cause  : 触发该异常的其他异常

  > 异常可以形成一个异常链，上层的异常由底层异常触发，cause表示底层异常

```java
 public synchronized Throwable initCause(Throwable cause) {
        if (this.cause != this)
            throw new IllegalStateException("Can't overwrite cause with " +
                     Objects.toString(cause, "a null"), this);
        if (cause == this)
            throw new IllegalArgumentException("Self-causation not permitted", this);
        this.cause = cause;
        return this;
    }
```

> 我们可以通过该方法设置底层异常cause，而且仅限于调用的构造方法中没设置cause，也就是说initCause智能调用一次，否则会抛出`IllegalStateException`异常

```java
   public synchronized Throwable fillInStackTrace() {
        if (stackTrace != null ||
            backtrace != null /* Out of protocol state */ ) {
            fillInStackTrace(0);// native方法
            stackTrace = UNASSIGNED_STACK;
        }
        return this;
    }
    private native Throwable fillInStackTrace(int dummy);
```

> 该方法用来保存异常栈信息

```java
// 打印异常信息到标准错误输出流 
public void printStackTrace() {
      printStackTrace(System.err);
 }
// 获取异常信息
 public String getMessage() {
      return detailMessage;
 }
// 获取异常的cause
public synchronized Throwable getCause() {
     return (cause==this ? null : cause);
 }
// 获取异常栈每一层的信息
public StackTraceElement[] getStackTrace() {
     return getOurStackTrace().clone();
}
private synchronized StackTraceElement[] getOurStackTrace() {
        // Initialize stack trace field with information from
        // backtrace if this is the first call to this method
       if (stackTrace == UNASSIGNED_STACK ||
           (stackTrace == null && backtrace != null) /* Out of protocol state */) {
           int depth = getStackTraceDepth();
           stackTrace = new StackTraceElement[depth];
           for (int i=0; i < depth; i++)
               stackTrace[i] = getStackTraceElement(i);
       } else if (stackTrace == null) {
           return UNASSIGNED_STACK;
       }
       return stackTrace;
}
```

再看一下`StackTraceElement`

```java
public final class StackTraceElement implements java.io.Serializable {
    // Normally initialized by VM (public constructor added in 1.5)
    private String declaringClass;
    private String methodName;
    private String fileName;
    private int    lineNumber;
    ...
}
```

### 异常类体系

![](https://liutianruo-2019-go-go-go.oss-cn-shanghai.aliyuncs.com/images/Java异常类体系.png)

+ `Throwable`
  + `Error` 
    + 系统错误或者资源耗尽
      + 虚拟机错误
      + 内存溢出错误
      + 栈溢出错误
  + `Exception`
    + 应用程序错误
      + IOException（输入输出IO异常）
        + 受检异常
      + RuntimeException 运行时异常
        + 未受检异常
      + SQLException  数据库SQL异常
        + 受检异常
    + 受检异常，编译器会强制要求程序员处理
    + 非受检异常，则没有这个限制

+ 自定义异常

  继承Exception类（或其子类），覆盖构造方法即可

  > 无需额外添加属性和方法

### 异常处理

1. try catch 处理

2. try catch 后再抛出异常

3. finally释放资源

4. throws

   + **声明方法可能会抛出的异常**

   + 并且该方法没有对这些异常处理，而是交给调用方处理

   + 编译器强制throws  受检异常，如IOException

     > 当然，是在没有try catch的前提下