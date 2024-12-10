package com.example.markscalculator.models;

public class StudentData {
    private final String name;
    private final String usn;
    private double exam1;
    private double exam2;
    private double exam3;

    public StudentData(String name, String usn, double exam1, double exam2, double exam3) {
        this.name = name;
        this.usn = usn;
        this.exam1 = exam1;
        this.exam2 = exam2;
        this.exam3 = exam3;
    }

    public StudentData(String name, String usn) {
        this(name, usn, 0.0, 0.0, 0.0);
    }

    public String getName() {
        return name;
    }

    public String getUsn() {
        return usn;
    }

    public double getExam1() {
        return exam1;
    }

    public double getExam2() {
        return exam2;
    }

    public double getExam3() {
        return exam3;
    }

    public void setExam1(double exam1) {
        this.exam1 = exam1;
    }

    public void setExam2(double exam2) {
        this.exam2 = exam2;
    }

    public void setExam3(double exam3) {
        this.exam3 = exam3;
    }

    public double getTotal() {
        return exam1 + exam2 + exam3;
    }

    public double getAverage() {
        return getTotal() / 3;
    }

    @Override
    public String toString() {
        return "StudentData{" +
                "name='" + name + '\'' +
                ", usn='" + usn + '\'' +
                ", exam1=" + exam1 +
                ", exam2=" + exam2 +
                ", exam3=" + exam3 +
                '}';
    }
}