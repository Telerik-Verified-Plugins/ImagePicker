cordova-imagePicker
===================

Cordova Plugin For Multiple Image Selection - implemented for iOS and Android 4.0 and above.

## Installing the plugin

The plugin conforms to the Cordova plugin specification, it can be installed
using the Cordova / Phonegap command line interface.

    # without desc
    phonegap plugin add https://github.com/Telerik-Verified-Plugins/ImagePicker.git
    cordova plugin add https://github.com/Telerik-Verified-Plugins/ImagePicker.git
    
    # with desc
    phonegap plugin add https://github.com/Telerik-Verified-Plugins/ImagePicker.git --variable PHOTO_LIBRARY_USAGE_DESCRIPTION="your usage message"

    cordova plugin add https://github.com/Telerik-Verified-Plugins/ImagePicker.git --variable PHOTO_LIBRARY_USAGE_DESCRIPTION="your usage message"


## Using the plugin

The plugin creates the object `window.imagePicker` with the method `getPictures(success, fail, options)`

Example - Get Full Size Images (all default options):
```javascript
window.imagePicker.getPictures(
    function(results) {
        for (var i = 0; i < results.length; i++) {
            console.log('Image URI: ' + results[i]);
        }
    }, function (error) {
        console.log('Error: ' + error);
    }
);
```

Example - Get at most 10 images scaled to width of 800:
```javascript
window.imagePicker.getPictures(
    function(results) {
        for (var i = 0; i < results.length; i++) {
            console.log('Image URI: ' + results[i]);
        }
    }, function (error) {
        console.log('Error: ' + error);
    }, {
        maximumImagesCount: 10,
        width: 800
    }
);
```

### Options

> **Android:** since 2.3.6 the Android side routes through the system photo picker
> (`PickVisualMedia`), which returns the original image and needs no runtime permission.
> The `width`, `height`, `quality` and `outputType` options below are still accepted for
> backwards compatibility but are **ignored on Android**: images are returned at original
> resolution and always as `file://` URIs (the `BASE64_STRING` output is no longer produced).
> They continue to apply on iOS.

    options = {
        // Android only. Max images to be selected, defaults to 15. If this is set to 1, upon
        // selection of a single image, the plugin will return it.
        maximumImagesCount: int,
        
        // iOS only (ignored on Android). Max width and height to allow the images to be.
        // Will keep aspect ratio no matter what.  So if both are 800, the returned image
        // will be at most 800 pixels wide and 800 pixels tall.  If the width is
        // 800 and height 0 the image will be 800 pixels wide if the source
        // is at least that wide.
        width: int,
        height: int,
        
        // iOS only (ignored on Android). quality of resized image, defaults to 100
        quality: int (0-100),

        // iOS only (ignored on Android). output type, defaults to FILE_URIs.
        // available options are 
        // window.imagePicker.OutputType.FILE_URI (0) or 
        // window.imagePicker.OutputType.BASE64_STRING (1)
        outputType: int
    };
    
### Note for Android Use

The selected images are copied into the app-private cache directory and returned as `file://`
URIs. The plugin sweeps its own leftover temp files (`ipick_*`) at the start of each pick, and
the cache is app-private (Android may also evict it under storage pressure); even so, move or
delete the files once you have read them in JavaScript.

## Permissions

**Android:** none. Since 2.3.6 the plugin uses the system photo picker
([`PickVisualMedia`](https://developer.android.com/training/data-storage/shared/photopicker)),
which grants per-selection access without any `READ_MEDIA_*` / `READ_EXTERNAL_STORAGE`
permission. The `hasReadPermission` / `requestReadPermission` methods are kept as no-op stubs
(`hasReadPermission` always resolves `true`, `requestReadPermission` succeeds immediately) so
existing callers keep working — there is nothing to request.

**iOS:** access is governed by `NSPhotoLibraryUsageDescription`, as before.


## Libraries used

#### ELCImagePicker

For iOS this plugin uses the ELCImagePickerController, with slight modifications for the iOS image picker.  ELCImagePicker uses the MIT License which can be found in the file LICENSE.

https://github.com/B-Sides/ELCImagePickerController

#### AndroidX Activity (`PickVisualMedia`)

Since 2.3.6 the Android side uses the system photo picker from `androidx.activity:activity`
(Apache 2.0). The previous Android implementation (MultiImageChooser, ImageFetcher, FakeR) was
removed when the system photo picker replaced the in-app gallery.

## License

The MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
