package com.wjl.redisdemo;

import org.springframework.stereotype.Component;

@Component
public class ComponetTest {

    private String a="aaa";

    private String b="bbb";


    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }
}
