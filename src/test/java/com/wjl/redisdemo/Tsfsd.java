package com.wjl.redisdemo;

public class Tsfsd {

  private static String a="aaa";

    public Tsfsd(){
        a="bbb";
        System.out.println("构造函数");
    }


    public static void tas(){
        System.out.println(a+"static函数");
    }
}
