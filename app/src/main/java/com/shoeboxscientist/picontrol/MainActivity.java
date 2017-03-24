package com.shoeboxscientist.picontrol;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.andretietz.android.controller.DirectionView;

public class MainActivity extends Activity {

    private static final String TAG = "PiControl.Main";

    private TextView debugLog;
    private WebViewManager webViewMgr;

    private DirectionView cameraCtrl;
    private DirectionView motorCtrl;
    private View openGrabbyHand;
    private View closeGrabbyHand;

    private PiControlApplication.NetworkCallbackInterface mCallbacks = new NetworkCallbacks();

    private BlocklyManager mBlocklyManager;
    private JoystickController mJoystickController;

    private ConnectionView mConnectionView;

    private ProgressDialog mConnectingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View baseView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(baseView);

        mConnectingDialog = new ProgressDialog(this);

        mConnectingDialog.setTitle("Connecting");
        mConnectingDialog.setMessage("Test");
        mConnectingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mConnectingDialog.setIndeterminate(true);
        mConnectingDialog.setButton(ProgressDialog.BUTTON_NEUTRAL, "Skip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.skipHotspotConnection();
            }
        });
        mConnectingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.d(TAG, "Cancel connection dialog");
                MainActivity.this.cancelConnection();
                setImmersiveMode();
            }
        });

        // Get hold of all the views.
        debugLog = (TextView) findViewById(R.id.debuglog);
        motorCtrl = (DirectionView) findViewById(R.id.motorCtrl);
        cameraCtrl = (DirectionView) findViewById(R.id.cameraCtrl);
        mConnectionView = new ConnectionView(baseView);

        // WebView Mgr for calls into & out of webview.
        webViewMgr = new WebViewManager(this, (WebView) findViewById(R.id.webview));
        webViewMgr.init();

        // Joystick controller for joystick listeners;
        mJoystickController = new JoystickController();

        // Blockly controller for interaction with Blockly Editor.
        mBlocklyManager = new BlocklyManager(this, getWindow(), webViewMgr);

        // Grabby hand controls
        openGrabbyHand = findViewById(R.id.openHand);
        closeGrabbyHand = findViewById(R.id.closeHand);
        openGrabbyHand.setOnTouchListener(mJoystickController.getGrabbyHandListener(true));
        closeGrabbyHand.setOnTouchListener(mJoystickController.getGrabbyHandListener(false));

        // Set up camera position.
        JoyStickPosition cameraPosition = new JoyStickPosition(cameraCtrl,
                mJoystickController.getCameraListener());

        // Set up motor position.
        JoyStickPosition motorPower = new JoyStickPosition(motorCtrl,
                mJoystickController.getMotorListener());

        // Everything is set up, the API ping to trigger connection and loading of stream
        // happens in onStart.
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity.onStart");

        // I added this because the WiFi scan wasn't working. Not sure if it's really needed
        // anymore.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION }, 1);
        }

        setup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity.onResume");
        setup();
    }

    private void setup() {
        setImmersiveMode();

        Log.d(TAG, "Setting MainActivity as network listener.");
        PiControlApplication.getInstance().setNetworkCallbackActivity(mCallbacks);

        // Assume we are disconnected on start & show connection dialogue.
        // I used to ping the server onStart but the timeout was so long it seemed to screw
        // things up.
        if (PiControlApplication.getInstance().isConnected() == false) {
            mConnectionView.show("Disconnected.");
        }
    }

    private void setImmersiveMode() {
        final View root = findViewById(R.id.root);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // Incoming request from connect dialog.
    private void connectToHotspot(String address) {
        getApp().setHost(address);
        getApp().connectHotspot();
    }

    private void skipHotspotConnection() {
        Log.d(TAG, "User skipped hotspot, attempting to connect directly.");
        getApp().cancelHotspotConnection();
        getApp().sendPing();
    }

    private void cancelConnection() {
        Log.d(TAG, "User cancelled connection.");
        getApp().cancelHotspotConnection();
    }

    public PiControlApplication getApp() {
        return PiControlApplication.getInstance();
    }


    /** -------------------------------------------------------------------------------------- **/

    /**
     * Receives callbacks from the network thread in response to UI activities.
     */
    private class NetworkCallbacks implements PiControlApplication.NetworkCallbackInterface {

        @Override
        public void pingResult(boolean success) {
            Log.d(TAG, "Ping result: " + success);
            if (success) {
                mConnectionView.hide();
                hideDialog();
                PiControlApplication.getInstance().setConnected(true);
                webViewMgr.loadStream(getApp().getStreamUrl());
            } else {
                PiControlApplication.getInstance().setConnected(false);
                mConnectionView.show("Ping failed, reconnect.");
                webViewMgr.setStreamFailed();
            }
        }

        @Override
        public void hotspotResult(boolean success, String msg) {
            if (success) {
                // Hotspot setup correctly, ping server to see if we're really connected.
                Log.d(TAG, "Hotspot connected, sending ping.");
                mConnectingDialog.setMessage("Sending ping...");
                getApp().sendPing();
            } else {
                // Hotspot setup failed.
                PiControlApplication.getInstance().setConnected(false);
                hideDialog();
                webViewMgr.setStreamFailed();
            }
        }

        private void hideDialog() {
            mConnectingDialog.hide();
            setImmersiveMode();
        }

        @Override
        public void cmdResult(boolean success) {
            if (!success) {
                PiControlApplication.getInstance().setConnected(false);
                mConnectionView.show("Uh, oh, request failed. Disconnected.");
                webViewMgr.setStreamFailed();
            }
        }
    }

    /** ------------------------------------------------------------------------------------- */

    private class ConnectionView {

        private View mConnectionDialog;
        private TextView mConnectivityText;
        private EditText mIPAddress;
        private View mConnectionSpinner;
        private Button mConnectButton;

        public ConnectionView(View v) {
            mConnectionDialog = v.findViewById(R.id.connectionDialog);
            mConnectivityText = (TextView) v.findViewById(R.id.connectivityText);
            mIPAddress = (EditText) v.findViewById(R.id.ip);
            mConnectionSpinner = v.findViewById(R.id.connectionSpinner);
            mConnectButton = (Button) v.findViewById(R.id.connectButton);

            mIPAddress.setText(getApp().getHost());

            mConnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Update preferences and attempt to connect the hotspot.
                    // Callback will come in the callbacks class.
                    mConnectingDialog.setMessage("Waiting for hotspot");
                    mConnectingDialog.show();
                    connectToHotspot(mIPAddress.getText().toString());
                }
            });

        }

        public void show(String msg) {
            mConnectionDialog.setVisibility(View.VISIBLE);
            mConnectivityText.setText(msg);
            mConnectionSpinner.setVisibility(View.GONE);
            mConnectButton.setVisibility(View.VISIBLE);
        }

        public void hide() {
            mConnectionDialog.setVisibility(View.GONE);
        }
    }

}
