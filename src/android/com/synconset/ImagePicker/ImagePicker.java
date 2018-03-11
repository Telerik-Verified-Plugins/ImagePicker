/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import java.io.ByteArrayOutputStream;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import android.content.Context;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import android.support.annotation.NonNull;
import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;

import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.api.widget.Widget;

public class ImagePicker extends CordovaPlugin {

    private final String TAG = "ImagePicker";
    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final int PERMISSION_REQUEST_CODE = 100;

    private CallbackContext callbackContext;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        final Context context = cordova.getActivity();

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);
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

            final int finalQuality = quality;
            final int finalOutputType = outputType;
            Log.d(TAG, "finalQuality: " + finalQuality);
            Log.d(TAG, "finalOutputType: " + finalOutputType);
            if (hasReadPermission()) {
                int color = Color.parseColor("#42A6AC");
                Widget widget = Widget.newDarkBuilder(context)
                        .title("Seleccionar fotos")
                        .statusBarColor(Color.BLACK)
                        .toolBarColor(color)
                        .navigationBarColor(Color.BLACK)
                        .mediaItemCheckSelector(color, color)
                        .bucketItemCheckSelector(color, color)
                        .buttonStyle(
                                Widget.ButtonStyle.newLightBuilder(context)
                                        .setButtonSelector(Color.WHITE, Color.WHITE)
                                        .build()
                        ).build();

                Album.image(context) // Image selection.
                        .multipleChoice()
                        .widget(widget)
                        .requestCode(0)
                        .camera(false)
                        .columnCount(3)
                        .selectCount(max)
                        .filterSize(null) // Filter the file size.
                        .filterMimeType(null) // Filter file format.
                        .afterFilterVisibility(false) // Show the filtered files, but they are not available.
                        .onResult(new Action<ArrayList<AlbumFile>>() {
                            @Override
                            public void onAction(int requestCode, @NonNull final ArrayList<AlbumFile> result) {
                                new ImagesTask(callbackContext, context).execute(result);
                            }
                        }).onCancel(new Action<String>() {
                            @Override
                            public void onAction(int requestCode, @NonNull String result) {
                                Log.d("App", "Error");
                                callbackContext.error("No images selected");
                            }
                        }).start();
            } else {
                requestReadPermission();
            }
            return true;
        }
        return false;
    }

    private String getBase64OfImage(Bitmap bm, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private class ImagesTask extends AsyncTask<List<AlbumFile>, Integer, List<String>> {

        private final String TAG = "MyTask";
        private final CallbackContext callbackContext;
        private final Context context;
        private ProgressDialog loading;

        public ImagesTask(CallbackContext callbackContext, Context context) {
            this.callbackContext = callbackContext;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            loading = new ProgressDialog(context);
            loading.setMax(100);
            loading.setMessage("Its loading....");
            loading.setTitle("ProgressDialog bar example");
            loading.show();
        }

        @Override
        protected void onPostExecute(List<String> images) {
            loading.dismiss();
            JSONArray res = new JSONArray(images);
            callbackContext.success(res);
        }

        @Override
        protected List<String> doInBackground(List<AlbumFile>... params) {
            List<String> images = new ArrayList<String>();
            Bitmap bm;
            for (AlbumFile af : params[0]) {
                if(1 > 0) {//finalOutputType
                    try {
                        bm = BitmapFactory.decodeFile(af.getPath());
                        images.add(getBase64OfImage(bm, 30));//finalQuality
                    } catch(Exception ex) {

                    }
                } else {
                    images.add(af.getPath());
                }
            }
            return images;
        }
    }

    @SuppressLint("InlinedApi")
    private boolean hasReadPermission() {
        return Build.VERSION.SDK_INT < 23 ||
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @SuppressLint("InlinedApi")
    private void requestReadPermission() {
        if (!hasReadPermission()) {
            ActivityCompat.requestPermissions(
                this.cordova.getActivity(),
                new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
        // This method executes async and we seem to have no known way to receive the result
        // (that's why these methods were later added to Cordova), so simply returning ok now.
        callbackContext.success();
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }
}
