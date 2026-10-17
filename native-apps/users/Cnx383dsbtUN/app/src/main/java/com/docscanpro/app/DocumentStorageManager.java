package com.docscanpro.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentStorageManager {
    private static final String PREF_NAME = "docscan_prefs";
    private static final String KEY_DOCS = "saved_documents";
    private final Context context;
    private final SharedPreferences prefs;

    public DocumentStorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<DocumentItem> getAllDocuments() {
        List<DocumentItem> list = new ArrayList<DocumentItem>();
        String jsonStr = prefs.getString(KEY_DOCS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                DocumentItem doc = DocumentItem.fromJsonObject(obj);
                if (doc != null) {
                    list.add(doc);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveDocument(DocumentItem doc) {
        List<DocumentItem> list = getAllDocuments();
        // Insert at beginning for newest first
        list.add(0, doc);
        persistList(list);
    }

    public synchronized boolean deleteDocument(String id) {
        List<DocumentItem> list = getAllDocuments();
        DocumentItem toDelete = null;
        for (int i = 0; i < list.size(); i++) {
            DocumentItem item = list.get(i);
            if (item.getId().equals(id)) {
                toDelete = item;
                list.remove(i);
                break;
            }
        }
        if (toDelete != null) {
            if (toDelete.getImagePath() != null) {
                File imgFile = new File(toDelete.getImagePath());
                if (imgFile.exists()) imgFile.delete();
            }
            if (toDelete.getPdfPath() != null) {
                File pdfFile = new File(toDelete.getPdfPath());
                if (pdfFile.exists()) pdfFile.delete();
            }
            persistList(list);
            return true;
        }
        return false;
    }

    public synchronized void clearAll() {
        List<DocumentItem> list = getAllDocuments();
        for (int i = 0; i < list.size(); i++) {
            DocumentItem doc = list.get(i);
            if (doc.getImagePath() != null) new File(doc.getImagePath()).delete();
            if (doc.getPdfPath() != null) new File(doc.getPdfPath()).delete();
        }
        prefs.edit().remove(KEY_DOCS).apply();
    }

    private void persistList(List<DocumentItem> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            arr.put(list.get(i).toJsonObject());
        }
        prefs.edit().putString(KEY_DOCS, arr.toString()).apply();
    }

    public File getDocumentsDirectory() {
        File dir = new File(context.getFilesDir(), "scanned_docs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}