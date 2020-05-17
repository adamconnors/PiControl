package com.shoeboxscientist.picontrol;

import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Contains the listenrs and controller logic for the joysticks.
 */
public class JoystickController {

    private static final int CONTROL_MODE_MOTORS = 1;
    private static final int CONTROL_MODE_ARM = 2;
    private int controlMode = CONTROL_MODE_MOTORS;

    private int cameraLeftRight;
    private int cameraUpDown;
    private int motorLeft;
    private int motorRight;
    private int grabbyHand;

    private int armBase, armElbow, armWrist, armHand;
    private int prevArmBase = Integer.MAX_VALUE, prevArmElbow = Integer.MAX_VALUE,
            prevArmWrist = Integer.MAX_VALUE, prevArmHand = Integer.MAX_VALUE;

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

                if (controlMode == CONTROL_MODE_MOTORS) {
                    // Controlling the motors
                    int[] values = RobotController.calculateMotorPower(x, y);
                    motorLeft = values[0];
                    motorRight = values[1];

                    // Don't waste time sending dupe commands.
                    if (motorLeft != previousMotorLeft || motorRight != previousMotorRight) {
                        previousMotorRight = motorRight;
                        previousMotorLeft = motorLeft;
                        sendCommand();
                    }
                }
            }

            @Override
            public void onUp() {
                if (controlMode == CONTROL_MODE_MOTORS) {
                    motorLeft = 0;
                    motorRight = 0;
                    sendCommand();
                } else {
                    armWrist = 0;
                    armHand = 0;
                    sendCommand();
                }
            }

            public void onDown(int x, int y) {
                if(controlMode == CONTROL_MODE_ARM) {
                    armWrist = (int)Math.floor(y/5);
                    armHand = (int)Math.floor(x/5);
                    sendCommand();
                }
            }
        };
    }

    public JoyStickPosition.Listener getCameraListener() {
        return new JoyStickPosition.Listener() {
            @Override
            public void onPositionUpdate(int x, int y) {
                if (controlMode == CONTROL_MODE_MOTORS) {
                    // x & y are %ages from -50 to 50, but servo range is %age, -100 to 100.
                    // You wouldn't believe I wrote both sides of that system, but anyway...
                    // Round to 20degs to avoid sending too many requests.
                    // % changes < 20 don't make much difference to the motors so quantise it here.
                    cameraLeftRight = (int) Math.round(x / 10f) * 20;
                    cameraUpDown = (int) Math.round(y / 10f) * 20;

                    // Temporary hack to make camera up/down control the wrist instead.
                    // Centre position is -60 so range is now 40-->-160
                    // cameraUpDown -= 60;

                    if (cameraLeftRight != previousCameraLeftRight || cameraUpDown != previousCameraUpDown) {
                        previousCameraLeftRight = cameraLeftRight;
                        previousCameraUpDown = cameraUpDown;
                        sendCommand();
                    }
                }
            }

            @Override
            public void onDown(int x, int y) {
                if (controlMode == CONTROL_MODE_ARM) {
                    armBase = (int)Math.floor(x/5);
                    armElbow = (int)Math.floor(y/5);
                    sendCommand();
                }

            }

            @Override
            public void onUp() {
                if (controlMode == CONTROL_MODE_MOTORS) {
                    cameraLeftRight = 0;
                    cameraUpDown = -2;
                    sendCommand();
                } else {
                    armBase = 0;
                    armElbow = 0;
                    sendCommand();
                }
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

    public SwitchCompat.OnCheckedChangeListener getModeToggleListener() {
        return new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    controlMode = CONTROL_MODE_ARM;
                } else {
                    controlMode = CONTROL_MODE_MOTORS;
                }
            }
        };
    }

    private void sendCommand() {
        PiControlApplication.getInstance().sendCommand(new Command.Builder(
                motorLeft, motorRight,
                cameraLeftRight, cameraUpDown,
                armBase, armElbow, armWrist, armHand,
                grabbyHand).build());
    }

}
