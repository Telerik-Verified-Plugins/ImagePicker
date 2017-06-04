/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final int PERMISSION_REQUEST_CODE = 100;

    private CallbackContext callbackContext;

    /**
     * Temporarily store the imagePickerIntent when asking for read permission
     */
    private Intent startAfterPermissionResult = null;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            if (!hasReadPermission())
                requestReadPermission(null);
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);
            final Intent imagePickerIntent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
            int max = 20;
            int desiredWidth = 0;
            int desiredHeight = 0;
            int quality = 100;
            int outputType = 0;
            if (params.has("maximumImagesCount")) {
                max = params.getInt("maximumImagesCount");
            }
            if (params.has("width")) {
                desiredWidth = params.getInt("width");
            }
            if (params.has("height")) {
                desiredHeight = params.getInt("height");
            }
            if (params.has("quality")) {
                quality = params.getInt("quality");
            }
            if (params.has("outputType")) {
                outputType = params.getInt("outputType");
            }

            imagePickerIntent.putExtra("MAX_IMAGES", max);
            imagePickerIntent.putExtra("WIDTH", desiredWidth);
            imagePickerIntent.putExtra("HEIGHT", desiredHeight);
            imagePickerIntent.putExtra("QUALITY", quality);
            imagePickerIntent.putExtra("OUTPUT_TYPE", outputType);

            if (hasReadPermission()) {
                cordova.startActivityForResult(this, imagePickerIntent, 0);
            } else {
                requestReadPermission(imagePickerIntent);
            }
            return true;
        }
        return false;
    }

    /**
     * Return whether we have READ_EXTERNAL_STORAGE
     */
    @SuppressLint("InlinedApi")
    private boolean hasReadPermission() {
        return Build.VERSION.SDK_INT < 23 || this.cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    /**
     * Request READ_EXTERNAL_STORAGE permission
     *
     * @param intent: Launch this after receiving the permission
     */
    @SuppressLint("InlinedApi")
    private void requestReadPermission(Intent intent) {
        startAfterPermissionResult = intent;
        this.cordova.requestPermission(
            this,
            PERMISSION_REQUEST_CODE,
            Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
            JSONArray res = new JSONArray(fileNames);
            callbackContext.success(res);

        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            callbackContext.error(error);

        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            callbackContext.success(res);

        } else {
            callbackContext.error("Invalid activity result");
        }
    }

    public void onRequestPermissionResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (startAfterPermissionResult != null) {
                    cordova.startActivityForResult(this, startAfterPermissionResult, 0);
                    startAfterPermissionResult = null;
                } else
                    callbackContext.success();
            } else {
                callbackContext.error("Permission denied");
            }
        } else {
            callbackContext.error("Invalid permission result");
        }
    }
}
