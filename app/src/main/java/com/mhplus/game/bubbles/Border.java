package com.mhplus.game.bubbles;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Border extends Drawable {
    private int mPosX;
    private int mPosY;
    private int mWidth;
    private int mHeight;
    private Paint mPaint = new Paint();

    @SuppressLint("ResourceAsColor")
    Border(int x, int y, int width, int height, int color) {
        super();
        mPosX = x;
        mPosY = y;
        mWidth = width;
        mHeight = height;
        mPaint.setColor(color);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(mPosX, mPosY, mPosX + mWidth , mPosY + mHeight, mPaint);
    }

    @Override
    public void setAlpha(int i) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    boolean isHit(Bubble bubble) {
        int r = bubble.getRadius();
        if (mPosY == 0 && mHeight <= 10) { // top
            int top = bubble.getPositionY() - r;
            return top <= mPosY + mHeight;
        } else if (mPosX == 0 && mWidth <= 10) { // left
            int left = bubble.getPositionX() - r;
            return left <= mPosX + mWidth;
        } else if (mPosX >= 1430 && mWidth <= 10) { // right
            int right = bubble.getPositionX() + r;
            return (right >= mPosX);
        } else if (mPosY == 2000 && mHeight <= 10) {
            int bottom = bubble.getPositionY() + r;
            return bottom >= mPosY;
        }
        return false;
    }
    int getPositionX() {
        return mPosX;
    }

    int getPositionY() {
        return mPosY;
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
    }
}
