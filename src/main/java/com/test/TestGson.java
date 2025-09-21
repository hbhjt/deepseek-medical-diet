package com.test;


import com.google.gson.Gson;

public class TestGson {
    public static void main(String[] args) {
        Student student = new Student(1,"张三");
        Gson gson = new Gson();
        String requestBodyJson = gson.toJson(student);



//        String s = JSONObject.toJSON(student).toString();
//        System.out.println(requestBodyJson);
//        System.out.println(s);
    }
}
class Student{
    private int sid;
    private String name;

    public Student(int sid, String name) {
        this.sid = sid;
        this.name = name;
    }
}