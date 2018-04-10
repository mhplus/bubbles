package com.mhplus.game.bubbles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

class Bubble {
    private static final String TAG = "Bubble";
    private static final float DECELERATION = 3.0F;

    private int mPosX;
    private int mPosY;
    private int mRadius;
    private Drawable mDrawable;

    private int mVelocityX;
    private int mVelocityY;
    private long mUpdatedTime = -1;

    Bubble(Context context, int x, int y, int r, int id) {
        mPosX = x;
        mPosY = y;
        mRadius = r;
        mDrawable = ContextCompat.getDrawable(context, id);
    }

    boolean isHit(Bubble b) {
        return false;
    }

    int getPositionX() {
        return mPosX;
    }

    int getPositionY() {
        return mPosY;
    }

    int getRadius() {
        return mRadius;
    }

    void updateDirection(Border border) {
        int type = border.getType();
        Log.i(TAG, "updateDirection borderType=" + type);
        Log.i(TAG, "updateDirection mVelocityX=" + mVelocityX);
        Log.i(TAG, "updateDirection mVelocityY=" + mVelocityY);
        if (type == Border.BORDER_TYPE_TOP) {
            if (mVelocityY < 0) {
                mVelocityY = -mVelocityY;
            }
        } else if (type == Border.BORDER_TYPE_LEFT) {
            if (mVelocityX < 0) {
                mVelocityX = -mVelocityX;
            }
        } else if (type == Border.BORDER_TYPE_RIGHT) {
            if (mVelocityX > 0) {
                mVelocityX = -mVelocityX;
            }
        } else {
            if (mVelocityY > 0) {
                mVelocityY = -mVelocityY;
            }
        }
    }

    void run(int vx, int vy, long updatedTime) {
        mVelocityX = vx;
        mVelocityY = vy;
        mUpdatedTime = updatedTime;
    }

    void updateState() {
        long current = System.currentTimeMillis();
        long dt = current - mUpdatedTime;
        if (dt == 0) {
            return;
        }
        Log.d(TAG, "updateState mUpdatedTime=" + mUpdatedTime + ", current=" + current + ", dt=" + dt);
        mUpdatedTime = current;
        float rate = (DECELERATION * dt) / 1000.F;
        Log.d(TAG, "handleMessage rate=" + rate + ", vx=" + mVelocityX + ", vy=" + mVelocityY);
        mVelocityX -= (int) (mVelocityX * rate);
        mVelocityY -= (int) (mVelocityY * rate);
        int dx = (int)(mVelocityX * dt / 1000.F);
        int dy = (int)(mVelocityY * dt / 1000.F);
        if (dx == 0 && dy == 0) {
            mVelocityX = 0;
            mVelocityY = 0;
        }
        moveTo(mPosX + dx, mPosY + dy);
        Log.d(TAG, "handleMessage Bubble position : X=" + mPosX +", Y=" + mPosY);
    }

    boolean isMoving() {
        return mVelocityX != 0 || mVelocityY != 0;
    }

    void draw(Canvas canvas) {
        mDrawable.setBounds(mPosX - mRadius, mPosY - mRadius,
                mPosX + mRadius, mPosY + mRadius);
        mDrawable.setAlpha((int)(256 * 0.8));
        mDrawable.draw(canvas);
    }

    void moveTo(int x, int y) {
        mPosX = x;
        mPosY = y;
    }
}
