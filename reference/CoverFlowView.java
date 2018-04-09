package com.windriver.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.lge.ivi.util.Log;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import com.lge.music.R;
import com.windriver.widget.Visualizer.Transformation;

public class CoverFlowView extends View implements Visualizer.Callback {
    private static final String TAG = "CoverFlowView";
    private static final int SCROLL_SIZE_PER_ITEM = 800;
    private static final int SNAP_MOVING_GAP = 100;
    private static final int SNAP_TOUCH_SLOP = 5;
    private static final int INVALID_POSITION = -1;
    private static final int FLING_ANIMATION_DURATION = 400;
    private static final int FLING_DISTANCE = 25;
    private boolean mFirstLayout = true;
    private boolean mFirstMove = true;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;

    private Paint  mPaint = new Paint();
    private Paint  mHQPaint = new Paint();

    private float mLastMotionX;
    private float mInitialMotionX;

    private int mTotalDeltaX;

    private int mCurrentPosition = INVALID_POSITION;
    private int mNextPosition = INVALID_POSITION;
    private int mPlayingPosition = INVALID_POSITION;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_DOWN = 1;
    private final static int TOUCH_STATE_SCROLLING = 2;
    private final static int TOUCH_STATE_FLING = 3;
    private int mTouchState = TOUCH_STATE_REST;

    public static final int THUMB_SMALL_SIZE = 0;
    public static final int THUMB_LARGE_SIZE = 1;

    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private static final int INVALID_POINTER = -1;

    private int mActivePointerId = INVALID_POINTER;
    private static final float NANOTIME_DIV = 1000000000.0f;
    private static final float SMOOTHING_SPEED = 0.75f;
    private static final float SMOOTHING_CONSTANT = (float) (0.016 / Math.log(SMOOTHING_SPEED));
    private float mSmoothingTime;
    private float mTouchX;
    private boolean mScrollEnabled;
    private int mThumbSize;
    private boolean mTouchEnabled = true;
    private boolean mFlingByScroll;
    private boolean mHighModel;

    private Drawable mFocusDrawable;
    private Drawable mShadowDrawable;
    private Drawable mShadowLeftDrawable;
    private Drawable mShadowRightDrawable;
    private Drawable mThumbBackground;
    private Rect mLeftShadowRect;
    private Rect mRightShadowRect;
    private int mLeftShadowVisibleX;
    private int mRightShadowVisibleX;

    private static final int SHADOW_WIDTH = 19;

    public static interface OnItemClickListener {
        public void onItemClick(CoverFlowView view, int position);
    }

    public static interface OnItemFocusChangeListener {
        public void onItemFocusChange(CoverFlowView view, int position, boolean byUser);
    }

    private OnItemClickListener mOnItemClickListener;
    private OnItemFocusChangeListener mOnItemFocusChangeListener;

    private OvershootInterpolator mScrollInterpolator;

    private static class OvershootInterpolator implements Interpolator {
        private static final float DEFAULT_TENSION = 1.0f;
        private float mTension;

        public OvershootInterpolator() {
            mTension = DEFAULT_TENSION;
        }

        public void setDistance(int distance) {
            mTension = distance > 0 ? DEFAULT_TENSION / distance : DEFAULT_TENSION;
        }

        public void disableSettle() {
            mTension = 0.f;
        }

        public float getInterpolation(float t) {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * ((mTension + 1) * t + mTension) + 1.0f;
        }
    }

    private Visualizer mVisualizer;
    private CoverFlowAdapter mAdapter;

    public CoverFlowView(Context context) {
        this(context, null, 0);
    }

    public CoverFlowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CoverFlowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setHapticFeedbackEnabled(false);
        mPaint.setAntiAlias(false);
        mPaint.setFilterBitmap(false);

