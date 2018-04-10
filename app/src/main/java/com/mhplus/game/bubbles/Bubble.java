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

    boolean isHit(Border border) {
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
        int x = border.getPositionX();
        int y = border.getPositionY();
        int w = border.getWidth();
        int h = border.getHeight();
        if (y == 0 && h <= 10) { // top
            if (mVelocityY < 0) {
                mVelocityY = -mVelocityY;
            }
        } else if (x == 0 && w <= 10) { // left
            if (mVelocityX < 0) {
                mVelocityX = -mVelocityX;
            }
        } else if (x >= 1430 && w <= 10) { // right
            if (mVelocityX > 0) {
                mVelocityX = -mVelocityX;
            }
        } else if (y == 2000 && h <= 10){ // bottom
            if (mVelocityY > 0) {
                mVelocityY = -mVelocityY;
            }
        } else {
            // No update
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
