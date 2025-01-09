//
//  TVIVideoFrame+Image.m
//  react-native-twilio-video-webrtc
//
//  Created by Puneet Pal Singh on 4/4/23.
//

#import "TVIVideoFrame+Image.h"
#import <Accelerate/Accelerate.h>

@implementation TVIVideoFrame (Image)

- (UIImage *)getImage:(BOOL)isLocal isMirroring:(BOOL)isMirroring ignoreOrientation:(BOOL)ignoreOrientation {
    CIImage *ciImage = [self convertToCIImage];
    
    CGImagePropertyOrientation newOrientation = kCGImagePropertyOrientationUp;
    UIDeviceOrientation currentOrientation = [[UIDevice currentDevice] orientation];
    
    if (isLocal) {
        switch (currentOrientation) {
            case UIDeviceOrientationPortrait:
            case UIDeviceOrientationFaceUp:
                newOrientation = isMirroring ? kCGImagePropertyOrientationLeftMirrored : kCGImagePropertyOrientationRight;
                break;
            case UIDeviceOrientationLandscapeLeft:
                newOrientation = isMirroring ? kCGImagePropertyOrientationDownMirrored : kCGImagePropertyOrientationUp;
                break;
            case UIDeviceOrientationLandscapeRight:
                newOrientation = isMirroring ? kCGImagePropertyOrientationUpMirrored : kCGImagePropertyOrientationDown;
                break;
            case UIDeviceOrientationPortraitUpsideDown:
            case UIDeviceOrientationFaceDown:
                newOrientation = isMirroring ? kCGImagePropertyOrientationRightMirrored : kCGImagePropertyOrientationLeft;
                break;
            default:
                break;
        }
    } else {
        switch (currentOrientation) {
            case UIDeviceOrientationPortrait:
            case UIDeviceOrientationFaceUp:
                newOrientation = kCGImagePropertyOrientationUp;
                break;
            case UIDeviceOrientationLandscapeLeft:
                newOrientation = ignoreOrientation ? kCGImagePropertyOrientationUp : kCGImagePropertyOrientationLeft;
                break;
            case UIDeviceOrientationLandscapeRight:
                newOrientation = ignoreOrientation ? kCGImagePropertyOrientationUp : kCGImagePropertyOrientationRight;
                break;
            case UIDeviceOrientationPortraitUpsideDown:
            case UIDeviceOrientationFaceDown:
                newOrientation = ignoreOrientation ? kCGImagePropertyOrientationUp : kCGImagePropertyOrientationDown;
                break;
            default:
                break;
        }
    }
    
    
    ciImage = [ciImage imageByApplyingCGOrientation:newOrientation];
    UIImage *image = [[UIImage alloc] initWithCIImage:ciImage];
    return image;
}

- (NSString *)getImagePath:(BOOL)isLocal isMirroring:(BOOL)isMirroring ignoreOrientation:(BOOL)ignoreOrientation {
    UIImage *lastRemoteVideoImage = [self getImage:isLocal isMirroring:isMirroring ignoreOrientation:ignoreOrientation];
    NSData *data = UIImageJPEGRepresentation(lastRemoteVideoImage, 1.0);
    
    if (!data) return nil;
    
    NSUUID *uuid = [NSUUID UUID];
    NSString *imageNameWihtoutExtension = [uuid UUIDString];
    NSString *imageName = [imageNameWihtoutExtension stringByAppendingPathExtension:@"jpeg"];
    NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:imageName];
    [data writeToFile:filePath atomically:YES];
    return [self makeValidFilePath:filePath];
}

- (NSString *)makeValidFilePath:(NSString *)filePath {
    NSURL *fileWithUrl = [NSURL fileURLWithPath:filePath];
    NSURL *absoluteUrl = [fileWithUrl URLByDeletingLastPathComponent];
    NSString *fileUrl = [NSString stringWithFormat:@"file://%@/%@", [absoluteUrl path] , [fileWithUrl lastPathComponent]];
    return fileUrl;
}

