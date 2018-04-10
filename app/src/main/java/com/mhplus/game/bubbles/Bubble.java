package com.mhplus.game.bubbles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

class Bubble {
    private static final String TAG = "Bubble";
    private static final float DECELERATION = 0.85F;

    private int mPosX;
    private int mPosY;
    private int mRadius;
    private Drawable mDrawable;

    private int mVelocityX;
    private int mVelocityY;

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

    void run(int vx, int vy) {
        mVelocityX = vx;
        mVelocityY = vy;
    }

    void updateState() {
        mVelocityX *= DECELERATION;
        mVelocityY *= DECELERATION;
        int dx = (int)(mVelocityX * 0.016);
        int dy = (int)(mVelocityY * 0.016);
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
