/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 *
 * Routes selection through the Android system photo picker
 * (androidx PickVisualMedia / PickMultipleVisualMedia), which requires no
 * runtime permission. Selected content:// URIs are copied into the app's
 * private cache directory and returned as file:// paths, preserving the
 * historical getPictures() contract (array of file URIs, multi-select).
 */
package com.synconset;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final int PICK_IMAGES_REQUEST = 0;

    private CallbackContext callbackContext;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            // The system photo picker needs no permission; keep the API surface intact.
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            // No-op: there is nothing to request for the system photo picker.
            callbackContext.success();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);
            int max = params.has("maximumImagesCount") ? params.getInt("maximumImagesCount") : 20;
            // width / height / quality / outputType are accepted for backwards
            // compatibility but unused: the app only consumes file URIs and does
            // no resizing.

            // Sweep stale temp files from previous selections before starting a new one.
            // The consumer has already read the prior batch by the time the picker reopens,
            // so deleting here avoids unbounded cache growth without racing the live result.
            sweepStaleCacheFiles();

            final Intent intent = buildPickIntent(max);
            cordova.startActivityForResult(this, intent, PICK_IMAGES_REQUEST);
            return true;
        }
        return false;
    }

    /**
     * Builds the system photo-picker intent. PickVisualMedia auto-selects the best
     * available mechanism: API 33+ system picker, the Play-services backported grid
     * on many API 24-32 devices, and SAF only where neither is available.
     */
    private Intent buildPickIntent(int max) {
        final Context context = cordova.getActivity();
        final PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build();

        final ActivityResultContract<PickVisualMediaRequest, ?> contract = (max <= 1)
            ? new ActivityResultContracts.PickVisualMedia()
            : new ActivityResultContracts.PickMultipleVisualMedia(max);

        return contract.createIntent(context, request);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_IMAGES_REQUEST) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            // Cancelled / nothing selected: return an empty array, as the old plugin did.
            callbackContext.success(new JSONArray());
            return;
        }

        final List<Uri> uris = extractUris(data);
        if (uris.isEmpty()) {
            callbackContext.success(new JSONArray());
            return;
        }

        // Copy off the UI thread: a cloud-backed selection may need to download first.
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                final JSONArray result = new JSONArray();
                try {
                    for (Uri uri : uris) {
                        result.put("file://" + copyToCache(uri));
                    }
                } catch (Exception e) {
                    callbackContext.error("Failed to read selected image: " + e.getMessage());
                    return;
                }
                callbackContext.success(result);
            }
        });
    }

    /** Reads the selected URIs from the picker result (clip data for multi-select, else getData). */
    private List<Uri> extractUris(Intent data) {
        final List<Uri> uris = new ArrayList<Uri>();
        if (data == null) {
            return uris;
        }
        final ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                final Uri u = clip.getItemAt(i).getUri();
                if (u != null) {
                    uris.add(u);
                }
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        return uris;
    }

    /**
     * Deletes leftover ipick_* temp files from earlier selections. Safe because the
     * file:// paths returned by a prior getPictures() call have already been consumed
     * by the time the user reopens the picker.
     */
    private void sweepStaleCacheFiles() {
        final File cacheDir = cordova.getActivity().getCacheDir();
        if (cacheDir == null) {
            return;
        }
        final File[] files = cacheDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.getName().startsWith("ipick_")) {
                f.delete();
            }
        }
    }

    /** Copies a content:// URI into the app-private cache dir and returns the absolute file path. */
    private String copyToCache(Uri uri) throws Exception {
        final Context context = cordova.getActivity();
        final ContentResolver resolver = context.getContentResolver();
        final File out = File.createTempFile("ipick_", extensionFor(resolver, uri), context.getCacheDir());

        InputStream in = null;
        OutputStream os = null;
        try {
            in = resolver.openInputStream(uri);
            if (in == null) {
                throw new Exception("openInputStream returned null for " + uri);
            }
            os = new FileOutputStream(out);
            final byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) { }
            }
            if (os != null) {
                try { os.close(); } catch (Exception ignored) { }
            }
        }
        return out.getAbsolutePath();
    }

    private String extensionFor(ContentResolver resolver, Uri uri) {
        final String type = resolver.getType(uri);
        final String ext = (type != null)
            ? MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            : null;
        return (ext != null) ? "." + ext : ".jpg";
    }

    /**
     * Choosing a picture launches another Activity, so we restore the callback if the
     * CordovaActivity is killed by the OS before we get the launched Activity's result.
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }
}