- (CIImage *)convertToCIImage {
    CIImage *ciImage = nil;
    
    OSType type = CVPixelBufferGetPixelFormatType(self.imageBuffer);
    
    if (type == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange) {
        ciImage = [CIImage imageWithCVImageBuffer:self.imageBuffer];
    } else {
        CVPixelBufferRef destinationPixelBuffer = nil;
        CVPixelBufferCreate(nil, self.width, self.height, kCVPixelFormatType_32BGRA, nil, &destinationPixelBuffer);
        [self convertFrameVImageYUV:self toBuffer:destinationPixelBuffer];
        ciImage = [CIImage imageWithCVPixelBuffer:destinationPixelBuffer];
    }
    
    return ciImage;
}

- (void)convertFrameVImageYUV:(TVIVideoFrame *)frame toBuffer:(CVPixelBufferRef)pixelBufferRef {
    if (pixelBufferRef == NULL) { return; }
    
    CVPixelBufferLockBaseAddress(frame.imageBuffer, 0);
    CVPixelBufferLockBaseAddress(pixelBufferRef, 0);
    
    vImage_YpCbCrPixelRange pixelRange = { 0, 128, 255, 255, 255, 1, 255, 0 };
    vImage_YpCbCrToARGB *outInfo = malloc(sizeof(vImage_YpCbCrToARGB));
    vImageYpCbCrType inType = kvImage420Yp8_Cb8_Cr8;
    vImageARGBType outType = kvImageARGB8888;
    
    vImageConvert_YpCbCrToARGB_GenerateConversion(kvImage_YpCbCrToARGBMatrix_ITU_R_601_4, &pixelRange, outInfo, inType, outType, kvImagePrintDiagnosticsToConsole);
    
    const uint8_t *yPlane = CVPixelBufferGetBaseAddressOfPlane(frame.imageBuffer, 0);
    const uint8_t *uPlane =  CVPixelBufferGetBaseAddressOfPlane(frame.imageBuffer, 1);
    const uint8_t *vPlane =  CVPixelBufferGetBaseAddressOfPlane(frame.imageBuffer, 2);
    size_t yWidth = CVPixelBufferGetWidthOfPlane(frame.imageBuffer, 0);
    size_t uWidth = CVPixelBufferGetWidthOfPlane(frame.imageBuffer, 1);
    size_t vWidth = CVPixelBufferGetWidthOfPlane(frame.imageBuffer, 2);
    size_t yHeight = CVPixelBufferGetHeightOfPlane(frame.imageBuffer, 0);
    size_t uHeight = CVPixelBufferGetHeightOfPlane(frame.imageBuffer, 1);
    size_t vHeight = CVPixelBufferGetHeightOfPlane(frame.imageBuffer, 2);
    size_t yStride = CVPixelBufferGetBytesPerRowOfPlane(frame.imageBuffer, 0);
    size_t uStride = CVPixelBufferGetBytesPerRowOfPlane(frame.imageBuffer, 1);
    size_t vStride = CVPixelBufferGetBytesPerRowOfPlane(frame.imageBuffer, 2);
    
    vImage_Buffer yPlaneBuffer = {.data = (void *)yPlane, .height = yHeight, .width = yWidth, .rowBytes = yStride};
    vImage_Buffer uPlaneBuffer = {.data = (void *)uPlane, .height = uHeight, .width = uWidth, .rowBytes = uStride};
    vImage_Buffer vPlaneBuffer = {.data = (void *)vPlane, .height = vHeight, .width = vWidth, .rowBytes = vStride};
    
    void *pixelBufferData = CVPixelBufferGetBaseAddress(pixelBufferRef);
    size_t rowBytes = CVPixelBufferGetBytesPerRow(pixelBufferRef);
    vImage_Buffer destinationImageBuffer = {.data = pixelBufferData, .height = frame.height, .width = frame.width, .rowBytes = rowBytes};
    
    uint8_t permuteMap[4] = {1, 2, 3, 0};
    
    vImageConvert_420Yp8_Cb8_Cr8ToARGB8888(&yPlaneBuffer, &vPlaneBuffer, &uPlaneBuffer, &destinationImageBuffer, outInfo, permuteMap, 255, 0);
    
    free(outInfo);
    CVPixelBufferUnlockBaseAddress(frame.imageBuffer, 0);
    CVPixelBufferUnlockBaseAddress(pixelBufferRef, 0);
}

@end
