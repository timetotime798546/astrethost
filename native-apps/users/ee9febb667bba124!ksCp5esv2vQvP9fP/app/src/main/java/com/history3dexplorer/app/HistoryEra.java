package com.history3dexplorer.app;

public class HistoryEra {
    public final String title;
    public final String year;
    public final String description;
    public final String funFact;
    public final int color;
    public final String triviaQuestion;
    public final String[] triviaOptions;
    public final int correctAnswerIndex;
    public final String triviaExplanation;

    public HistoryEra(String title, String year, String description, String funFact, int color,
                      String triviaQuestion, String[] triviaOptions, int correctAnswerIndex, String triviaExplanation) {
        this.title = title;
        this.year = year;
        this.description = description;
        this.funFact = funFact;
        this.color = color;
        this.triviaQuestion = triviaQuestion;
        this.triviaOptions = triviaOptions;
        this.correctAnswerIndex = correctAnswerIndex;
        this.triviaExplanation = triviaExplanation;
    }
}