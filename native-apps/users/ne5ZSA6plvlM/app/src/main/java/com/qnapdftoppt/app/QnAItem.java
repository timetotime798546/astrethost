package com.qnapdftoppt.app;

import java.io.Serializable;

public class QnAItem implements Serializable {
    private int id;
    private String question;
    private String answer;
    private String category; // e.g. Short Answer, Long Answer

    public QnAItem() {
    }

    public QnAItem(int id, String question, String answer) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.category = "General Q&A";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}