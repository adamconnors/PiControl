package com.shoeboxscientist.picontrol;

import android.view.MotionEvent;
import android.view.View;

/**
 * Contains the listenrs and controller logic for the joysticks.
 */
public class JoystickController {

    private int cameraLeftRight;
    private int cameraUpDown;
    private int motorLeft;
    private int motorRight;
    private int grabbyHand;

    private int previousCameraLeftRight = Integer.MAX_VALUE;
    private int previousCameraUpDown = Integer.MAX_VALUE;
    private int previousMotorLeft = Integer.MAX_VALUE;
    private int previousMotorRight = Integer.MAX_VALUE;
    private int previousGrabbyHand = Integer.MAX_VALUE;

    public JoystickController() {

    }

    public JoyStickPosition.Listener getMotorListener() {
        return new JoyStickPosition.Listener() {

            @Override
            public void onPositionUpdate(int x, int y) {

                int[] values = RobotController.calculateMotorPower(x, y);
                motorLeft = values[0];
                motorRight = values[1];

                // Don't waste time sending dupe commands.
                if (previousMotorLeft != motorLeft || previousMotorRight != motorRight) {
                    previousMotorRight = motorRight;
                    previousMotorLeft = motorLeft;
                    sendCommand();
                }
            }

            @Override
            public void onStop() {
                motorLeft = 0;
                motorRight = 0;
                sendCommand();
            }
        };
    }

    public JoyStickPosition.Listener getCameraListener() {
        return new JoyStickPosition.Listener() {
            @Override
            public void onPositionUpdate(int x, int y) {
                // x & y are %ages from -50 to 50, but servo range is %age, -100 to 100.
                // You wouldn't believe I wrote both sides of that system, but anyway...
                // Round to 20degs to avoid sending too many requests.
                // % changes < 20 don't make much difference to the motors so quantise it here.
                cameraLeftRight = (int) Math.round(x / 10f) * 20;
                cameraUpDown = (int) Math.round(y / 10f) * 20;

                if (cameraLeftRight != previousCameraLeftRight
                        || cameraUpDown != previousCameraUpDown) {
                    previousCameraLeftRight = cameraLeftRight;
                    previousCameraUpDown = cameraUpDown;
                    sendCommand();
                }
            }

            @Override
            public void onStop() {
                cameraLeftRight = 0;
                cameraUpDown = -2;
                sendCommand();
            }
        };
    }

    public View.OnTouchListener getGrabbyHandListener(final boolean open) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    grabbyHand = (open) ? Command.GRABBY_HAND_OPEN : Command.GRABBY_HAND_CLOSE;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    grabbyHand = Command.GRABBY_HAND_STOP;
                }

                if (grabbyHand != previousGrabbyHand) {
                    sendCommand();
                }

                previousGrabbyHand = grabbyHand;
                return true;
            }
        };
    }

    private void sendCommand() {
        PiControlApplication.getInstance().sendCommand(new Command.Builder(motorLeft, motorRight,
                cameraLeftRight, cameraUpDown, grabbyHand).build());
    }

}
