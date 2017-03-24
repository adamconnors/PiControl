package com.shoeboxscientist.picontrol;

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Application activity, handles all network requests and callbacks.
 */
public class PiControlApplication extends Application {

    private static final String TAG = "PiControl";

    private static PiControlApplication mInstance;
    private NetworkCallbackInterface mNetworkCallbackActivity;

    private Handler mNetworkHandler;
    private SenderThread mNetworkThread;
    private RestClient mClient;

    private String mLastRunScript = null;

    // Whether to ping or not or whether to show connection status.
    private boolean mIsConnected = false;

    // Shared Preferences wrapper for all our shared settings.
    private Settings mSettings;

    // Helper class for connecting to WiFi hotspot.
    private HotspotManager mHotspotManager;

    public static final String KEY_MSG = "msg";

    public static final int MSG_RESPONSE = 0;
    public static final int MSG_PING_RESPONSE = 1;
    public static final int MSG_HOTSPOT_RESPONSE = 2;
    public static final int MSG_CONNECTIVITY_UPDATE = 3;

    public static final int HOTSPOT_RESPONSE_SUCCESS = 1;
    public static final int HOTSPOT_RESPONSE_FAILED = 2;

    // Default ports for control API and video stream.
    private static final String API_PORT = "8081";
    private static final String STREAM_PORT = "8080";
    private static final String STREAM_PATH = "/stream/video.mjpeg";

    private static final boolean FAKE_MODE = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        // Create settings store.
        mSettings = new Settings(this);

        // Set up servo messaging.
        mClient = new RestClient(mSettings.getHost(), API_PORT, FAKE_MODE);

        // Create the network handler to receive callback from the network.
        mNetworkHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                processNetworkMessage(msg);
            }
        };

        // Create WiFi Hotspot Manager
        mHotspotManager = new HotspotManager(this, mNetworkHandler);

        // Start the network sender thread.
        mNetworkThread = new SenderThread(mClient, mNetworkHandler);
        mNetworkThread.start();

    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public void setConnected(boolean connected) {
        mIsConnected = connected;
    }

    public String getStreamUrl() {
        return "http://" + mSettings.getHost() + ":" + STREAM_PORT + STREAM_PATH;
    }

    /**
     * Incoming messages from non-UI are returned to the UI thread here from NetworkThread and
     * HotspotManager.
     */
    private void processNetworkMessage(Message msg) {
        Log.d("adam", "Received network msg: " + msg);

        if (mNetworkCallbackActivity == null) {
            Log.d(TAG, "No callback activity, dropping msg: " + msg);
            return;
        }

        if (msg.what == MSG_RESPONSE) {
            // These are the majority of messages returned during normal usage.
            // TODO: Trap errors and go into connectivity state.
            return;
        }

        if (msg.what == MSG_RESPONSE) {
            Log.d(TAG, "Cmd result: " + msg.arg1);
            mNetworkCallbackActivity.cmdResult(msg.arg1 == 200);
        } else if (msg.what == MSG_PING_RESPONSE) {
            Log.d(TAG, "Ping result: " + msg.arg1);
            mNetworkCallbackActivity.pingResult(msg.arg1 == 200);
        } else if (msg.what == MSG_HOTSPOT_RESPONSE) {
            // Hotspot response.
            Log.d(TAG, "Hotspot result: " + msg.arg1);
            String str = msg.getData().getString(KEY_MSG);
            mNetworkCallbackActivity.hotspotResult(msg.arg1 == HOTSPOT_RESPONSE_SUCCESS, str);
        }
    }

    /**
     * Returns the last run or saved script from the BlocklyActivity.
     * Temporary hack to get access to BlocklyScript in main activity.
     * @return a string of javascript ready to be injected into the webview.
     */
    public void setLastRunScript(String script) {
        mLastRunScript = script;
    }

    /**
     * Returns the last run or saved script from the BlocklyActivity.
     * Temporary hack to get access to BlocklyScript in main activity.
     * @return a string of javascript ready to be injected into the webview.
     */
    public String getLastRunScript() {
        return mLastRunScript;
    }

    /**
     * Fires off a network call to send a command to the server, the callback when that command
     * completes goes back to the networkCallbackActivity.
     *
     * TODO: Try using Futures as it might be cleaner.*
     */
    public void sendCommand(Command cmd) {
        mNetworkThread.sendCommand(cmd, 0);
    }

    /**
     * Fires off a delayed network command to the server.
     */
     public void sendCommand(Command cmd, int secondsDelay) {
         mNetworkThread.sendCommand(cmd, secondsDelay);
     }


    public void setHost(String hostIP) {
        mSettings.setHost(hostIP);
        mClient.setHost(hostIP, API_PORT);
    }

    public String getHost() {
        return mSettings.getHost();
    }

    public void sendPing() {
        mNetworkThread.ping();
    }

    public void connectHotspot() {
        mHotspotManager.connectToHotSpot(mSettings.getHotspotSSID(), mSettings.getHotspotPSWD());
    }

    public void cancelHotspotConnection() {
        mHotspotManager.cancel();
    }

    public static PiControlApplication getInstance() {
        return mInstance;
    }

    public void setNetworkCallbackActivity(NetworkCallbackInterface i) {
        mNetworkCallbackActivity = i;
    }

    /**
     * Recevies all the network callbacks from the network thread. Implemented by any activity
     * that wants to use the network. The current activity must call
     * {@link #setNetworkCallbackActivity(NetworkCallbackInterface)} in its onStart method to
     * ensure that it receives the network callbacks.
     */
    public static interface NetworkCallbackInterface {

        /**
         * The result of a ping request, used when connecting.
         * @param success
         */
        void pingResult(boolean success);

        /**
         * The result of the hotspot set up, used when connecting with hotspot.
         * @param success
         */
        void hotspotResult(boolean success, String msg);

        /**
         * The result of a command request, used whenever sending commands to the robot.
         * @param success
         */
        void cmdResult(boolean success);
    }
}
