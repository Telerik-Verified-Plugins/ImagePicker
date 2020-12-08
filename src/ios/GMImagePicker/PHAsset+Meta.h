//
//  PHAsset+Meta.h
//  CordovaPlugins
//
//  Created by Alexander on 11/19/15.
//  Copyright © 2015 Kentdome, LLC. All rights reserved.
//

#import <Foundation/Foundation.h>
@import Photos;

#import <objc/runtime.h>

#import <Photos/PHAsset.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^PHAssetStringBlock)(NSString *string);
typedef void (^PHAssetBoolBlock)(BOOL success);
typedef void (^PHAssetMetadataBlock)(NSDictionary *metadata);

@interface PHAsset (Meta)

-(void)setMetadata:(NSDictionary *)metadata;

/*!
 @method        requestUniformTypeWithCompletionBlock
 @description   Get system-declared uniform type identifiers of an asset (com.compuserve.gif, public.png, etc)
 @param         completionBlock This block is passed a string. This parameter may be nil.
 */
-(void)requestUniformTypeWithCompletionBlock:(PHAssetStringBlock)completionBlock;

/*!
 @method        requestMetadataWithOptions:options:completionBlock
 @description   Get metadata dictionary of an asset (contains sub-dictionaries EXIF, GPS etc)
 @param         options An PHContentEditingInputRequestOptions to specify options of a PHAsset object (networkAccessAllowed, progressHandler)
 @param         completionBlock This block is passed a dictionary of metadata properties. This parameter may be nil.
 */
-(void)requestMetadataWithOptions:(PHContentEditingInputRequestOptions*)options completionBlock:(PHAssetMetadataBlock)completionBlock;


/*!
 @method        requestMetadataWithCompletionBlock
 @description   Get metadata dictionary of an asset (contains sub-dictionaries EXIF, GPS etc)
 @param         completionBlock This block is passed a dictionary of metadata properties. This parameter may be nil.
 */
-(void)requestMetadataWithCompletionBlock:(PHAssetMetadataBlock)completionBlock;

@end

NS_ASSUME_NONNULL_END
