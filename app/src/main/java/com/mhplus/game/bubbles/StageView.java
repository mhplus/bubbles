package com.mhplus.game.bubbles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

public class StageView extends View {
    private static final String TAG = "CoverFlowView";

    private boolean mIsClearing = false;
    private VelocityTracker mTracker;
    private Bubble mBubble;
    private ArrayList<Border> mBorders = new ArrayList<>();
    private ArrayList<Bubble> mBubbles = new ArrayList<>();
    private int mMaximumVelocity;
    private int mActivePointerId;

    private AnimationHandler mHandler;
    private Context mContext;

    public StageView(Context context) {
        this(context, null, 0);
    }

    public StageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mHandler = new AnimationHandler();

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mMaximumVelocity = conf.getScaledMaximumFlingVelocity();
        mTracker = VelocityTracker.obtain();
        int borderColor = getResources().getColor(R.color.colorAccent, null);
        mBorders.add(new Border(Border.BORDER_TYPE_BOTTOM,
                0, 2000, 1440, 10, borderColor)); // bottom
        mBorders.add(new Border(Border.BORDER_TYPE_TOP,
                0, 0, 1440, 10, borderColor)); //top
        mBorders.add(new Border(Border.BORDER_TYPE_LEFT,
                0, 0, 10, 2000, borderColor)); //left
        mBorders.add(new Border(Border.BORDER_TYPE_RIGHT,
                1430, 0, 10, 2000, borderColor)); // right

        createBubble();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(INVALIDATED);
        mHandler.removeMessages(ANIMATION_FINISHED);
        Log.i(TAG, "onDetachedFromWindow::");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Border d: mBorders) {
            d.draw(canvas);
        }
        for (Bubble b: mBubbles) {
            b.draw(canvas);
        }
        if (mBubble != null) {
            mBubble.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "onMeasure width=" + width +", height" + height);
        setMeasuredDimension(width, height);
    }

    private static final int INVALIDATED = 0;
    private static final int ANIMATION_FINISHED = 1;
    @SuppressLint("HandlerLeak")
    private class AnimationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case INVALIDATED: {
                mBubble.updateState();
                if (!mBubble.isMoving()) {
                    sendEmptyMessageDelayed(ANIMATION_FINISHED, 200);
                    break;
                }
                for (Border b : mBorders) {
                    if (b.isHit(mBubble)) {
                        mBubble.updateDirection(b);
                        break;
                    }
                }
                postInvalidate();
                sendEmptyMessageDelayed(INVALIDATED, 1000 / 60);
                break;
            }
            case ANIMATION_FINISHED:
                int r = mBubble.getRadius();
                if (mBubble.getPositionY() + r < 2000) {
                    mBubbles.add(mBubble);
                }
                createBubble();
                postInvalidate();
            default:
                break;
            }
        }
    }

    void createBubble() {
        mBubble = new Bubble(mContext, 720, 2200, 100, R.drawable.bubble);
        mBubble.moveTo(720, 2200);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        int x = (int) ev.getX();
        int y = (int) ev.getY();
        mTracker.addMovement(ev);
//        Log.d(TAG, "onTouchEvent x=" + x + ", y=" + y);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                if (y < 2000) {
                    mBubbles.clear();
                    mIsClearing = true;
                } else {
                    createBubble();
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mIsClearing) {
                    return true;
                }
                mBubble.moveTo(x, y);
                invalidate();
                return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mIsClearing) {
                mIsClearing = false;
                return true;
            }
            mTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            int velocityX = (int) mTracker.getXVelocity(mActivePointerId);
            int velocityY = (int) mTracker.getYVelocity(mActivePointerId);
            Log.i(TAG, "ACTION_UP velocityX=" + velocityX + ", velocityY=" + velocityY);
            mBubble.run(velocityX, velocityY, System.currentTimeMillis());
            performClick();
            mHandler.sendEmptyMessage(INVALIDATED);
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
