package com.cromulator.picontrol;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;

/**
 * Receives high-level commands (probably from javascript executing in the webview) and runs them.
 * Running won't commence until the execute command is received. This is necessary since javascript
 * can't "wait" very easily so I have to do it here.
 *
 * Commands generated here end up at the RestClient which expects parameters of the form:
 * cameraX - Degrees: -45 to 45
 * cameraY - Degrees: -45 to 45
 * motorLeft - Percent Power: -100 to 100
 * motorRight - Percent Power: -100 to 100
 */
public class RobotCommandExecutor implements WebViewManager.RobotEventListener {

    private static String TAG = "PiControl.Commands";

    private ArrayList<ExecutionCommand> mCommands = new ArrayList<>();

    private static final String POSITION_UP = "up";
    private static final String POSITION_DOWN = "down";
    private static final String POSITION_LEFT = "left";
    private static final String POSITION_RIGHT = "right";
    private static final String POSITION_CENTRE = "centre";

    private final ExecutionThread mThread;
    private final ExecutionListener mExecutionListener;

    public static interface ExecutionListener {
        void print(String msg);
    }

    public RobotCommandExecutor(ExecutionListener listener) {
        mThread = new ExecutionThread();
        mExecutionListener = listener;
        mThread.start();
    }

    private final Command stop = new Command.Builder()
            .setMotorLeft(0)
            .setMotorRight(0)
            .build();

    @Override
    public void reset() {
        mCommands.clear();
    }

    @Override
    public void print(String string) {
        mCommands.add(new ExecutionCommand(string));
    }

    @Override
    public void forward(String power) {
        int p = parse(power);
        Command fwd = new Command.Builder()
                .setMotorLeft(p)
                .setMotorRight(p)
                .build();
        mCommands.add(new ExecutionCommand(fwd));
    }

    @Override
    public void backward(String power) {
        int p = parse(power);
        Command bkwd = new Command.Builder()
                .setMotorLeft(p * -1)
                .setMotorRight(p * -1)
                .build();

        mCommands.add(new ExecutionCommand(bkwd));
    }

    @Override
    public void rotateLeft() {
        Command left = new Command.Builder()
                .setMotorLeft(-100)
                .setMotorRight(100)
                .build();

        mCommands.add(new ExecutionCommand(left));
    }

    @Override
    public void rotateRight() {
        Command right = new Command.Builder()
                .setMotorLeft(100)
                .setMotorRight(-100)
                .build();

        mCommands.add(new ExecutionCommand(right));
    }

    @Override
    public void camera(String position) {

        Command.Builder posn = new Command.Builder();
        if (POSITION_UP.equals(position)) {
            posn.setCameraPan(0).setCameraTilt(-45);
        } else if (POSITION_DOWN.equals(position)) {
            posn.setCameraPan(0).setCameraTilt(45);
        } else if (POSITION_LEFT.equals(position)) {
            posn.setCameraPan(-45).setCameraTilt(0);
        } else if (POSITION_RIGHT.equals(position)) {
            posn.setCameraPan(45).setCameraTilt(0);
        } else if (POSITION_CENTRE.equals(position)) {
            posn.setCameraPan(0).setCameraTilt(0);
        } else {
            Log.w(TAG, "Camera position not recognized: " + position);
        }

        mCommands.add(new ExecutionCommand(posn.build()));
    }

    @Override
    public void wait(String seconds) {
        mCommands.add(new ExecutionCommand(parse(seconds)));
    }

    @Override
    public void execute() {
        Log.d(TAG, "Starting commands...");
        mThread.nextCommand(0);
    }

    private void executionFinished() {
        Log.d(TAG, "Execution finished.");
        PiControlApplication.getInstance().sendCommand(stop);

    }

    private int parse(String seconds) {
        try {
            return Integer.parseInt(seconds);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse interval: " + seconds);
            return 0;
        }
    }

    /**
     * Value class to store the required command or a time interval.
     * NULL commands are interpreted as "wait" for the specified number of seconds or print
     * message.
     *
     * TODO: Use a ExecutionCommand subclasses for things like print.
     */
    private static class ExecutionCommand {
        public final Command cmd;
        public final int ms;
        public final String message;

        public ExecutionCommand(String m) {
            message = m;
            ms = 0;
            cmd = null;
        }

        public ExecutionCommand(Command c) {
            cmd = c;
            ms = 0;
            message = null;
        }

        public ExecutionCommand(int ms) {
            cmd = null;
            this.ms = ms;
            message = null;
        }

        public String toString() {
            return "cmd: " + cmd + " in " + ms + "ms";
        }
    }


    private class ExecutionThread extends Thread {

        private Handler handler;

        public ExecutionThread() {
        }

        public void run() {
            Looper.prepare();
            handler = new Handler();
            Log.d(TAG, "Created Execution Handler.");
            Looper.loop();
        }

        private synchronized void runNextCommand() {

            if (mCommands.size() == 0) {
                Log.d(TAG, "Commands finished, return.");
                executionFinished();
                return;
            }

            ExecutionCommand c = mCommands.remove(0);
            Log.d(TAG, "runNextCommand: " + c);
            if (c.cmd != null) {
                PiControlApplication.getInstance().sendCommand(c.cmd);
                nextCommand(0);
            } else if (c.message != null) {
                if (mExecutionListener != null) {
                    mExecutionListener.print(c.message);
                    nextCommand(0);
                }
            } else {
                nextCommand(c.ms);
            }
        }

        public void nextCommand(long ms) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    runNextCommand();
                }
            };

            if (ms == 0) {
                Log.d(TAG, "Post: NOW");
                handler.post(run);
            } else {
                Log.d(TAG, "Post in " + ms + "ms");
                handler.postDelayed(run, ms);
            }
        }
    }
}
