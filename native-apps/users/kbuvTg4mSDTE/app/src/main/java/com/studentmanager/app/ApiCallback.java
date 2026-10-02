package com.studentmanager.app;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String errorMsg);
}