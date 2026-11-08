package com.qnapdftoppt.app;

public class ConversionRecord {
    private String id;
    private String title;
    private int questionCount;
    private String fileName;
    private String createdAt;

    public ConversionRecord(String id, String title, int questionCount, String fileName, String createdAt) {
        this.id = id;
        this.title = title;
        this.questionCount = questionCount;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getQuestionCount() { return questionCount; }
    public String getFileName() { return fileName; }
    public String getCreatedAt() { return createdAt; }

    // Offline logic calculation
    public int getTotalSlides() {
        return questionCount + 1; // 1 Title Slide + N Q&A Slides
    }
}