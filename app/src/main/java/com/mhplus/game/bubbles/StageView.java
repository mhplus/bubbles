package com.mhplus.game.bubbles;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import java.util.ArrayList;

public class StageView extends View {
    private static final String TAG = "CoverFlowView";

    private VelocityTracker mTracker;
    private Bubble mBubble;
    private ArrayList<StaticDrawable> mStaticDrawables = new ArrayList<>();
    private int mMaximumVelocity;
    private int mActivePointerId;

    private OverScroller mScroller;
    private static class OvershootInterpolator implements Interpolator {
        private static final float DEFAULT_TENSION = 1.0f;
        private float mTension;

        OvershootInterpolator() {
            mTension = DEFAULT_TENSION;
        }

        public float getInterpolation(float t) {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * ((mTension + 1) * t + mTension) + 1.0f;
        }
    }

    public StageView(Context context) {
        this(context, null, 0);
    }

    public StageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //setHapticFeedbackEnabled(false);
        //Handler mHandler = new Handler();
        mScroller = new OverScroller(context, new OvershootInterpolator());

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mMaximumVelocity = conf.getScaledMaximumFlingVelocity();
        mTracker = VelocityTracker.obtain();
        int borderColor = getResources().getColor(R.color.colorAccent, null);
        mStaticDrawables.add(new StaticDrawable(
                0, 2000, 1440, 10, borderColor));
        mStaticDrawables.add(new StaticDrawable(
                0, 0, 1440, 10, borderColor));
        mStaticDrawables.add(new StaticDrawable(
                0, 0, 10, 2000, borderColor));
        mStaticDrawables.add(new StaticDrawable(
                1430, 0, 10, 2000, borderColor));

        mBubble = new Bubble(context, 720, 2200, 100, R.drawable.bubble);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow::");
    }

    @Override
    public void computeScroll() {
        mScroller.computeScrollOffset();
        //int x = mScroller.getCurrX();
        //int y = mScroller.getCurrY();
        //Log.i(TAG, "computeScroll scroll X=" + x + ", y=" + y);
        //postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.translate(mScrollX, 0);
        for (StaticDrawable d: mStaticDrawables) {
            d.draw(canvas);
        }
        mBubble.draw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "onMeasure width=" + width +", height" + height);
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        int x = (int) ev.getX();
        int y = (int) ev.getY();
        mTracker.addMovement(ev);
        Log.d(TAG, "onTouchEvent x=" + x + ", y=" + y);
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mBubble.moveTo(x, y);
            mActivePointerId = ev.getPointerId(0);
            invalidate();
            return true;
        case MotionEvent.ACTION_MOVE:
            mBubble.moveTo(x, y);
            invalidate();
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            mTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            int velocityX = (int) mTracker.getXVelocity(mActivePointerId);
            int velocityY = (int) mTracker.getYVelocity(mActivePointerId);
            Log.i(TAG, "ACTION_UP velocityX=" + velocityX + ", velocityY=" + velocityY);
            mScroller.fling(x, y,
                    velocityX, velocityY,
                    0, 1440,
                    0, 2200,
                    0, 0);
            performClick();
            return true;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean performClick() {
        Log.d(TAG, "Clicked");
        return super.performClick();
    }
}
