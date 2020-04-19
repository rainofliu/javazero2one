# String原理总结

## 1. String为什么用final修饰

这样设计的原因是什么

## 2. String中的value真的不可变吗？

不是的，从Java 5开始，我们可以通过反射来修改String对象中value的值

```java
        String value = "hello";
        // 面向对象规则：一切对象要new

        // 合法的写法（但会被视作异类）
        String value2 = new String("hello");

        System.out.println("value :"+value);
        System.out.println("value2 :"+value2);

        // 从Java1.5开始，对象的属性可以通过反射来修改
        char[] chars = "World".toCharArray();
        // 获取String当中的value字段
        Field valueField =String.class.getDeclaredField("value");
        // 设置value字段可以被修改
        valueField.setAccessible(true);
        // 修改value值
       valueField.set(value2,chars);

        System.out.println("value :"+value);
        System.out.println("value2 :"+value2);
```

> value :hello
> value2 :hello
> value :hello
> value2 :World
>
> **此时value2已被修改为World**