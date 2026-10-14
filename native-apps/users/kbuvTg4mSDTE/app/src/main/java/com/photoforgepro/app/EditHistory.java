package com.photoforgepro.app;

import java.util.ArrayList;
import java.util.List;

public class EditHistory {
    private final List<EditState> undoStack = new ArrayList<EditState>();
    private final List<EditState> redoStack = new ArrayList<EditState>();
    private static final int MAX_HISTORY = 30;

    public void record(EditState state) {
        redoStack.clear();
        undoStack.add(state.copy());
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.remove(0);
        }
    }

    public boolean canUndo() {
        return undoStack.size() > 1;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public EditState undo() {
        if (!canUndo()) {
            return null;
        }
        EditState current = undoStack.remove(undoStack.size() - 1);
        redoStack.add(current);
        return undoStack.get(undoStack.size() - 1).copy();
    }

    public EditState redo() {
        if (!canRedo()) {
            return null;
        }
        EditState next = redoStack.remove(redoStack.size() - 1);
        undoStack.add(next);
        return next.copy();
    }
}