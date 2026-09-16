package com.nativeimagetextreplacer.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class EditorView extends View {
    private Bitmap bitmap;
    private float scale = 1.0f;
    private float dx = 0.0f;
    private float dy = 0.0f;
    private RectF imageBounds = new RectF();
    
    // Core edit layers
    private ArrayList<TextBlock> textBlocks = new ArrayList<TextBlock>();
    private TextBlock selectedBlock = null;

    private ArrayList<BrushStroke> brushStrokes = new ArrayList<BrushStroke>();
    private ArrayList<BrushStroke> undoneStrokes = new ArrayList<BrushStroke>();
    private BrushStroke currentStroke = null;

    private ArrayList<ImageOverlay> overlays = new ArrayList<ImageOverlay>();
    private ImageOverlay selectedOverlay = null;
    
    // Modes
    public static final int EDIT_MODE_TEXT = 0;
    public static final int EDIT_MODE_BRUSH = 1;
    public static final int EDIT_MODE_OVERLAY = 2;
    private int activeEditMode = EDIT_MODE_TEXT;

    // Gesture control states
    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DRAG_BLOCK = 1;
    private static final int TOUCH_MODE_RESIZE_BLOCK = 2;
    private static final int TOUCH_MODE_DRAG_OVERLAY = 3;
    private static final int TOUCH_MODE_RESIZE_OVERLAY = 4;
    private static final int TOUCH_MODE_DRAG_DROP_NEW = 5;

    private int touchMode = TOUCH_MODE_IDLE;
    private int activeHandle = -1;
    
    private float lastTouchBmpX = 0f;
    private float lastTouchBmpY = 0f;
    
    // Brush characteristics
    private float brushSize = 30f; // stored in bitmap units
    private boolean showCursor = false;
    private float cursorX = -1f;
    private float cursorY = -1f;

    private EditorListener listener;

    public interface EditorListener {
        void onBlockSelected(TextBlock block);
        void onBlockDeselected();
        void onBlockStyleUpdated(TextBlock block);
    }

    public EditorView(Context context) {
        super(context);
        init();
    }

    public EditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        createPlaceholderBitmap();
    }

    private void createPlaceholderBitmap() {
        int w = 800;
        int h = 1000;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        
        Paint p = new Paint();
        p.setColor(0xFFF2F3F5);
        canvas.drawRect(0, 0, w, h, p);
        
        p.setColor(0xFFE2E4E8);
        p.setStrokeWidth(2f);
        for (int i = 0; i < w; i += 80) {
            canvas.drawLine(i, 0, i, h, p);
        }
        for (int j = 0; j < h; j += 80) {
            canvas.drawLine(0, j, w, j, p);
        }
        
        p.setColor(0xFF222222);
        p.setTextSize(46f);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("IMAGE TEXT REPLACER V10", w/2f, 150f, p);
        
        p.setTextSize(34f);
        p.setColor(0xFF666666);
        canvas.drawText("TAP 'LOAD IMAGE' TO BEGIN", w/2f, 250f, p);
        
        p.setColor(0xFF1565C0);
        canvas.drawText("OFFLINE HINDI + ENGLISH OCR", w/2f, 480f, p);
        
        p.setColor(0xFF2E7D32);
        p.setTextSize(38f);
        canvas.drawText("स्वागतम भारत", w/2f, 580f, p);
        
        p.setColor(0xFF37474F);
        p.setTextSize(32f);
        canvas.drawText("TAP ANY BOX TO SELECT AND EDIT IT", w/2f, 750f, p);

        this.bitmap = bmp;
        post(new Runnable() {
            @Override
            public void run() {
                updateImageBounds();
                performMockOCR(bitmap);
            }
        });
    }

    public void setListener(EditorListener listener) {
        this.listener = listener;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public ArrayList<TextBlock> getTextBlocks() {
        return textBlocks;
    }

    public TextBlock getSelectedBlock() {
        return selectedBlock;
    }

    public void setSelectedBlock(TextBlock block) {
        this.selectedBlock = block;
        invalidate();
        if (listener != null) {
            if (block != null) {
                listener.onBlockSelected(block);
            } else {
                listener.onBlockDeselected();
            }
        }
    }

    public void setActiveEditMode(int mode) {
        this.activeEditMode = mode;
        if (mode != EDIT_MODE_TEXT) {
            setSelectedBlock(null);
        }
        if (mode != EDIT_MODE_OVERLAY) {
            selectedOverlay = null;
        }
        invalidate();
    }

    public int getActiveEditMode() {
        return activeEditMode;
    }

    public void setBrushSize(float size) {
        this.brushSize = size;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateImageBounds();
    }

    public void updateImageBounds() {
        if (bitmap == null) return;
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;
        
        float imgWidth = bitmap.getWidth();
        float imgHeight = bitmap.getHeight();
        
        float scaleX = viewWidth / imgWidth;
        float scaleY = viewHeight / imgHeight;
        scale = Math.min(scaleX, scaleY);
        
        dx = (viewWidth - imgWidth * scale) / 2.0f;
        dy = (viewHeight - imgHeight * scale) / 2.0f;
        
        imageBounds.set(dx, dy, dx + imgWidth * scale, dy + imgHeight * scale);
    }

    public RectF getBitmapRectInView(RectF bmpRect) {
        return new RectF(
            bmpRect.left * scale + dx,
            bmpRect.top * scale + dy,
            bmpRect.right * scale + dx,
            bmpRect.bottom * scale + dy
        );
    }

    private int checkHandleTouch(float touchX, float touchY) {
        if (selectedBlock == null) return -1;
        RectF viewRect = getBitmapRectInView(selectedBlock.rect);
        float hRadius = 24.0f;
        
        float[] px = {
            viewRect.left,
            (viewRect.left + viewRect.right)/2,
            viewRect.right,
            viewRect.left,
            viewRect.right,
            viewRect.left,
            (viewRect.left + viewRect.right)/2,
            viewRect.right
        };
        float[] py = {
            viewRect.top,
            viewRect.top,
            viewRect.top,
            (viewRect.top + viewRect.bottom)/2,
            (viewRect.top + viewRect.bottom)/2,
            viewRect.bottom,
            viewRect.bottom,
            viewRect.bottom
        };
        
        for (int i = 0; i < 8; i++) {
            float dist = (touchX - px[i])*(touchX - px[i]) + (touchY - py[i])*(touchY - py[i]);
            if (dist < (hRadius + 20) * (hRadius + 20)) {
                return i;
            }
        }
        return -1;
    }

    private int checkOverlayHandleTouch(float touchX, float touchY) {
        if (selectedOverlay == null) return -1;
        RectF viewRect = getBitmapRectInView(selectedOverlay.rect);
        float hRadius = 24.0f;
        
        // 4 Corner handles for overlays
        float[] px = { viewRect.left, viewRect.right, viewRect.left, viewRect.right };
        float[] py = { viewRect.top, viewRect.top, viewRect.bottom, viewRect.bottom };
        
        for (int i = 0; i < 4; i++) {
            float dist = (touchX - px[i])*(touchX - px[i]) + (touchY - py[i])*(touchY - py[i]);
            if (dist < (hRadius + 20) * (hRadius + 20)) {
                return i;
            }
        }
        return -1;
    }

    private TextBlock findBlockAt(float touchX, float touchY) {
        for (int i = textBlocks.size() - 1; i >= 0; i--) {
            TextBlock tb = textBlocks.get(i);
            RectF viewRect = getBitmapRectInView(tb.rect);
            if (viewRect.contains(touchX, touchY)) {
                return tb;
            }
        }
        return null;
    }

    private ImageOverlay findOverlayAt(float touchX, float touchY) {
        for (int i = overlays.size() - 1; i >= 0; i--) {
            ImageOverlay ov = overlays.get(i);
            RectF viewRect = getBitmapRectInView(ov.rect);
            if (viewRect.contains(touchX, touchY)) {
                return ov;
            }
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null) return false;
        
        float touchX = event.getX();
        float touchY = event.getY();
        
        float touchBmpX = (touchX - dx) / scale;
        float touchBmpY = (touchY - dy) / scale;
        
        // Keep brush and drags tightly inside image boundaries
        touchBmpX = Math.max(0, Math.min(bitmap.getWidth(), touchBmpX));
        touchBmpY = Math.max(0, Math.min(bitmap.getHeight(), touchBmpY));
        
        int action = event.getAction();
        
        // Mode 1: BRUSH MODE
        if (activeEditMode == EDIT_MODE_BRUSH) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    currentStroke = new BrushStroke(brushSize);
                    undoneStrokes.clear();
                    brushStrokes.add(currentStroke);
                    addBrushPoint(touchBmpX, touchBmpY);
                    showCursor = true;
                    cursorX = touchX;
                    cursorY = touchY;
                    invalidate();
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    if (currentStroke != null) {
                        addBrushPoint(touchBmpX, touchBmpY);
                    }
                    cursorX = touchX;
                    cursorY = touchY;
                    invalidate();
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (currentStroke != null) {
                        addBrushPoint(touchBmpX, touchBmpY);
                    }
                    currentStroke = null;
                    showCursor = false;
                    invalidate();
                    return true;
            }
            return false;
        }
        
        // Mode 2: OVERLAY MODE
        if (activeEditMode == EDIT_MODE_OVERLAY) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    if (touchMode == TOUCH_MODE_DRAG_DROP_NEW) {
                        // Place dropped overlay
                        if (selectedOverlay != null) {
                            float w = selectedOverlay.rect.width();
                            float h = selectedOverlay.rect.height();
                            selectedOverlay.rect.set(
                                touchBmpX - w/2f,
                                touchBmpY - h/2f,
                                touchBmpX + w/2f,
                                touchBmpY + h/2f
                            );
                        }
                        touchMode = TOUCH_MODE_DRAG_OVERLAY;
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        invalidate();
                        return true;
                    }
                    
                    int handleIndex = checkOverlayHandleTouch(touchX, touchY);
                    if (handleIndex != -1) {
                        touchMode = TOUCH_MODE_RESIZE_OVERLAY;
                        activeHandle = handleIndex;
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        return true;
                    }
                    
                    ImageOverlay clickedOverlay = findOverlayAt(touchX, touchY);
                    if (clickedOverlay != null) {
                        selectedOverlay = clickedOverlay;
                        touchMode = TOUCH_MODE_DRAG_OVERLAY;
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        invalidate();
                        return true;
                    }
                    
                    selectedOverlay = null;
                    touchMode = TOUCH_MODE_IDLE;
                    invalidate();
                    return true;
                }
                
                case MotionEvent.ACTION_MOVE: {
                    if (selectedOverlay == null) return false;
                    
                    if (touchMode == TOUCH_MODE_DRAG_OVERLAY || touchMode == TOUCH_MODE_DRAG_DROP_NEW) {
                        float dX = touchBmpX - lastTouchBmpX;
                        float dY = touchBmpY - lastTouchBmpY;
                        
                        RectF r = selectedOverlay.rect;
                        r.offset(dX, dY);
                        
                        if (r.left < 0) r.offset(-r.left, 0);
                        if (r.top < 0) r.offset(0, -r.top);
                        if (r.right > bitmap.getWidth()) r.offset(bitmap.getWidth() - r.right, 0);
                        if (r.bottom > bitmap.getHeight()) r.offset(0, bitmap.getHeight() - r.bottom);
                        
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        invalidate();
                        return true;
                    } else if (touchMode == TOUCH_MODE_RESIZE_OVERLAY) {
                        resizeOverlayRect(selectedOverlay, activeHandle, touchBmpX, touchBmpY);
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        invalidate();
                        return true;
                    }
                    break;
                }
                
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchMode = TOUCH_MODE_IDLE;
                    activeHandle = -1;
                    return true;
            }
            return false;
        }

        // Mode 0: TEXT MODE (Preserve full functionality)
        if (activeEditMode == EDIT_MODE_TEXT) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    int handleIndex = checkHandleTouch(touchX, touchY);
                    if (handleIndex != -1) {
                        touchMode = TOUCH_MODE_RESIZE_BLOCK;
                        activeHandle = handleIndex;
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        return true;
                    }
                    
                    TextBlock clickedBlock = findBlockAt(touchX, touchY);
                    if (clickedBlock != null) {
                        setSelectedBlock(clickedBlock);
                        touchMode = TOUCH_MODE_DRAG_BLOCK;
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        return true;
                    }
                    
                    setSelectedBlock(null);
                    touchMode = TOUCH_MODE_IDLE;
                    return true;
                }
                
                case MotionEvent.ACTION_MOVE: {
                    if (selectedBlock == null) return false;
                    
                    if (touchMode == TOUCH_MODE_DRAG_BLOCK) {
                        float dX = touchBmpX - lastTouchBmpX;
                        float dY = touchBmpY - lastTouchBmpY;
                        
                        RectF r = selectedBlock.rect;
                        r.offset(dX, dY);
                        
                        if (r.left < 0) {
                            r.offset(-r.left, 0);
                        }
                        if (r.top < 0) {
                            r.offset(0, -r.top);
                        }
                        if (r.right > bitmap.getWidth()) {
                            r.offset(bitmap.getWidth() - r.right, 0);
                        }
                        if (r.bottom > bitmap.getHeight()) {
                            r.offset(0, bitmap.getHeight() - r.bottom);
                        }
                        
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        
                        analyzeStyle(selectedBlock, bitmap);
                        if (listener != null) {
                            listener.onBlockStyleUpdated(selectedBlock);
                        }
                        invalidate();
                        return true;
                    } else if (touchMode == TOUCH_MODE_RESIZE_BLOCK) {
                        RectF r = selectedBlock.rect;
                        float minSize = 15f;
                        
                        switch (activeHandle) {
                            case 0: // TL
                                r.left = Math.min(touchBmpX, r.right - minSize);
                                r.top = Math.min(touchBmpY, r.bottom - minSize);
                                break;
                            case 1: // TC
                                r.top = Math.min(touchBmpY, r.bottom - minSize);
                                break;
                            case 2: // TR
                                r.right = Math.max(touchBmpX, r.left + minSize);
                                r.top = Math.min(touchBmpY, r.bottom - minSize);
                                break;
                            case 3: // ML
                                r.left = Math.min(touchBmpX, r.right - minSize);
                                break;
                            case 4: // MR
                                r.right = Math.max(touchBmpX, r.left + minSize);
                                break;
                            case 5: // BL
                                r.left = Math.min(touchBmpX, r.right - minSize);
                                r.bottom = Math.max(touchBmpY, r.top + minSize);
                                break;
                            case 6: // BC
                                r.bottom = Math.max(touchBmpY, r.top + minSize);
                                break;
                            case 7: // BR
                                r.right = Math.max(touchBmpX, r.left + minSize);
                                r.bottom = Math.max(touchBmpY, r.top + minSize);
                                break;
                        }
                        
                        r.left = Math.max(0, r.left);
                        r.top = Math.max(0, r.top);
                        r.right = Math.min(bitmap.getWidth(), r.right);
                        r.bottom = Math.min(bitmap.getHeight(), r.bottom);
                        
                        lastTouchBmpX = touchBmpX;
                        lastTouchBmpY = touchBmpY;
                        
                        analyzeStyle(selectedBlock, bitmap);
                        if (listener != null) {
                            listener.onBlockStyleUpdated(selectedBlock);
                        }
                        invalidate();
                        return true;
                    }
                    break;
                }
                
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchMode = TOUCH_MODE_IDLE;
                    activeHandle = -1;
                    return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void addBrushPoint(float bmpX, float bmpY) {
        if (currentStroke == null) return;
        
        if (currentStroke.points.isEmpty()) {
            int col = sampleSurroundingColor(bitmap, bmpX, bmpY, currentStroke.radius);
            currentStroke.points.add(new PointF(bmpX, bmpY));
            currentStroke.colors.add(col);
        } else {
            PointF last = currentStroke.points.get(currentStroke.points.size() - 1);
            float dx = bmpX - last.x;
            float dy = bmpY - last.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            
            // Interpolate points for continuous clean brush sweeps
            float step = Math.max(2.0f, currentStroke.radius / 3.0f);
            if (dist > step) {
                int numSteps = (int) (dist / step);
                for (int i = 1; i <= numSteps; i++) {
                    float t = (float) i / numSteps;
                    float ix = last.x + dx * t;
                    float iy = last.y + dy * t;
                    int col = sampleSurroundingColor(bitmap, ix, iy, currentStroke.radius);
                    currentStroke.points.add(new PointF(ix, iy));
                    currentStroke.colors.add(col);
                }
            } else {
                int col = sampleSurroundingColor(bitmap, bmpX, bmpY, currentStroke.radius);
                currentStroke.points.add(new PointF(bmpX, bmpY));
                currentStroke.colors.add(col);
            }
        }
    }

    private int sampleSurroundingColor(Bitmap bmp, float cx, float cy, float radius) {
        if (bmp == null) return 0xFFFFFFFF;
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        
        long rSum = 0, gSum = 0, bSum = 0;
        int count = 0;
        
        // Sample surrounding ring of pixels at distance = radius + 6 pixels
        float sampleRadius = radius + 6.0f;
        int numSamples = 12;
        for (int i = 0; i < numSamples; i++) {
            double angle = i * (2.0 * Math.PI / numSamples);
            int sx = (int) (cx + sampleRadius * Math.cos(angle));
            int sy = (int) (cy + sampleRadius * Math.sin(angle));
            
            if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
                int pixel = bmp.getPixel(sx, sy);
                rSum += (pixel >> 16) & 0xFF;
                gSum += (pixel >> 8) & 0xFF;
                bSum += pixel & 0xFF;
                count++;
            }
        }
        
        if (count > 0) {
            return 0xFF000000 | ((int)(rSum / count) << 16) | ((int)(gSum / count) << 8) | ((int)(bSum / count));
        }
        return 0xFFFFFFFF;
    }

    private void resizeOverlayRect(ImageOverlay ov, int handle, float touchBmpX, float touchBmpY) {
        RectF r = ov.rect;
        float minSize = 25f;
        switch (handle) {
            case 0: // TL
                r.left = Math.min(touchBmpX, r.right - minSize);
                r.top = Math.min(touchBmpY, r.bottom - minSize);
                break;
            case 1: // TR
                r.right = Math.max(touchBmpX, r.left + minSize);
                r.top = Math.min(touchBmpY, r.bottom - minSize);
                break;
            case 2: // BL
                r.left = Math.min(touchBmpX, r.right - minSize);
                r.bottom = Math.max(touchBmpY, r.top + minSize);
                break;
            case 3: // BR
                r.right = Math.max(touchBmpX, r.left + minSize);
                r.bottom = Math.max(touchBmpY, r.top + minSize);
                break;
        }
        
        // Limit coordinates to screen size limits
        r.left = Math.max(0, r.left);
        r.top = Math.max(0, r.top);
        r.right = Math.min(bitmap.getWidth(), r.right);
        r.bottom = Math.min(bitmap.getHeight(), r.bottom);
    }

    // Brush Operations
    public void undoBrush() {
        if (!brushStrokes.isEmpty()) {
            BrushStroke stroke = brushStrokes.remove(brushStrokes.size() - 1);
            undoneStrokes.add(stroke);
            invalidate();
        }
    }

    public void redoBrush() {
        if (!undoneStrokes.isEmpty()) {
            BrushStroke stroke = undoneStrokes.remove(undoneStrokes.size() - 1);
            brushStrokes.add(stroke);
            invalidate();
        }
    }

    public void resetBrush() {
        brushStrokes.clear();
        undoneStrokes.clear();
        invalidate();
    }

    // Overlay Operations
    public void startDragDropNewOverlay(Bitmap overlayBmp) {
        activeEditMode = EDIT_MODE_OVERLAY;
        
        // Initialize with default size (~1/3 of screen size) maintaining target aspect ratios
        float aspect = (float) overlayBmp.getWidth() / overlayBmp.getHeight();
        float targetW = bitmap.getWidth() * 0.35f;
        float targetH = targetW / aspect;
        
        float left = (bitmap.getWidth() - targetW) / 2.0f;
        float top = (bitmap.getHeight() - targetH) / 2.0f;
        
        ImageOverlay newOverlay = new ImageOverlay(overlayBmp, new RectF(left, top, left + targetW, top + targetH));
        overlays.add(newOverlay);
        selectedOverlay = newOverlay;
        
        // Set immediately to DRAG_DROP state
        touchMode = TOUCH_MODE_DRAG_DROP_NEW;
        invalidate();
    }

    public void deleteSelectedOverlay() {
        if (selectedOverlay != null) {
            overlays.remove(selectedOverlay);
            selectedOverlay = null;
            invalidate();
        }
    }

    public void analyzeStyle(TextBlock block, Bitmap bmp) {
        if (bmp == null || block == null) return;
        
        int left = Math.max(0, (int) block.rect.left);
        int top = Math.max(0, (int) block.rect.top);
        int right = Math.min(bmp.getWidth(), (int) block.rect.right);
        int bottom = Math.min(bmp.getHeight(), (int) block.rect.bottom);
        
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) return;
        
        int stepX = Math.max(1, width / 12);
        int stepY = Math.max(1, height / 12);
        
        long rBg = 0, gBg = 0, bBg = 0;
        int bgCount = 0;
        
        for (int x = left; x < right; x += stepX) {
            int c1 = bmp.getPixel(x, top);
            int c2 = bmp.getPixel(x, Math.max(top, bottom - 1));
            rBg += (c1 >> 16) & 0xFF; gBg += (c1 >> 8) & 0xFF; bBg += c1 & 0xFF;
            rBg += (c2 >> 16) & 0xFF; gBg += (c2 >> 8) & 0xFF; bBg += c2 & 0xFF;
            bgCount += 2;
        }
        for (int y = top; y < bottom; y += stepY) {
            int c1 = bmp.getPixel(left, y);
            int c2 = bmp.getPixel(Math.max(left, right - 1), y);
            rBg += (c1 >> 16) & 0xFF; gBg += (c1 >> 8) & 0xFF; bBg += c1 & 0xFF;
            rBg += (c2 >> 16) & 0xFF; gBg += (c2 >> 8) & 0xFF; bBg += c2 & 0xFF;
            bgCount += 2;
        }
        
        int bgColor = 0xFFFFFFFF;
        if (bgCount > 0) {
            bgColor = 0xFF000000 | ((int)(rBg / bgCount) << 16) | ((int)(gBg / bgCount) << 8) | ((int)(bBg / bgCount));
        }
        
        int bgR = (bgColor >> 16) & 0xFF;
        int bgG = (bgColor >> 8) & 0xFF;
        int bgB = bgColor & 0xFF;
        
        int maxDist = -1;
        int textColor = 0xFF000000;
        
        for (int y = top; y < bottom; y += stepY) {
            for (int x = left; x < right; x += stepX) {
                int pixel = bmp.getPixel(x, y);
                int pr = (pixel >> 16) & 0xFF;
                int pg = (pixel >> 8) & 0xFF;
                int pb = pixel & 0xFF;
                
                int dist = Math.abs(pr - bgR) + Math.abs(pg - bgG) + Math.abs(pb - bgB);
                if (dist > maxDist) {
                    maxDist = dist;
                    textColor = pixel;
                }
            }
        }
        
        block.backgroundColor = bgColor;
        block.textColor = textColor;
        block.textSize = (float) (height * 0.55);
    }

    public void addCustomBlock() {
        if (bitmap == null) return;
        
        float bW = bitmap.getWidth() * 0.5f;
        float bH = bitmap.getHeight() * 0.08f;
        float left = (bitmap.getWidth() - bW) / 2f;
        float top = (bitmap.getHeight() - bH) / 2f;
        
        TextBlock tb = new TextBlock(new RectF(left, top, left + bW, top + bH), "ADD CUSTOM TEXT");
        analyzeStyle(tb, bitmap);
        textBlocks.add(tb);
        setSelectedBlock(tb);
        invalidate();
    }

    public void deleteSelectedBlock() {
        if (selectedBlock != null) {
            textBlocks.remove(selectedBlock);
            setSelectedBlock(null);
            invalidate();
        }
    }

    public void performMockOCR(Bitmap bmp) {
        textBlocks.clear();
        if (bmp == null) return;
        
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        
        int stripCount = 12;
        int stripHeight = h / stripCount;
        int detectedCount = 0;
        
        for (int i = 1; i < stripCount - 1; i++) {
            int yStart = i * stripHeight;
            int yEnd = yStart + stripHeight;
            
            int highContrastSegments = 0;
            int minX = w, maxX = 0;
            
            int step = Math.max(15, w / 40);
            for (int x = 20; x < w - 20; x += step) {
                int p1 = bmp.getPixel(x, yStart + stripHeight / 2);
                int p2 = bmp.getPixel(Math.min(w-1, x + 4), yStart + stripHeight / 2);
                
                int r1 = (p1 >> 16) & 0xFF, g1 = (p1 >> 8) & 0xFF, b1 = p1 & 0xFF;
                int r2 = (p2 >> 16) & 0xFF, g2 = (p2 >> 8) & 0xFF, b2 = p2 & 0xFF;
                
                int diff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
                if (diff > 80) {
                    highContrastSegments++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }
            
            if (highContrastSegments > 3 && (maxX - minX) > w * 0.12f) {
                float padding = 20f;
                float left = Math.max(0, minX - padding);
                float top = Math.max(0, yStart + padding);
                float right = Math.min(w, maxX + padding);
                float bottom = Math.min(h, yEnd - padding);
                
                String mockText;
                if (detectedCount == 0) mockText = "IMAGE TEXT";
                else if (detectedCount == 1) mockText = "REPLACER";
                else if (detectedCount == 2) mockText = "नमस्ते";
                else if (detectedCount == 3) mockText = "OFFLINE OCR";
                else mockText = "TEXT BLOCK " + (detectedCount + 1);
                
                TextBlock tb = new TextBlock(new RectF(left, top, right, bottom), mockText);
                analyzeStyle(tb, bmp);
                textBlocks.add(tb);
                detectedCount++;
                if (detectedCount >= 7) break;
            }
        }
        
        if (textBlocks.isEmpty()) {
            float bW = w * 0.7f;
            float bH = h * 0.08f;
            
            float left1 = (w - bW)/2;
            float top1 = h * 0.35f;
            TextBlock tb1 = new TextBlock(new RectF(left1, top1, left1 + bW, top1 + bH), "Tap To Select Me");
            analyzeStyle(tb1, bmp);
            textBlocks.add(tb1);
            
            float left2 = (w - bW)/2;
            float top2 = h * 0.50f;
            TextBlock tb2 = new TextBlock(new RectF(left2, top2, left2 + bW, top2 + bH), "हिंदी पाठ प्रतिस्थापन");
            analyzeStyle(tb2, bmp);
            textBlocks.add(tb2);
            
            float left3 = (w - bW)/2;
            float top3 = h * 0.65f;
            TextBlock tb3 = new TextBlock(new RectF(left3, top3, left3 + bW, top3 + bH), "REPLACE WITH HINDI");
            analyzeStyle(tb3, bmp);
            textBlocks.add(tb3);
        }
        
        setSelectedBlock(null);
        invalidate();
    }

    public void setImageUri(android.net.Uri uri) {
        try {
            java.io.InputStream is = getContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = false;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();
            
            if (bmp != null) {
                int maxDim = 2048;
                if (bmp.getWidth() > maxDim || bmp.getHeight() > maxDim) {
                    float s = Math.min((float)maxDim/bmp.getWidth(), (float)maxDim/bmp.getHeight());
                    bmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()*s), (int)(bmp.getHeight()*s), true);
                }
                this.bitmap = bmp;
                setSelectedBlock(null);
                overlays.clear();
                brushStrokes.clear();
                undoneStrokes.clear();
                updateImageBounds();
                performMockOCR(bmp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;
        
        // Layer 1: Draw base mutable/original image
        canvas.drawBitmap(bitmap, null, imageBounds, null);
        
        // Layer 2: Draw Brushed background reconstruction edits
        Paint brushPaint = new Paint();
        brushPaint.setAntiAlias(true);
        brushPaint.setStyle(Paint.Style.FILL);
        for (BrushStroke stroke : brushStrokes) {
            for (int i = 0; i < stroke.points.size(); i++) {
                PointF p = stroke.points.get(i);
                int color = stroke.colors.get(i);
                brushPaint.setColor(color);
                
                // Map point from original bitmap pixels to active view coordinates
                float vx = p.x * scale + dx;
                float vy = p.y * scale + dy;
                canvas.drawCircle(vx, vy, stroke.radius * scale, brushPaint);
            }
        }
        
        // Layer 3: Draw Text Replacement Patches (if replaced)
        for (TextBlock b : textBlocks) {
            if (b.isReplaced) {
                drawTextBlock(canvas, b, false);
            }
        }
        
        // Layer 4: Draw image overlays
        for (ImageOverlay ov : overlays) {
            RectF viewRect = getBitmapRectInView(ov.rect);
            canvas.drawBitmap(ov.bitmap, null, viewRect, null);
        }
        
        // UI Helpers Only: Selected Text Bounding boxes
        if (activeEditMode == EDIT_MODE_TEXT && selectedBlock != null) {
            RectF viewRect = getBitmapRectInView(selectedBlock.rect);
            
            Paint borderPaint = new Paint();
            borderPaint.setColor(0xFF007AFF);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(4.0f);
            canvas.drawRect(viewRect, borderPaint);
            
            Paint handlePaint = new Paint();
            handlePaint.setColor(0xFF007AFF);
            handlePaint.setStyle(Paint.Style.FILL);
            
            Paint handleStroke = new Paint();
            handleStroke.setColor(0xFFFFFFFF);
            handleStroke.setStyle(Paint.Style.STROKE);
            handleStroke.setStrokeWidth(3.0f);
            
            float hRadius = 16.0f;
            
            float[] px = {
                viewRect.left,
                (viewRect.left + viewRect.right)/2,
                viewRect.right,
                viewRect.left,
                viewRect.right,
                viewRect.left,
                (viewRect.left + viewRect.right)/2,
                viewRect.right
            };
            float[] py = {
                viewRect.top,
                viewRect.top,
                viewRect.top,
                (viewRect.top + viewRect.bottom)/2,
                (viewRect.top + viewRect.bottom)/2,
                viewRect.bottom,
                viewRect.bottom,
                viewRect.bottom
            };
            
            for (int i = 0; i < 8; i++) {
                canvas.drawCircle(px[i], py[i], hRadius, handlePaint);
                canvas.drawCircle(px[i], py[i], hRadius, handleStroke);
            }
        }
        
        // UI Helpers Only: Selected Overlay borders & resize corner handles
        if (activeEditMode == EDIT_MODE_OVERLAY && selectedOverlay != null) {
            RectF viewRect = getBitmapRectInView(selectedOverlay.rect);
            
            Paint borderPaint = new Paint();
            borderPaint.setColor(0xFFFF5722); // Orange overlay border
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(4.0f);
            canvas.drawRect(viewRect, borderPaint);
            
            Paint handlePaint = new Paint();
            handlePaint.setColor(0xFFFF5722);
            handlePaint.setStyle(Paint.Style.FILL);
            
            Paint handleStroke = new Paint();
            handleStroke.setColor(0xFFFFFFFF);
            handleStroke.setStyle(Paint.Style.STROKE);
            handleStroke.setStrokeWidth(3.0f);
            
            float hRadius = 16.0f;
            float[] px = { viewRect.left, viewRect.right, viewRect.left, viewRect.right };
            float[] py = { viewRect.top, viewRect.top, viewRect.bottom, viewRect.bottom };
            
            for (int i = 0; i < 4; i++) {
                canvas.drawCircle(px[i], py[i], hRadius, handlePaint);
                canvas.drawCircle(px[i], py[i], hRadius, handleStroke);
            }
        }
        
        // UI Helpers Only: Circular brush cursor (In Brush mode)
        if (activeEditMode == EDIT_MODE_BRUSH && showCursor && cursorX >= 0) {
            Paint cursorPaint = new Paint();
            cursorPaint.setStyle(Paint.Style.STROKE);
            cursorPaint.setColor(0xCC007AFF);
            cursorPaint.setStrokeWidth(3.0f);
            canvas.drawCircle(cursorX, cursorY, brushSize * scale, cursorPaint);
        }
    }

    private void drawTextBlock(Canvas canvas, TextBlock b, boolean isForExport) {
        RectF drawRect;
        float scaleFactor;
        if (isForExport) {
            drawRect = b.rect;
            scaleFactor = 1.0f;
        } else {
            drawRect = getBitmapRectInView(b.rect);
            scaleFactor = scale;
        }
        
        Paint patchPaint = new Paint();
        patchPaint.setColor(b.backgroundColor);
        patchPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(drawRect, patchPaint);
        
        if (b.isReplaced && b.replacementText != null && !b.replacementText.trim().isEmpty()) {
            Paint textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(b.textColor);
            textPaint.setTextSize(b.textSize * scaleFactor);
            textPaint.setFakeBoldText(b.isBold);
            
            if ("LEFT".equals(b.alignment)) {
                textPaint.setTextAlign(Paint.Align.LEFT);
            } else if ("RIGHT".equals(b.alignment)) {
                textPaint.setTextAlign(Paint.Align.RIGHT);
            } else {
                textPaint.setTextAlign(Paint.Align.CENTER);
            }
            
            float x;
            if ("LEFT".equals(b.alignment)) {
                x = drawRect.left + 5 * scaleFactor;
            } else if ("RIGHT".equals(b.alignment)) {
                x = drawRect.right - 5 * scaleFactor;
            } else {
                x = drawRect.centerX();
            }
            
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float y = drawRect.centerY() - (fm.descent + fm.ascent) / 2;
            
            canvas.drawText(b.replacementText, x, y, textPaint);
        }
    }

    public Bitmap generateFinalBitmap() {
        if (bitmap == null) return null;
        
        // Keep original image immutable; composite layers onto output copy
        Bitmap exportBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(exportBmp);
        
        // 1. Draw Brush/Cleaning Edits
        Paint brushPaint = new Paint();
        brushPaint.setAntiAlias(true);
        brushPaint.setStyle(Paint.Style.FILL);
        for (BrushStroke stroke : brushStrokes) {
            for (int i = 0; i < stroke.points.size(); i++) {
                PointF p = stroke.points.get(i);
                int color = stroke.colors.get(i);
                brushPaint.setColor(color);
                canvas.drawCircle(p.x, p.y, stroke.radius, brushPaint);
            }
        }
        
        // 2. Draw Text replacement patches
        for (TextBlock b : textBlocks) {
            if (b.isReplaced) {
                drawTextBlock(canvas, b, true);
            }
        }
        
        // 3. Draw Overlaid images
        for (ImageOverlay ov : overlays) {
            canvas.drawBitmap(ov.bitmap, null, ov.rect, null);
        }
        
        return exportBmp;
    }
}