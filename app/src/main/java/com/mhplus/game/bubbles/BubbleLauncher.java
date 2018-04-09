package com.mhplus.game.bubbles;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

public class BubbleLauncher {
    private static final String TAG = "BubbleLauncher";

    private static int MAX_SLOPE = 172;
    private static int MIN_SLOPE = 8;
    private int mDirection;
    private int mAngle;
    private BitmapDrawable mDrawable;

    BubbleLauncher() {
        mDirection = -1; // left
        mAngle = 90;
    }

    public void draw(Canvas canvas) {
        //mDrawable.draw(canvas);
    }
}
