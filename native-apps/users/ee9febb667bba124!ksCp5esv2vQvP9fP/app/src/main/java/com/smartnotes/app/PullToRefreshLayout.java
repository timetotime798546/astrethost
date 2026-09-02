package com.smartnotes.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.ListView;

public class PullToRefreshLayout extends LinearLayout {
    private static final float PULL_RESISTANCE = 2.0f;
    private int touchSlop;
    private float initialDownY;
    private boolean isBeingDragged = false;
    private boolean isRefreshing = false;
    private int triggerDistance;
    private int headerHeight;

    private View headerView;
    private BeautifulLoaderView loaderView;
    private ListView targetListView;
    private OnRefreshListener refreshListener;
    private int lastTickStep = 0;

    public interface OnRefreshListener {
        void onRefresh();
    }

    public PullToRefreshLayout(Context context) {
        super(context);
        init(context);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        triggerDistance = (int) (getResources().getDisplayMetrics().density * 80);
        headerHeight = (int) (getResources().getDisplayMetrics().density * 80);

        headerView = new LinearLayout(context);
        ((LinearLayout) headerView).setOrientation(HORIZONTAL);
        ((LinearLayout) headerView).setGravity(android.view.Gravity.CENTER);

        loaderView = new BeautifulLoaderView(context);
        LinearLayout.LayoutParams loaderParams = new LinearLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 50),
                (int) (getResources().getDisplayMetrics().density * 50)
        );
        headerView.addView(loaderView, loaderParams);

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, headerHeight
        );
        headerParams.topMargin = -headerHeight;
        addView(headerView, 0, headerParams);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (targetListView == null) {
            targetListView = findListView(this);
        }
    }

    private ListView findListView(View view) {
        if (view instanceof ListView) {
            return (ListView) view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ListView result = findListView(group.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    private boolean canChildScrollUp() {
        if (targetListView == null) return false;
        if (targetListView.getChildCount() == 0) return false;
        return targetListView.getFirstVisiblePosition() > 0
                || targetListView.getChildAt(0).getTop() < targetListView.getPaddingTop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isRefreshing) {
            return true;
        }
        if (canChildScrollUp()) {
            return false;
        }

        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialDownY = ev.getY();
                isBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                float dy = y - initialDownY;
                if (dy > touchSlop && !isBeingDragged) {
                    isBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isBeingDragged = false;
                break;
        }
        return isBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRefreshing) {
            return true;
        }

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                float dy = y - initialDownY;
                if (dy < 0) {
                    dy = 0;
                }

                float pullDist = dy / PULL_RESISTANCE;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) headerView.getLayoutParams();
                lp.topMargin = (int) (pullDist - headerHeight);
                if (lp.topMargin > triggerDistance) {
                    lp.topMargin = triggerDistance;
                }
                headerView.setLayoutParams(lp);

                float progress = Math.min(1.0f, pullDist / triggerDistance);
                loaderView.setPullProgress(progress);

                int currentStep = (int) (progress * 15);
                if (currentStep > lastTickStep && currentStep < 15) {
                    SoundManager.playPullTick();
                    lastTickStep = currentStep;
                } else if (currentStep < lastTickStep) {
                    lastTickStep = currentStep;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                LinearLayout.LayoutParams lpUp = (LinearLayout.LayoutParams) headerView.getLayoutParams();
                int currentPull = lpUp.topMargin + headerHeight;
                if (currentPull >= triggerDistance) {
                    startRefreshing();
                } else {
                    smoothScrollTo(-headerHeight);
                }
                isBeingDragged = false;
                lastTickStep = 0;
                break;
        }
        return true;
    }

    private void startRefreshing() {
        if (isRefreshing) return;
        isRefreshing = true;
        loaderView.setRefreshing(true);
        smoothScrollTo(0);
        SoundManager.playRefreshTrigger();
        if (refreshListener != null) {
            refreshListener.onRefresh();
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing == this.isRefreshing) return;
        this.isRefreshing = refreshing;
        if (!refreshing) {
            loaderView.setRefreshing(false);
            smoothScrollTo(-headerHeight);
            SoundManager.playRefreshComplete();
        } else {
            loaderView.setRefreshing(true);
            smoothScrollTo(0);
        }
    }

    private void smoothScrollTo(final int targetMargin) {
        final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) headerView.getLayoutParams();
        final int startMargin = lp.topMargin;
        final int delta = targetMargin - startMargin;
        final int duration = 200;
        final long startTime = System.currentTimeMillis();

        post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= duration) {
                    lp.topMargin = targetMargin;
                    headerView.setLayoutParams(lp);
                } else {
                    float t = (float) elapsed / duration;
                    t = t * (2 - t);
                    lp.topMargin = (int) (startMargin + delta * t);
                    headerView.setLayoutParams(lp);
                    post(this);
                }
            }
        });
    } 
}