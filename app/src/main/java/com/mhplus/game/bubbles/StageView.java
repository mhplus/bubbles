package com.mhplus.game.bubbles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class StageView extends View {
    private static final String TAG = "CoverFlowView";

    private Paint mHQPaint = new Paint();

    private Bubble mBubble;
    private BubbleLauncher mBubbleLauncher;
    private ArrayList<StaticDrawable> mStaticDrawables = new ArrayList<>();


    public StageView(Context context) {
        this(context, null, 0);
    }

    public StageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setHapticFeedbackEnabled(false);

        mHQPaint.setAntiAlias(true);
        mHQPaint.setFilterBitmap(true);
        int borderColor = getResources().getColor(R.color.colorAccent, null);
        mStaticDrawables.add(new StaticDrawable(
                0, 2000, 1440, 10, null, borderColor));
        mStaticDrawables.add(new StaticDrawable(
                0, 0, 1440, 10, null, borderColor));
        mStaticDrawables.add(new StaticDrawable(
                0, 0, 10, 2000, null, borderColor));
        mStaticDrawables.add(new StaticDrawable(
                1430, 0, 10, 2000, null, borderColor));

        mBubble = new Bubble(context, 720, 2200, 100, R.drawable.bubble);
        mBubbleLauncher = new BubbleLauncher();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow::");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.translate(mScrollX, 0);
        for (StaticDrawable d: mStaticDrawables) {
            d.draw(canvas);
        }
        mBubbleLauncher.draw(canvas);
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
        Log.d(TAG, "onTouchEvent x=" + x + ", y=" + y);
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mBubble.moveTo(x, y);
            invalidate();
            return true;
        case MotionEvent.ACTION_MOVE:
            mBubble.moveTo(x, y);
            invalidate();
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
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
