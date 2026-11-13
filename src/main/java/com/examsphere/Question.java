package com.examsphere;

/** Model class for a multiple-choice question. */
public class Question {
    private final int id;
    private final String text;
    private final String optionA, optionB, optionC, optionD;

    public Question(int id, String text, String a, String b, String c, String d) {
        this.id = id;
        this.text = text;
        this.optionA = a;
        this.optionB = b;
        this.optionC = c;
        this.optionD = d;
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
}
