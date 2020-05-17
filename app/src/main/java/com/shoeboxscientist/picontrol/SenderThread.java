package com.shoeboxscientist.picontrol;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Sends a command off the UI thread & returns response to mNetworkCallback
 */
public class SenderThread extends Thread {
    private Handler handler;

    private final RestClient mClient;
    private final Handler mNetworkHandler;

    private static final String TAG = "PiControl.Sender";

    public SenderThread(RestClient client, Handler networkHandler) {
        mClient = client;
        mNetworkHandler = networkHandler;
    }

    public void run() {
        Looper.prepare();
        handler = new Handler();
        Log.d(TAG, "Created Network Handler.");
        Looper.loop();
    }

    public synchronized void ping() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Send the message to the network.
                int code = mClient.sendCommand(new Command.Builder(0, 0, 0, 0, 0, 0, 0, 0, 0).build());

                // Send the response back to the UI thread.
                Message rtn = mNetworkHandler.obtainMessage(PiControlApplication.MSG_PING_RESPONSE,
                        code, 0);
                mNetworkHandler.sendMessage(rtn);
            }
        });
    }

    public synchronized void sendCommand(final Command cmd, int seconds) {

        Runnable command = new Runnable() {
            @Override
            public void run() {
                {
                    // Send the message to the network.
                    int code = mClient.sendCommand(cmd);

                    // Send the response back to the UI thread.
                    Message rtn = mNetworkHandler.obtainMessage(PiControlApplication.MSG_RESPONSE,
                            code, 0);
                    mNetworkHandler.sendMessage(rtn);
                }
            }
        };

        Log.d(TAG, "Sending command " + cmd);
        if (seconds == 0) {
            handler.post(command);
        } else {
            handler.postDelayed(command, seconds * 1000);
        }
    }
}
