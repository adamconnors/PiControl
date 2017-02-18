package com.cromulator.picontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Set;

/**
 * Handles checking for & finding WiFi hotspot
 */
public class HotspotManager {

    private final Context ctx;
    private Handler mNetworkHandler;
    private NetworkReceiver mNetworkReceiver;

    WifiManager mWifiManager;

    private static final String TAG = "PiControl.Hotspot";

    private static enum State {
        DISCONNECTED, WAITING_FOR_HOTSPOT, CONNECTING, CONNECTED;
    }

    private State mState;
    private String mSSID;
    private String mPWD;


    public HotspotManager(Context ctx, Handler networkHandler) {
        this.ctx = ctx;
        mState = State.DISCONNECTED;
        mNetworkHandler = networkHandler;
        mNetworkReceiver = new NetworkReceiver();
        mWifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }

    public String getCurrentSSID() {
        ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = mgr.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return null;
        }

        if (activeNetworkInfo.isConnected()) {
            WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && connectionInfo.getSSID() != null) {
                return connectionInfo.getSSID();
            }
        }
        return null;
    }

    public void cancel() {
        mState = State.DISCONNECTED;
        try {
            Log.d(TAG, "Unregistering wifi receiver.");
            ctx.unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver wasn't registered because we were already connected.
        }
    }

    public void connectToHotSpot(final String ssid, String pswd) {
        mSSID = "\"" + ssid + "\"";
        mPWD = pswd;

        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        String currentSSID = connectionInfo.getSSID();
        Log.d(TAG, "Looking for " + mSSID + " current SSID = " + currentSSID);
        if (mSSID.equals(currentSSID)) {
            Log.d(TAG, "Already connected to " + ssid);
            sendConnectedMessage(true, "Already Connected.");
            return;
        }

        mState = State.WAITING_FOR_HOTSPOT;
        waitForHotspot();
    }

    private void waitForHotspot() {
        cancel();
        Log.d(TAG, "Waiting for hotspot.");
        ctx.registerReceiver(mNetworkReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }

    private void connect() {

        String currentSSID = getCurrentSSID();
        Log.d(TAG, "Connecting to: " + mSSID + " current ssid=" + currentSSID);

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = mSSID;
        conf.preSharedKey = "\""+ mPWD +"\"";

        // Register for a connectivity change.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        ctx.registerReceiver(mNetworkReceiver, intentFilter);

        int netId = mWifiManager.addNetwork(conf);

        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.disconnect();
        } else {
            mWifiManager.setWifiEnabled(true);
        }
        mWifiManager.enableNetwork(netId, true);
        mWifiManager.reconnect();
    }

    private void sendConnectedMessage(boolean success, String str) {

        try {
            ctx.unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver wasn't registered because we were already connected.
        }

        Message msg = mNetworkHandler.obtainMessage(
                PiControlApplication.MSG_HOTSPOT_RESPONSE,
                (success) ? PiControlApplication.HOTSPOT_RESPONSE_SUCCESS
                        : PiControlApplication.HOTSPOT_RESPONSE_FAILED, 0);
        Bundle data = new Bundle();
        data.putString(PiControlApplication.KEY_MSG, str);
        msg.setData(data);
        mNetworkHandler.sendMessage(msg);
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                List<ScanResult> wifiPoints = mWifiManager.getScanResults();
                Log.d(TAG, "WiFi scan results available: " + wifiPoints.size());
                for (ScanResult result : wifiPoints) {
                    Log.d(TAG, "Wifi: " + result.SSID);
                    if (mSSID.equals("\"" + result.SSID + "\"")) {
                        Log.d(TAG, "Found Wifi, trying to connect to it.");
                        connect();
                        return;
                    }
                }

                Log.d(TAG, "Couldn't find network, starting another scan");
                waitForHotspot();
                return;
            } else if (intent.getAction() == ConnectivityManager.CONNECTIVITY_ACTION) {
                String currentSSID = getCurrentSSID();
                Log.d(TAG, "Current " + currentSSID + " targetSSID=" + mSSID);
                if (currentSSID != null && currentSSID.equals(mSSID)) {
                    sendConnectedMessage(true, "Connected!");
                    Log.d(TAG, "Current equals target, we're connected.");
                }
            } else {
                Log.d(TAG, "Ignoring wifi action: " + intent.getAction());
                return;
            }


//            ConnectivityManager conn = (ConnectivityManager) context
//                    .getSystemService(Context.CONNECTIVITY_SERVICE);
//
//            NetworkInfo networkInfo = conn.getActiveNetworkInfo();
//
//            // Checks the user prefs and the network connection. Based on the result, decides whether
//            // to refresh the display or keep the current display.
//            // If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
//            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
//                    && getCurrentSSID() != null && getCurrentSSID().equals(mSSID)) {
//                sendConnectedMessage(true);
//            } else {
//                sendConnectedMessage(false);
//            }
        }
    }
}
