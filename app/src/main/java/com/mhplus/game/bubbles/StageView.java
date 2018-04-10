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

    /*
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
    */

    public StageView(Context context) {
        this(context, null, 0);
    }

    public StageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        //setHapticFeedbackEnabled(false);
        mHandler = new AnimationHandler();
        //mScroller = new OverScroller(context, new OvershootInterpolator());

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mMaximumVelocity = conf.getScaledMaximumFlingVelocity();
        mTracker = VelocityTracker.obtain();
        int borderColor = getResources().getColor(R.color.colorAccent, null);
        mBorders.add(new Border(
                0, 2000, 1440, 10, borderColor)); // bottom
        mBorders.add(new Border(
                0, 0, 1440, 10, borderColor)); //top
        mBorders.add(new Border(
                0, 0, 10, 2000, borderColor)); //left
        mBorders.add(new Border(
                1430, 0, 10, 2000, borderColor)); // right

        createBubble();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow::");
    }

    @Override
    public void computeScroll() {
        //mScroller.computeScrollOffset();
        //int x = mScroller.getCurrX();
        //int y = mScroller.getCurrY();
        //Log.i(TAG, "computeScroll scroll X=" + x + ", y=" + y);
        //postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.translate(mScrollX, 0);
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
            case INVALIDATED:
                mBubble.updateState();
                if (!mBubble.isMoving()) {
                    sendEmptyMessageDelayed(ANIMATION_FINISHED, 200);
                    break;
                }
                for (Border b: mBorders) {
                    if (b.isHit(mBubble)) {
                        mBubble.updateDirection(b);
                        break;
                    }
                }
                postInvalidate();
                sendEmptyMessageDelayed(INVALIDATED, 1000 / 60);
                break;
            case ANIMATION_FINISHED:
                mBubbles.add(mBubble);
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
        Log.d(TAG, "onTouchEvent x=" + x + ", y=" + y);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (y < 2000) {
                    mBubbles.clear();
                    mIsClearing = true;
                    if (mBubble == null) {
                        createBubble();
                    } else {
                        mBubble.moveTo(720, 2200);
                    }
                    invalidate();
                    return true;
                }
                mBubble.moveTo(x, y);
                mActivePointerId = ev.getPointerId(0);
                //mPosX = x;
                //mPosY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mIsClearing) {
                    return true;
                }
                if (mBubble != null) {
                    mBubble.moveTo(x, y);
                    //mPosX = x;
                    //mPosY = y;
                    invalidate();
                }
                return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mIsClearing) {
                mIsClearing = false;
                return true;
            }
            if (mBubble != null) {
                mTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) mTracker.getXVelocity(mActivePointerId);
                int velocityY = (int) mTracker.getYVelocity(mActivePointerId);
                Log.i(TAG, "ACTION_UP velocityX=" + velocityX + ", velocityY=" + velocityY);
                mBubble.run(velocityX, velocityY);
                performClick();
                mHandler.sendEmptyMessage(INVALIDATED);
            }
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
