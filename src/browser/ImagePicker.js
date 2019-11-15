var cacheDirectory;

//Chrome needs its own stuff
if (require('./isChrome')()) {
  cacheDirectory = 'filesystem:' + window.location.origin + '/temporary/';
} else {
  cacheDirectory = 'file:///temporary/';
}

//Edge needs its own stuff
if (!HTMLCanvasElement.prototype.toBlob) {
  Object.defineProperty(HTMLCanvasElement.prototype, 'toBlob', {
    value: function (callback, type, quality) {
      var canvas = this;
      setTimeout(function() {
        var binStr = atob( canvas.toDataURL(type, quality).split(',')[1] ),
        len = binStr.length,
        arr = new Uint8Array(len);

        for (var i = 0; i < len; i++ ) {
          arr[i] = binStr.charCodeAt(i);
        }

        callback( new Blob( [arr], {type: type || 'image/png'} ) );
      });
    }
  });
}

async function getPictures(successCallback, errorCallback, data) {
  //Set default params just in case
  var params = {
    maximumImagesCount: 20,
    width: 0,
    height: 0,
    quality: 100,
    outputType: 0
  }

  //Read params from cordova
  if (data[0].maximumImagesCount) params.maximumImagesCount = data[0].maximumImagesCount;
  if (data[0].width) params.width = data[0].width;
  if (data[0].height) params.height = data[0].height;
  if (data[0].quality) params.quality = data[0].quality;
  if (data[0].outputType) params.outputType = data[0].outputType;

  openImagePicker(params.maximumImagesCount, params.width, params.height, params.quality, params.outputType).then((images) => {
    successCallback(images);
  }).catch((error) => {
    errorCallback(error);
  });
}

//hasReadPermission is not relevant in browsers, let's just return a success so everyone is happy
async function hasReadPermission(successCallback, errorCallback, data) {
  successCallback();
}

//requestReadPermission is not relevant in browsers, let's just return a success so everyone is happy
async function requestReadPermission(successCallback, errorCallback, data) {
  successCallback();
}

function openImagePicker(maximumImagesCount, desiredWidth, desiredHeight, quality, outputType) {
  return new Promise((resolve, reject) => {
    let fileChooser = document.createElement('input');

    fileChooser.type = 'file';
    fileChooser.accept = 'image/png, image/jpeg';

    if (maximumImagesCount > 1) {
      fileChooser.setAttribute('multiple', '');
    }

    fileChooser.onchange = (event) => {
      let resizeImagePromises = [];
      let fileNames =  []; //We need to store filenames as chrome deletes event.target.files to quickly

      if (event.target.files.length <= maximumImagesCount) {
        for (let file of event.target.files) {
          fileNames.push(file.name);
          resizeImagePromises.push(resizeImage(file, desiredWidth, desiredHeight, quality, outputType));
        }

        Promise.all(resizeImagePromises).then((images) => {
          if (outputType == 0) {
            //If we need FILE_URI, we need to store the files in the temporary dir of the browser
            //So we need to write the files there Before
            let saveBlobPromises = [];

            for (let i = 0; i < images.length; i++) {
              saveBlobPromises.push(saveBlobToTemporaryFileSystem(images[i], fileNames[i]));
            }

            Promise.all(saveBlobPromises).then((fileURIs) => {
              resolve(fileURIs);
            }).catch((error) => {
              reject('ERROR_WHILE_CREATING_FILES ' + error);
            });
          } else {
            //resizeImages returns the images in base64, so no need to do anything
            resolve(images);
          }
        }).catch((error) => {
          reject('ERROR_WHILE_RESIZING ' + error);
        });
      } else {
        reject('TO_MANY_IMAGES');
      }
    };

    //Now that the event is hooked we can click it
    fileChooser.click();
  });
}

function resizeImage(file, desiredWidth, desiredHeight, quality, outputType) {
  return new Promise((resolve, reject) => {
    let reader = new FileReader();

    //if (desiredWidth == 0 || desiredHeight == 0 || ['gif'].some(type => file.type.includes(type))) {
    if (desiredWidth == 0 || desiredHeight == 0) {
      //Resolve original file if no scaling
      //If we have a gif we do not resize it because we would loose animation
      if (outputType == 0) {
        //No scaling required and FILE_URI required?
        //Lets just resolve the file, which is a blob
        resolve(file);
      } else {
        //No scaling required and BASE64_STRING required?
        //Lets read the file and resolve the base64
        reader.readAsDataURL(file);

        reader.onload = (e) => {
          resolve(e.target.result);
        };

        reader.onerror = (error) => {
          reject(error);
        }
      }
    } else {
      //We need to resize, so lets do it
      reader.readAsDataURL(file);

      reader.onload = (e) => {
        let img = new Image();
        img.src = e.target.result;
        img.onload = (pic) => {
          let canvas = document.createElement('canvas');

          if (img.height > desiredHeight || img.width > desiredWidth) {
            if ((img.height / desiredHeight) > (img.width / desiredWidth)) {
              canvas.width = img.width / (img.height / desiredHeight)
              canvas.height = desiredHeight;
            } else {
              canvas.width = desiredWidth
              canvas.height = img.height / (img.width / desiredWidth) ;
            }
          } else {
            canvas.width = img.width;
            canvas.height = img.height;
          }

          let ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

          if (outputType == 0) {
            //FILE_URI required, resolve with a blob
            ctx.canvas.toBlob((blob) => {
              resolve(blob);
            }, 'image/jpeg', (quality / 100));

          } else {
            //base64 required
            //To be consistent with Android (and iOS) we remove the base64 header from the string
            resolve(ctx.canvas.toDataURL('image/jpeg', (quality / 100)).replace('data:image/jpeg;base64,', ''));
          }
        };
      };

      reader.onerror = (error) => {
        reject(error);
      }
    }
  });
}

function saveBlobToTemporaryFileSystem(blob, fileName) {
  return new Promise((resolve, reject) => {
    window.requestFileSystem(window.TEMPORARY, blob.size, (fs) => {
      fs.root.getFile(fileName, {create: true, exclusive: false}, (fileEntry) => {
        fileEntry.createWriter((fileWriter) => {
          fileWriter.onwriteend = (e) => {
            resolve(cacheDirectory + fileName);
          };

          fileWriter.onerror = (error) => {
            reject(error);
          };

          if (blob) {
            fileWriter.write(blob);
          } else {
            reject('ERROR_NO_DATA_TO_WRITE');
          }
        });
      });
    });
  });
}

module.exports = {
  getPictures: getPictures,
  hasReadPermission: hasReadPermission,
  requestReadPermission: requestReadPermission
};

require( "cordova/exec/proxy" ).add( "ImagePicker", module.exports );
