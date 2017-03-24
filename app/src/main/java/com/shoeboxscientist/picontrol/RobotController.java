package com.shoeboxscientist.picontrol;

/**
 * Converts joystick x, y inputs into raw values to be sent to the robot.
 */
public class RobotController {

    /**
     * Returns power left / power right based on joystick x / y position.
     */
    public static int[] calculateMotorPower(int x, int y) {
        // x & y are %ages from -50 to 50, motor power is %age from -100 to 100
        // split across left / right according to x position.
        // 100, 50      100,100     100, 50
        // -100,100      0,0       100,-100
        // -50,-100   -100,-100    -100, -50
        int power = y * -2;
        int turningPowerLeft = x * 2;
        int turningPowerRight = x * -2;

        // Calculate total as power + turning power.
        float leftPower = (float) power + turningPowerLeft;
        float rightPower = (float) power + turningPowerRight;

        // Scale both down so largest is 100%
        float max = Math.max(Math.abs(leftPower), Math.abs(rightPower));
        if (max > 100f) {
            float scale = 100f / (float) max;
            leftPower *= scale;
            rightPower *= scale;
        }

        // Lock motors together if they are close to each other, so you can drive straight
        // more easily.
        if (Math.abs(leftPower - rightPower) <= 35) {
            max = Math.max(leftPower, rightPower);
            leftPower = (int) max;
            rightPower = (int) max;
        }

        // % changes < 20 don't make much difference to the motors so quantise it here.
        int motorLeft = (int) Math.round(leftPower / 20f) * 20;
        int motorRight = (int) Math.round(rightPower / 20f) * 20;
        return new int[] { motorLeft, motorRight };
    }
}
