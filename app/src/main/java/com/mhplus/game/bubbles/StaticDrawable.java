package com.mhplus.game.bubbles;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class StaticDrawable extends Drawable {
    private int mPosX;
    private int mPosY;
    private int mWidth;
    private int mHeight;
    private Bitmap mBitmap;
    private Paint mPaint = new Paint();

    @SuppressLint("ResourceAsColor")
    public StaticDrawable(int x, int y, int width, int height, Bitmap bitmap, int color) {
        super();
        mPosX = x;
        mPosY = y;
        mWidth = width;
        mHeight = height;
        mBitmap = bitmap;
//        mPaint.setAntiAlias(false);
//        mPaint.setFilterBitmap(false);
        mPaint.setColor(color);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(mPosX, mPosY, mPosX + mWidth , mPosY + mHeight, mPaint);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
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
}