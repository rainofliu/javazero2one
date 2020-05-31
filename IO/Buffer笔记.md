[toc]

Buffer是缓冲区，是包在一个对象内的基本数据元素数组。

# 属性

```java
    private int mark = -1; // 一个备忘位置（标记）
    private int position = 0; // 下一个要被读、写的元素的索引 （位置）
    private int limit; // 缓冲区中第一个不能被读或者写的元素（缓冲区中现存元素的计数） 上界
    private int capacity; // 缓冲区中能够容量的数据元素的最大数量（固定） 容量
```



```java
public class CharBufferDrain {

    private static int index = 0;

    private static String[] strings = {
            "A random string value",
            "The product of an infinite number of monkeys",
            "Hey hey we're the Monkees",
            "Opening act for the Monkees: Jimi Hendrix",
            "'Scuse me while I kiss this fly", // Sorry Jimi ;-)
            "Help Me! Help Me!",
    };

    public static void main(String[] args) {
        CharBuffer buffer = CharBuffer.allocate(100);
        while (fillBuffer(buffer)) {
            buffer.flip();
            drainBuffer(buffer);
            buffer.clear();
        }
    }

    private static void drainBuffer(CharBuffer buffer) {
        while (buffer.hasRemaining()) {
            System.out.println(buffer.get());
        }
        System.out.println(" ");
    }

    /**
     * 填充缓冲区
     */
    private static boolean fillBuffer(CharBuffer buffer) {

        if (index >= strings.length) {
            return false;
        }
        String string = strings[index++];
        for (int i = 0; i < strings.length; i++) {
            buffer.put(string.charAt(i));
        }
        return true;
    }
}
```