        mHQPaint.setAntiAlias(true);
        mHQPaint.setFilterBitmap(true);
        init();
    }

    private void init() {
        Context context = getContext();
        mScrollInterpolator = new OvershootInterpolator();
        mScroller = new OverScroller(context, mScrollInterpolator);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mVisualizer = new Visualizer(this);
        mFocusDrawable = context.getResources().getDrawable(com.android.internal.R.drawable.skin_gen_focus);
        mHighModel = (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);		
    }

    public void setEffect(int res) {
        mVisualizer.setTransitionEffect(getContext(), res);
        if (res == R.xml.effect_shift_scale_small) {
            mThumbSize = THUMB_SMALL_SIZE;
        } else {
            mThumbSize = THUMB_LARGE_SIZE;
        }
        initShadowEffect();
    }

    public void setScrollEnabled(boolean enabled) {
        mScrollEnabled = enabled;
    }

    public void setAdapter(CoverFlowAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.release();
        }
        mAdapter = adapter;
        if (adapter != null) {
            mAdapter.setThumbnailSize(mThumbSize);
            mAdapter.setOnDataChangeListener(new CoverFlowAdapter.OnDataChangeListener() {
                public void onDataChanged(int position) {
                    invalidate();
                }
            });
        }
        mTouchState = TOUCH_STATE_REST;
        mCurrentPosition = INVALID_POSITION;
        mNextPosition = INVALID_POSITION;
        mPlayingPosition = INVALID_POSITION;

        mVisualizer.setChildCount(getItemCount());
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        invalidate();
        mHandler.removeMessages(MSG_TOUCH_IDLE);
        initShadowEffect();
        initFocusDrawable();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow::");
        if (mAdapter != null) {
            mAdapter.release();
            mAdapter = null;
        }
    }

    public CoverFlowAdapter getAdapter() {
        return mAdapter;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void setOnItemFocusChangeListener(OnItemFocusChangeListener l) {
        mOnItemFocusChangeListener = l;
    }

    public int getItemCount() {
        if (mAdapter != null){
            return mAdapter.getCount();
        }
        return 0;
    }

    public void setPosition(int position) {
        Log.i(TAG, "setPosition mCurrentPosition=" + mCurrentPosition + ", position=" + position);
        if (mCurrentPosition == position) {
            return;
        }
        position = Math.max(0, Math.min(position, getItemCount() - 1));
        mCurrentPosition = position;
        mTouchX = mScrollX = position * SCROLL_SIZE_PER_ITEM;
        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
        postInvalidate();
        performItemFocusChanged(position);
        // If player set shuffle on, called setPosition after called snapToPosition.
        // so, reset mTouchState.
        mHandler.removeMessages(MSG_TOUCH_IDLE);
        mTouchState = TOUCH_STATE_REST;
    }

    public int getPosition() {
        return mCurrentPosition;
    }

    public void setPlayingPosition(int position) {
        mPlayingPosition = position;
    }

    private static final int MSG_TOUCH_IDLE = 0;
    private static final int MSG_TOUCH_IDLE_TIMEOUT = 200;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_TOUCH_IDLE:
                int position;
                boolean byUser = isScrolling();
                if (mNextPosition != INVALID_POSITION) {
                    position = Math.max(0, Math.min(mNextPosition, getItemCount() - 1));
                } else {
                    //Log.i(TAG, "MSG_TOUCH_IDLE mScrollX=" + mScrollX + ", mTotalDeltaX : " + mTotalDeltaX);
                    //final int gap = ((mScroller.getFinalX() - mScroller.getStartX()) > 0)
                    //              ? (SCROLL_SIZE_PER_ITEM - SNAP_MOVING_GAP) : SNAP_MOVING_GAP/2;
                    final int gap = (mTotalDeltaX < 0)
                                  ? (SCROLL_SIZE_PER_ITEM - SNAP_MOVING_GAP) : SNAP_MOVING_GAP;
                    position = (mScrollX + gap) / SCROLL_SIZE_PER_ITEM;
                    if (mScrollEnabled) {
                        snapToPosition(position);
                    }
                }
                Log.i(TAG, "MSG_TOUCH_IDLE mCurrentPosition=" + mCurrentPosition + ", position : " + position);
                mTouchState = TOUCH_STATE_REST;
                mNextPosition = INVALID_POSITION;
                if (!mFlingByScroll) {
                    if (mPlayingPosition != position) {
                        snapToPosition(mPlayingPosition);
                    }
                } else {
                    if (mCurrentPosition != position || mPlayingPosition != position) {
                        performItemFocusChanged(position, byUser);
                    }
                    mFlingByScroll = false;
                }
                mCurrentPosition = position;
                break;
            default:
                break;
            }
        }
    };

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mTouchX = mScrollX = mScroller.getCurrX();
            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
            postInvalidate();
            if (mTouchState == TOUCH_STATE_FLING) {
                mHandler.removeMessages(MSG_TOUCH_IDLE);
                mHandler.sendEmptyMessageDelayed(MSG_TOUCH_IDLE, MSG_TOUCH_IDLE_TIMEOUT);
            }
        } else if (mNextPosition != INVALID_POSITION) {
            if (mTouchState == TOUCH_STATE_FLING) {
                mHandler.removeMessages(MSG_TOUCH_IDLE);
                mHandler.sendEmptyMessageDelayed(MSG_TOUCH_IDLE, MSG_TOUCH_IDLE_TIMEOUT);
            }
        } else if (mTouchState == TOUCH_STATE_SCROLLING) {
            final float now = System.nanoTime() / NANOTIME_DIV;
            final float e = (float) Math.exp((now - mSmoothingTime) / SMOOTHING_CONSTANT);
            final float dx = mTouchX - mScrollX;
            mScrollX += dx * e;
            mSmoothingTime = now;

            // Keep generating points as long as we're more than 1px away from the target
            if (dx > 1.f || dx < -1.f) {
                postInvalidate();
            }
        }
    }

    @Override
    public void onDrawItem(Canvas canvas, int i, int whichChunk, Transformation xform, boolean hasPartialItems) {
        Bitmap bitmap = mAdapter.getCachedBitmap(i);

        final int restoreTo = canvas.save();
        canvas.translate(mScrollX + xform.tx, xform.ty);
        canvas.concat(xform.matrix);
        Paint paint = (mTouchState == TOUCH_STATE_SCROLLING ||
                mTouchState == TOUCH_STATE_FLING) ? mPaint : mHQPaint;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (hasPartialItems) {
            Rect rect = mVisualizer.getPartialItemRect(whichChunk);
            if (xform.alpha != 255) {
                canvas.saveLayerAlpha(rect.left, rect.top,
                        rect.right, rect.bottom,
                        xform.alpha, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
            }
            canvas.drawBitmap(bitmap, rect, rect, paint);
        } else {
            if (xform.alpha != 255) {
                int x = 0;
                int w = width;
                if (mShadowLeftDrawable != null) {
                    x -= SHADOW_WIDTH;
                }
                if (mShadowRightDrawable != null) {
                    w += SHADOW_WIDTH;
                }
                canvas.saveLayerAlpha(x, 0, w, height, xform.alpha, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
            }
            if (mThumbBackground != null) {
                mThumbBackground.setBounds(new Rect(0, 0, width, height));
                mThumbBackground.draw(canvas);
            }
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }

        if (mScrollEnabled) {
            mShadowLeftDrawable.setBounds(new Rect(-SHADOW_WIDTH, 0, 0, height));
            mShadowLeftDrawable.draw(canvas);
            mShadowRightDrawable.setBounds(new Rect(width, 0, width + SHADOW_WIDTH, height));
            mShadowRightDrawable.draw(canvas);
        }

        canvas.restoreToCount(restoreTo);
    }

    @Override
    public void onFinishTransition(Canvas canvas) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //final long drawingTime = getDrawingTime();
        final float scrollPos = (float) mTouchX / SCROLL_SIZE_PER_ITEM;
        //long startTime = System.currentTimeMillis();
        mVisualizer.setScrollPosition(scrollPos);
        mVisualizer.draw(canvas);
        //long finishTime = System.currentTimeMillis();
        /*
        Log.i(TAG, "drawing finish diff=" + (finishTime - startTime) +
                ", mTouchState = " + mTouchState +
                ", mScrollX = " + mScrollX +
                ", mAnimationState = " + mVisualizer.getState());
                */
        final boolean finishedTransition = mTouchState == TOUCH_STATE_REST &&
                                           mVisualizer.getState() == Visualizer.STATE_TRANSITION;
        if (finishedTransition) {
            if (mVisualizer.hasAnimation()) {
                mVisualizer.startClosingAnimation();
                mFirstMove = true;
            } else {
                mVisualizer.setState(Visualizer.STATE_IDLE);
                invalidate();
            }
        }
        //Log.i(TAG, "onDraw mScrollEnabled=" + mScrollEnabled +
        //        ", isInTouchMode()=" + isInTouchMode());
        if (mScrollEnabled) {
            canvas.translate(mScrollX, 0);
            if (!isInTouchMode()) {
                mFocusDrawable.draw(canvas);
            }
        } else {
            drawShadowEffect(canvas);
        }
    }

    private void initShadowEffect() {
        int height, offsetY;
        Resources resources = getResources();
        int shadowWidth = (int)resources.getDimension(R.dimen.coverflow_shadow_effect_width);
        int screenWidth = this.getWidth();
        if (mThumbSize == THUMB_SMALL_SIZE) {
            offsetY = (int)resources.getDimension(R.dimen.coverflow_small_shadow_offset);
            height = (int)resources.getDimension(R.dimen.coverflow_small_thumb_height);
        } else {
            offsetY = 0;
            height = (int)resources.getDimension(R.dimen.coverflow_thumb_height);
        }
        mLeftShadowRect = new Rect(0, offsetY, shadowWidth, offsetY + height);
        mRightShadowRect = new Rect(screenWidth - shadowWidth, offsetY, screenWidth, offsetY + height);
        mShadowDrawable = resources.getDrawable(R.drawable.skin_cover_shadow);
        int offsetX = mVisualizer.getStartOffsetX();
        int pageWidth = mVisualizer.getPageWidth();
        mLeftShadowVisibleX = SCROLL_SIZE_PER_ITEM * (offsetX - shadowWidth) / pageWidth;
        mRightShadowVisibleX = ((getItemCount() - 1) * SCROLL_SIZE_PER_ITEM) - mLeftShadowVisibleX;

        if (mScrollEnabled) {
            mShadowLeftDrawable = resources.getDrawable(R.drawable.mus_cover_shadow_l);
            mShadowRightDrawable = resources.getDrawable(R.drawable.mus_cover_shadow_r);
            mThumbBackground = resources.getDrawable(R.drawable.skin_mus_thumb_bg);
        }
    }

    private void drawShadowEffect(Canvas canvas) {
        canvas.translate(mScrollX, 0);
        if (mScrollX > mLeftShadowVisibleX) {
            mShadowDrawable.setBounds(mLeftShadowRect);
            mShadowDrawable.draw(canvas);
        }
        if (mScrollX < mRightShadowVisibleX) {
            mShadowDrawable.setBounds(mRightShadowRect);
            mShadowDrawable.draw(canvas);
        }
    }

    private void initFocusDrawable() {
        Rect rect = mVisualizer.getCenterRect();
        mFocusDrawable.setBounds(rect);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        computeScroll();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("CoverFlowView can only be used in EXACTLY mode.");
        }
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("CoverFlowView can only be used in EXACTLY mode.");
        }
        if (mFirstLayout) {
            mFirstLayout = false;
        }
        setMeasuredDimension(width, height);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private int determineTargetPosition(int current, float offset,
            int velocity, int totalDeltaX) {
        int target;
        if (Math.abs(totalDeltaX) > FLING_DISTANCE &&
                Math.abs(velocity) > mMinimumVelocity) {
            if (totalDeltaX * velocity > 0) {
                if (offset == 0.0f && velocity > 0) {
                    target = current - 1;
                } else {
                    target = velocity > 0 ? current : current + 1;
                }
            } else {
                target = totalDeltaX > 0 ? current : current + 1;
            }
            /*
            } else if (Math.abs(deltaX) > SNAP_TOUCH_SLOP) {
                target = deltaX > 0 ? current : current + 1;
            } else {
                target = (int) (current + offset + 0.5f);
            }
            */
        } else {
            target = (int) (current + offset + 0.5f);
        }
        return target;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(!mTouchEnabled) {
            Log.i(TAG, "onTouchEvent is ignored !!");
            return true;
        }
        acquireVelocityTrackerAndAddMovement(ev);

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
                mTouchState = TOUCH_STATE_SCROLLING;
            } else {
                mTouchState = TOUCH_STATE_DOWN;
            }
            mNextPosition = INVALID_POSITION;
            mLastMotionX = mInitialMotionX = ev.getX();
            mActivePointerId = ev.getPointerId(0);
            mHandler.removeMessages(MSG_TOUCH_IDLE);
            break;
        case MotionEvent.ACTION_MOVE:
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
                mTouchState = TOUCH_STATE_SCROLLING;
            }
            if (mTouchState == TOUCH_STATE_SCROLLING || mTouchState == TOUCH_STATE_DOWN) {
                // Scroll to follow the motion event
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float deltaX = mLastMotionX - x;
                if (Math.abs(deltaX) > SNAP_TOUCH_SLOP) {
                    mTouchState = TOUCH_STATE_SCROLLING;
                }
                if (deltaX < -SNAP_TOUCH_SLOP) {
                    if (mTouchX > 0) {
                        stopAnimationIfNeeded();
                        mTouchX += Math.max(-mTouchX, deltaX);
                        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
                        invalidate();
                    }
                } else if (deltaX > SNAP_TOUCH_SLOP) {
                    final float availableToScroll = (getItemCount() - 1) * SCROLL_SIZE_PER_ITEM - mTouchX;
                    if (availableToScroll > 0) {
                        stopAnimationIfNeeded();
                        mTouchX += Math.min(availableToScroll, deltaX);
                        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
                        invalidate();
                    }
                } else {
                    return true;
                }
                mLastMotionX = x;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) velocityTracker.getXVelocity(mActivePointerId);
                //Log.i(TAG, "ACTION_UP mScrollX=" + mScrollX);
                final int position = mScrollX / SCROLL_SIZE_PER_ITEM;
                final float offset = (float) (mScrollX % SCROLL_SIZE_PER_ITEM) / SCROLL_SIZE_PER_ITEM;
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final int deltaX = (int) (x - mLastMotionX);
                mTotalDeltaX = (int) (x - mInitialMotionX);
                if (mScrollEnabled) {
                    if (Math.abs(velocityX) > mMinimumVelocity) {
                        if (mTotalDeltaX * velocityX < 0) {
                            Log.i(TAG, "ACTION_UP velocityX=" + velocityX + ", totalDeltaX=" + mTotalDeltaX);
                            velocityX = -velocityX;
						}

                        mScroller.fling((int)(mTouchX), 0,
                                -velocityX, 0,
                                0, getScrollRange(),
                                0, 0,
                                SCROLL_SIZE_PER_ITEM, 0);
                        mTouchState = TOUCH_STATE_FLING;
                        invalidate();
                    } else {
                        Log.i(TAG, "ACTION_UP position=" + position +
                                ", offset=" + offset + ", velocityX=" + velocityX +
                                ", deltaX=" + deltaX +
                                ", totalDeltaX=" + mTotalDeltaX);
                        int next = determineTargetPosition(position, offset, velocityX, mTotalDeltaX);
                        snapToPosition(next, true);
                    }
                } else {
                    Log.i(TAG, "ACTION_UP position=" + position +
                            ", offset=" + offset + ", velocityX=" + velocityX +
                            ", deltaX=" + deltaX +
                            ", totalDeltaX=" + mTotalDeltaX);
                    int next = determineTargetPosition(position, offset, velocityX, mTotalDeltaX);
                    snapToPosition(next, true);
                }
                mFlingByScroll = true;
                mActivePointerId = INVALID_POINTER;
                releaseVelocityTracker();
                return true;
            } else if (action == MotionEvent.ACTION_UP &&
                    mTouchState == TOUCH_STATE_DOWN) {
                int pointerIndex = ev.findPointerIndex(mActivePointerId);
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);
                int p = determinTouchPosition(x, y);
                if (p >= 0 && p < getItemCount()) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(this, p);
                    }
                }
                mFlingByScroll = true;
                mHandler.removeMessages(MSG_TOUCH_IDLE);
                mHandler.sendEmptyMessageDelayed(MSG_TOUCH_IDLE, MSG_TOUCH_IDLE_TIMEOUT);
            }
            mTouchState = TOUCH_STATE_REST;
            mActivePointerId = INVALID_POINTER;
            releaseVelocityTracker();
            break;
        case MotionEvent.ACTION_POINTER_UP:
            onSecondaryPointerUp(ev);
            break;
        }
        return true;
    }

    private int determinTouchPosition(float x, float y) {
        int p = mVisualizer.determinPosition(x, y);
        Log.i(TAG, "determinTouchPosition p=" + p + ", x=" + x + ", y=" + y);
        if (p < 0) {
            return -1;
        }
        int scrollPosition = mScrollX / SCROLL_SIZE_PER_ITEM;
        return scrollPosition + p - mVisualizer.getCenterFramePosition();
    }

    private void performItemFocusChanged(int position) {
        performItemFocusChanged(position, false);
    }

    private void performItemFocusChanged(int position, boolean byUser) {
        if (getItemCount() != 0 && mOnItemFocusChangeListener != null) {
            mOnItemFocusChangeListener.onItemFocusChange(this, position, byUser);
        }
    }

    private int getScrollRange() {
        final int count = mAdapter.getCount();
        if (count == 0)
            return 0;
        return (count - 1) * SCROLL_SIZE_PER_ITEM;
    }

    public void snapToPosition(int position) {
        snapToPosition(position, false);
    }

    public void moveTo(int step) {
        int n = getItemCount();
        if (n == 0) {
            Log.w(TAG, "No CoverFlow items, ignore moveTo method");
            return;
        }
        int p = (mCurrentPosition + step) % n;
        if (p < 0) p = n + p;
        snapToPosition(p);
        performItemFocusChanged(p);
        mCurrentPosition = p;
    }

    private void snapToPosition(int position, boolean settle) {
        if (position < 0 || position >= getItemCount()) {
            Log.i(TAG, "Invalid position : " + position + ", count=" + getItemCount());
            if (mTouchState != TOUCH_STATE_REST) {
                mHandler.removeMessages(MSG_TOUCH_IDLE);
                mHandler.sendEmptyMessageDelayed(MSG_TOUCH_IDLE, MSG_TOUCH_IDLE_TIMEOUT);
            }
            return;
        }
        final int newX = position * SCROLL_SIZE_PER_ITEM;
        final int delta = newX - mScrollX;
        if (delta == 0 || mNextPosition == position) {
            Log.i(TAG, "snapToPosition ignored position=" + position +
                    ", mNextPosition=" + mNextPosition +
                    ", mScrollX=" + mScrollX +
                    ", delta=" + delta);
            // Album art is not centered problem. Blocking code below.
            if (!mHighModel) {
                Log.i(TAG, "snapToPosition Mid model");
                if (delta == 0) {
                    mNextPosition = position;
                    mHandler.removeMessages(MSG_TOUCH_IDLE);
                    mHandler.sendEmptyMessageDelayed(MSG_TOUCH_IDLE, MSG_TOUCH_IDLE_TIMEOUT);
                }
                return;
            }
        }
        mNextPosition = position;
        //Log.i(TAG, "snapToPosition next position=" + position);

        final int screenDelta = Math.max(1, Math.abs(position - mCurrentPosition));

        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        if (settle) {
            mScrollInterpolator.setDistance(screenDelta);
        } else {
            mScrollInterpolator.disableSettle();
        }
        mScroller.startScroll(mScrollX, 0, delta, 0, FLING_ANIMATION_DURATION);
        mTouchState = TOUCH_STATE_FLING;
        invalidate();
    }

    private void stopAnimationIfNeeded() {
        if (mVisualizer != null) {
            if (mFirstMove && mVisualizer.getState() == Visualizer.STATE_INTRO_ANIMATION) {
                mVisualizer.stopAnimation();
                mFirstMove = false;
            } else {
                mVisualizer.setState(Visualizer.STATE_TRANSITION);
            }
        }
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public boolean isScrolling() {
        return (mTouchState == TOUCH_STATE_FLING) || (mTouchState == TOUCH_STATE_SCROLLING);
    }

    public void setTouchEnabled(boolean enabled) {
        Log.i(TAG, "setTouchEnabled  " + enabled);
        mTouchEnabled = enabled;
    }
}
