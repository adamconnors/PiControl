package com.shoeboxscientist.picontrol;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Simple interface for calling the REST API to control Pi.
 *
 * http://<ip>:8080/cmd?c=0,0&m=0,0
 * c = camera
 * m = motor
 **/
public class RestClient {

    private static final String TAG = "PiControl.RestClient";

    private String username;
    private String password;
    private String host;
    private String port;
    private boolean fakeMode;

    public RestClient(String host, String port, boolean fakeMode) {
        this.host = host;
        this.port = port;
        this.fakeMode = fakeMode;
    }

    public void setHost(String host, String port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Send a request to the server.
     * @param cmd The command used to construct the request.
     * @return true Http response
     */
    public int sendCommand(Command cmd) {

        // TODO: Use proper URI build and allow pan/tilt or left/right to be set separately.
        StringBuilder url = new StringBuilder("/cmd?");
        boolean needsSeparator = false;
        if (cmd.hasCameraPan()) {
            url.append("c=").append(cmd.getCameraPan()).append(",").append(cmd.getCameraTilt());
            needsSeparator = true;
        }

        if (cmd.hasMotorLeft()) {
            if (needsSeparator) { url.append("&"); }
            url.append("m=").append(cmd.getMotorLeft()).append(",").append(cmd.getMotorRight());
            needsSeparator = true;
        }

        if (cmd.hasGrabbyHand()) {
            if (needsSeparator) { url.append("&"); }
            switch (cmd.getGrabbyHand()) {
                case Command.GRABBY_HAND_STOP:
                    url.append("h=").append("0,1");
                    break;
                case Command.GRABBY_HAND_OPEN:
                    url.append("h=").append("1,1");
                    break;
                case Command.GRABBY_HAND_CLOSE:
                    url.append("h=").append("1,2");
                    break;
            }
        }

        if (fakeMode) {
            Log.d(TAG, "Fake mode, not really sending request: " + getUrl(url.toString()));
            return 200;
        }

        Log.d(TAG, "Sending: " + url.toString());
        try {
            HttpResponse resp = makeRequest(url.toString());
            return resp.code;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send request to: " + getUrl(url.toString()), e);
            return -1;
        }
    }

    private HttpResponse makeRequest(String path) throws Exception {
        BufferedReader rd = null;
        try {
            // Make get request.
            Log.d(TAG, "Making request: " + getUrl(path));
            URL url = new URL(getUrl(path));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int code = conn.getResponseCode();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder rtn = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                rtn.append(line);
            }
            rd.close();
            Log.d(TAG, "Got response: " + code);
            return new HttpResponse(code, rtn.toString());
        } finally {
            if (rd != null) {
                rd.close();
            }
        }
    }

    private String getUrl(String path) {
        String urlBase = "http://" + host + ":" + port;
        return urlBase + path;
    }

    private static class HttpResponse {
        public final int code;
        public final String content;

        public HttpResponse(int code, String content) {
            this.code = code;
            this.content = content;
        }
    }
}
