package com.cromulator.picontrol;

/**
 * Value class and builder for sending messages to the robot. RestClient uses this class
 * to send appropriate values to the robot API.
 * cameraX - Degrees: -45 to 45
 * cameraY - Degrees: -45 to 45
 * motorLeft - Percent Power: -100 to 100
 * motorRight - Percent Power: -100 to 100
 */
public class Command {

    private static int NO_VALUE = Integer.MAX_VALUE;
    public final static int GRABBY_HAND_STOP = 0;
    public final static int GRABBY_HAND_OPEN = 1;
    public final static int GRABBY_HAND_CLOSE = 2;

    private final Integer motorLeft;
    private final Integer motorRight;
    private final Integer cameraPan;
    private final Integer cameraTilt;
    private final Integer grabbyHand;

    private Command(int mL, int mR, int pan, int tilt, int gh) {
        motorLeft = mL;
        motorRight = mR;
        cameraPan = pan;
        cameraTilt = tilt;
        grabbyHand = gh;
    }

    public boolean hasMotorLeft() { return motorLeft != NO_VALUE; };
    public boolean hasMotorRight() { return motorRight != NO_VALUE; }
    public boolean hasCameraPan() { return cameraPan != NO_VALUE; }
    public boolean hasCameraTilt() { return cameraTilt != NO_VALUE; }
    public boolean hasGrabbyHand() { return grabbyHand != NO_VALUE; }

    public Integer getGrabbyHand() { return (hasGrabbyHand()) ? grabbyHand : null; }
    public Integer getMotorLeft() { return (hasMotorLeft()) ? motorLeft : null; }
    public Integer getMotorRight() { return (hasMotorRight()) ? motorRight : null; }
    public Integer getCameraPan() { return (hasCameraPan()) ? cameraPan : null; }
    public Integer getCameraTilt() { return (hasCameraTilt()) ? cameraTilt : null; }

    public String toString() {
        return " mL=" + getMotorLeft() + " mR=" + getMotorRight()
                + " pan=" + getCameraPan() + " tilt=" + getCameraTilt()
                + " grabby=" + getGrabbyHand();
    }

    /**
     * Builder class to construct robot command.
     */
    public static class Builder {

        private int motorLeft = NO_VALUE;
        private int motorRight = NO_VALUE;
        private int pan = NO_VALUE;
        private int tilt = NO_VALUE;
        private int grabbyHand = NO_VALUE;

        public Builder() {}

        public Builder(int mL, int mR, int pan, int tilt, int grabbyHand) {
            motorLeft = mL;
            motorRight = mR;
            this.pan = pan;
            this.tilt = tilt;
            this.grabbyHand = grabbyHand;
        }

        /** @param mL Motor left power -100 to 100 % */
        public Builder setMotorLeft(int mL) { motorLeft = mL; return this; }

        /** @param mR Motor left power -100 to 100 % */
        public Builder setMotorRight(int mR) { motorRight = mR; return this; }

        /** @param pan Camera Pan -45 to 45 deg % */
        public Builder setCameraPan(int pan) { this.pan = pan; return this; }

        /** @param tilt Camera Tilt -45 to 45 deg % */
        public Builder setCameraTilt(int tilt) { this.tilt = tilt; return this; }

        public Builder setGrabbyHand(int hand) { this.grabbyHand = hand; return this; }

        public Command build() {
            return new Command(motorLeft, motorRight, pan, tilt, grabbyHand);
        }
    }
}
