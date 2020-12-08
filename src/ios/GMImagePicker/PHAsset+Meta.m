//
//  PHAsset+Meta.m
//  PHAssetTest
//
//  Created by Alexander on 11/19/15.
//  Copyright © 2015 Kentdome, LLC. All rights reserved.
//

#import "PHAsset+Meta.h"

@implementation PHAsset (Meta)

-(void)requestUniformTypeWithCompletionBlock:(PHAssetStringBlock)completionBlock{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        PHImageRequestOptions *options = [[PHImageRequestOptions alloc]init];
        options.networkAccessAllowed = YES;
        PHImageManager *manager = [[PHImageManager alloc] init];
        [manager requestImageDataForAsset:self options:options resultHandler:
         ^(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info) {
             dispatch_async(dispatch_get_main_queue(), ^{
                 completionBlock(dataUTI);
             });
         }];
    });
}


-(void)requestMetadataWithOptions:(PHContentEditingInputRequestOptions*)options completionBlock:(PHAssetMetadataBlock)completionBlock{
    if(self.mediaType == PHAssetMediaTypeImage){
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [self requestContentEditingInputWithOptions:options completionHandler:^(PHContentEditingInput *contentEditingInput, NSDictionary *info) {
                CIImage *image = [CIImage imageWithContentsOfURL:contentEditingInput.fullSizeImageURL];
                dispatch_async(dispatch_get_main_queue(), ^{
                    completionBlock(image.properties);
                });
            }];

        });
    }
}


-(void)requestMetadataWithCompletionBlock:(PHAssetMetadataBlock)completionBlock{
    PHImageRequestOptions *options = [PHImageRequestOptions new];
    options.networkAccessAllowed = YES;
    options.synchronous = YES;
    options.version = PHImageRequestOptionsVersionOriginal;
    PHImageManager *manager = [[PHImageManager alloc] init];
    [manager requestImageDataForAsset:self options:options resultHandler:^(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info){
        CIImage *image = [CIImage imageWithData:imageData];
        dispatch_async(dispatch_get_main_queue(), ^{
            completionBlock(image.properties);
        });
    }];
}

@end
