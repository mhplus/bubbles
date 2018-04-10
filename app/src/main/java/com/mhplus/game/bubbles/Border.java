package com.mhplus.game.bubbles;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Border extends Drawable {
    static final int BORDER_TYPE_LEFT = 0;
    static final int BORDER_TYPE_RIGHT = 1;
    static final int BORDER_TYPE_TOP = 2;
    static final int BORDER_TYPE_BOTTOM = 3;
    private Rect mRect;
    private Paint mPaint = new Paint();
    private int mType;

    @SuppressLint("ResourceAsColor")
    Border(int type, int x, int y, int width, int height, int color) {
        super();
        mType = type;
        mRect = new Rect(x, y, x + width, y + height);
        mPaint.setColor(color);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(mRect, mPaint);
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
        int x = bubble.getPositionX();
        int y = bubble.getPositionY();
        int r = bubble.getRadius();
        if (mType == BORDER_TYPE_LEFT) {
            return x - r <= mRect.right;
        } else if (mType == BORDER_TYPE_RIGHT) {
            return x + r >= mRect.left;
        } else if (mType == BORDER_TYPE_TOP) {
            return y - r <= mRect.bottom;
        } else {
            return y + r >= mRect.top && y - r <= mRect.bottom;
        }
    }

    int getType() {
        return mType;
    }
}
