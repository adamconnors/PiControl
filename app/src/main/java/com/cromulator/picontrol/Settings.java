package com.cromulator.picontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.InvalidPropertiesFormatException;

/**
 * A place for all the shared settings associated with this application.
 */
public class Settings {

    // Shared Preferences keys and toolbox values.
    // Prefer to connect via hotspot or directly via IP
    private static final String KEY_CONNECT_PREFER_HOTSPOT = "CONNECT_PREF_HOTSPOT";
    private static final String KEY_IP = "IP";

    // The hotspot details when connecting via hotspot
    private static final String KEY_HOTSPOT_SSID = "HOTSPOT_SSID";
    private static final String KEY_HOTSPOT_PSWD = "HOTSPOT_PASSWD";

    private static final String PREFS_NAME = "com.cromulator.picontrol";
    private SharedPreferences mPrefs;

    public Settings(Context ctx) {

        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (mPrefs.contains(KEY_IP)) {
            // Preferences already exist, leave them alone.
            return;
        }

        SharedPreferences.Editor editor = mPrefs.edit();

        // The hardcoded details configured in hostapd.conf on the Pi
        editor.putString(KEY_IP, "172.24.1.1");
        editor.putString(KEY_HOTSPOT_SSID, "GUARDBOT-AP");
        editor.putString(KEY_HOTSPOT_PSWD, "raspberry");
        editor.apply();
    }

    public void setHost(String ip) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(KEY_IP, ip);
        editor.apply();
    }

    public String getHost() {
        return mPrefs.getString(KEY_IP, "");
    }

    public String getHotspotSSID() {
        return mPrefs.getString(KEY_HOTSPOT_SSID, "");
    }

    public String getHotspotPSWD() {
        return mPrefs.getString(KEY_HOTSPOT_PSWD, "");
    }
}
