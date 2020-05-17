package com.shoeboxscientist.picontrol;

import android.view.MotionEvent;
import android.view.View;

/**
 * Returns a position as a % for x,y from +50% to -50% based on a given view.
 *
 */
public class JoyStickPosition {

    private int lastX = Integer.MAX_VALUE;
    private int lastY = Integer.MAX_VALUE;

    public JoyStickPosition(final View view, final Listener listener) {

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float width = (float) view.getWidth();
                float height = (float) view.getHeight();

                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {

                    float x = event.getX();
                    float y = event.getY();

                    float xpercent = toPercent(x, width);
                    float ypercent = toPercent(y, height);

                    int xp = (int) Math.round(xpercent);
                    int yp = (int) Math.round(ypercent);

                    if (xp != lastX || yp != lastY) {
                        lastX = xp;
                        lastY = yp;
                        listener.onPositionUpdate(xp, yp);
                    }

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        listener.onDown(xp, yp);
                    }
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    listener.onUp();
                }

                return false;
            }
        });

    }


    private float toPercent(float pos, float size) {
        return (float) (((pos - (size / 2.)) / size ) * 100.);
    }


    public static interface Listener {
        void onPositionUpdate(int x, int y);
        void onDown(int x, int y);
        void onUp();
    }

}
