package com.mhplus.game.bubbles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

public class Bubble {
    private int mPosX;
    private int mPosY;
    private int mRadius;
    Drawable mDrawable;

    Bubble(Context context, int x, int y, int r, int id) {
        mPosX = x;
        mPosY = y;
        mRadius = r;
        mDrawable = ContextCompat.getDrawable(context, id);
    }

    public void draw(Canvas canvas) {
        mDrawable.setBounds(mPosX - mRadius, mPosY - mRadius,
                mPosX + mRadius, mPosY + mRadius);
        mDrawable.draw(canvas);
    }

    public void moveTo(int x, int y) {
        mPosX = x;
        mPosY = y;
    }
}
