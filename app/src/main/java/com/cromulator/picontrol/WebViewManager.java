package com.cromulator.picontrol;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Handles the WebView functions, translation into / out of javascript.
 */
public class WebViewManager {

    private WebView mWebView;
    private Context mCtx;
    private Handler mUiThread;
    private boolean mInitializing = false;

    private RobotEventListener mRobotEventListener;
    private WebViewEventListener mWebViewEventListener;

    private static final String TAG = "PiControl.WebView";

    public WebViewManager(Context context, WebView webView) {
        mCtx = context;
        mWebView = webView;

        mUiThread = new Handler();

        // Set up the web view client.
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // TODO: Respond to error state -- e.g. show disconnected graphic.
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "loaded: " + url);
                onLoad();
            }
        });

        webView.addJavascriptInterface(new WebViewCallbacks(), "App");
    }

    private void onLoad() {
        // Only care about page load events in response to initializing the view.
        if (mInitializing) {
            mInitializing = false;
            callJavaScript("init();");
            if (mWebViewEventListener != null) {
                mWebViewEventListener.onWebViewInitialised();
            }
        }
    }

    private void callJavaScript(final String js) {
        Log.d(TAG, "Calling fn: javascript:" + js);

        mUiThread.post(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript:" + js);
            }
        });
    }

    public void init() {
        mInitializing = true;
        mWebView.loadUrl("file:///android_asset/webview/main.html");
    }

    /**
     * Causes the javascript macro to be executed in the webView.
     */
    public void executeMacroScript(String script) {
        Log.d(TAG, "Executing: " + script);

        // Get rid of any previous commands in the executor.
        mRobotEventListener.reset();

        if (script == null) {
            return;
        }

        if (!script.endsWith(";")) {
            script += ";";
        }

        callJavaScript(script);
    }

    public void setRobotEventListener(RobotEventListener listener) {
        mRobotEventListener = listener;
    }

    public void setWebViewEventListener(WebViewEventListener listener) {
        mWebViewEventListener = listener;
    }

    /**
     * Shows the static img for a failed stream so we know we're not connected.
     */
    public void setStreamFailed() {
        Log.d(TAG, "Setting stream failed.");
        callJavaScript("setStreamLocation('tvstatic.gif');");
    }

    public void loadStream(String streamUrl) {
        callJavaScript("setStreamLocation('" + streamUrl + "');");
    }

    /**
     * Webview interface, receives callback from javascript executing in the WebView.
     */
    private class WebViewCallbacks {
        @android.webkit.JavascriptInterface
        public void forward(String power) { mRobotEventListener.forward(power); }

        @android.webkit.JavascriptInterface
        public void backward(String power) { mRobotEventListener.backward(power); }

        @android.webkit.JavascriptInterface
        public void rotateLeft() { mRobotEventListener.rotateLeft(); }

        @android.webkit.JavascriptInterface
        public void rotateRight() { mRobotEventListener.rotateRight(); }

        @android.webkit.JavascriptInterface
        public void camera(String position) { mRobotEventListener.camera(position); }

        @android.webkit.JavascriptInterface
        public void wait(String millis) { mRobotEventListener.wait(millis); }

        @android.webkit.JavascriptInterface
        public void print(String str) { mRobotEventListener.print(str); }

        @android.webkit.JavascriptInterface
        public void execute() { mRobotEventListener.execute(); }
    }

    /**
     * Mgr callback, passes callbacks from the executing javascript to the specified listener
     * which will send commands to the robot.
     */
    public static interface RobotEventListener {
        void forward(String power);
        void backward(String power);
        void rotateLeft();
        void rotateRight();
        void camera(String position);
        void wait(String seconds);
        void execute();
        void reset();
        void print(String string);
    }

    /**
     * Callback to notify the parent when the webview is fully loaded.
     */
    public static interface WebViewEventListener {
        void onWebViewInitialised();
    }
}
