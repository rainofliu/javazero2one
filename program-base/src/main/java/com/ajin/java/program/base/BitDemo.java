package com.ajin.java.program.base;

/**
 * Java中位运算 Demo
 *
 * @author ajin
 */

public class BitDemo {
    public static void main(String[] args) {
        int a = 4; // 100
        int b = a << 2; // 左移2位就是1
        int c = a >> 2; // 右移2位 001
        //  a左移两位：16
        // a右移两位：1
        System.out.println("a左移两位：" + b);
        System.out.println("a右移两位：" + c);
    }
}
