package com.ajin.java.program.base.functional;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

/**
 * Lambda表达式Demo
 *
 * @author ajin
 */

public class LambdaDemo {

    @FunctionalInterface
    public interface Action {
        void execute();
    }

    public static void main(String[] args) {
        Action action = () -> {
            System.out.println("Hello World!");
        };
        // 匿名内置类写法
        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                print(evt);
            }
        };
        // Lambda表达式传统写法
        PropertyChangeListener listener1 = (event) -> {
            print(event);
        };
        // Lambda 简略写法
        PropertyChangeListener listener2 = LambdaDemo::print;
    }

    private static void print(Object object) {
        System.out.println(object.toString());
    }

    private static void showSupplier() {
        Supplier<String> stringSupplier = () -> "Hello World!";

    }
}
