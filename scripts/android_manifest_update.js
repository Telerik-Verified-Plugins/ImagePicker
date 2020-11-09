#!/usr/bin/env node

module.exports = function (context) {

    var fs = require('fs');
    var path = require('path');
    var manifestDirectory = path.join(context.opts.projectRoot, '/platforms/android/app/src/main/');
    var manifestFile = path.join(manifestDirectory, 'AndroidManifest.xml');

    var LEGACY_STORAGE_ATTRIBUTE = 'android:requestLegacyExternalStorage';
    var LEGACY_STORAGE_VALUE = LEGACY_STORAGE_ATTRIBUTE + '="true"';
    var LEGACY_STORAGE_REGEX = new RegExp(LEGACY_STORAGE_ATTRIBUTE + '="([^"]+)"');

    fs.exists(manifestDirectory, function (exists) {
        if (!exists) {
            console.log('Manifest directory not found. No image-picker updates.');
        }
        else {

            fs.readFile(manifestFile, 'utf8', function (err, data) {
                if (err) {
                    throw new Error('Unable to read AndroidManifest.xml: ' + err);
                }

                var updatedManifest;

                //Add the android:requestLegacyExternalStorage value in the manifest
                if (!data.match(LEGACY_STORAGE_ATTRIBUTE)) {
                    updatedManifest = data.replace(/<application/g, '<application ' + LEGACY_STORAGE_VALUE + ' ');
                }
                //update the android:requestLegacyExternalStorage value in the manifest, if it's not set to true
                else if (data.match(LEGACY_STORAGE_REGEX) && !data.match(LEGACY_STORAGE_VALUE)) {
                    updatedManifest = data.replace(LEGACY_STORAGE_REGEX, LEGACY_STORAGE_VALUE);
                }

                //Update the actual file - write it out
                if (updatedManifest) {
                    fs.writeFile(manifestFile, updatedManifest, 'utf8', function (err) {
                        if (err) throw new Error('Unable to write AndroidManifest.xml: ' + err);
                    })
                }
            });
        }
    });
}