package com.cromulator.picontrol;


import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Simple interface for calling the REST API to control Pi using WebIOClient.
 * This is deprecated as I switched to using my owner Rest client instead which is called
 * by RestClient.
 *
 * curl 'http://192.168.1.80:8000/devices/pwm0/pwm/0/angle/-34' -X POST -H 'Authorization: Basic d2ViaW9waTpyYXNwYmVycnk='
 *
 **/
public class WebIOClient {

    private static final String TAG = "WebIOClient";

    private String username;
    private String password;
    private final String urlBase;
    private boolean fakeMode;

    private long lastCallMillis = -1;
    private static long CALL_THRESHOLD_MILLIS = 300;

    private final String PWM_PATH = "/devices/pwm0/pwm/$/angle/";

    public WebIOClient(String host, String port, boolean fakeMode) {
        this.urlBase = "http://" + host + ":" + port;
        this.fakeMode = fakeMode;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public boolean setPWMAngle(float angleX, float angleY) {

        if (System.currentTimeMillis() - lastCallMillis < CALL_THRESHOLD_MILLIS) {
            Log.d(TAG, "Too many calls, dropping.");
            return false;
        }

        boolean success1 = call(0, angleX);
        boolean success2 = call(1, angleY);
        lastCallMillis = System.currentTimeMillis();

        return success1 && success2;

    }

    private boolean call(int channel, float angle) {
        String path = PWM_PATH.replaceAll("\\$", Integer.toString(channel))
                + Float.toString(angle);

        if (fakeMode) {
            Log.d(TAG, "Fake mode, not really sending request: " + getUrl(path));
            return true;
        }

        try {
            HttpResponse resp = makeAuthenticatedRequest(path);
            if (resp.code == 200) {
                return true;
            } else {
                Log.w(TAG, "Response to " + getUrl(path) + " was: " + resp.code);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send request to: " + getUrl(path), e);
            return false;
        }

    }

    private HttpResponse makeAuthenticatedRequest(String path) throws Exception {
        BufferedReader in = null;
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    new URL(getUrl(path)).openConnection();
            String encoded = Base64.encodeToString((username + ":" + password).getBytes(),
                    Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + encoded);
            connection.setRequestMethod("POST");

            int responseCode = connection.getResponseCode();

            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            return new HttpResponse(responseCode, response.toString());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private String getUrl(String path) {
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
