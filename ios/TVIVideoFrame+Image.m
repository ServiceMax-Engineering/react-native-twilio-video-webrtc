//
//  TVIVideoFrame+Image.m
//  RNTwilioVideoWebRTC
//
//  Created by Puneet Pal Singh on 4/4/23.
//  Copyright © 2023 Employ. All rights reserved.
//

#import "TVIVideoFrame+Image.h"

@implementation TVIVideoFrame (Image)

- (UIImage *)getImage {
    CIImage *ciImage = [[CIImage alloc] initWithCVPixelBuffer:self.imageBuffer];
    CIContext *context = [[CIContext alloc] initWithOptions:nil];
    CGImageRef cgImage = [context createCGImage:ciImage fromRect:ciImage.extent];
    UIImage *image = [[UIImage alloc] initWithCGImage:cgImage];
    CGImageRelease(cgImage);
    return image;
}

- (NSString *)getImagePath {
    NSUUID *uuid = [NSUUID UUID];
    NSString *imageNameWihtoutExtension = [uuid UUIDString];
    NSString *imageName = [imageNameWihtoutExtension stringByAppendingPathExtension:@"jpeg"];
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *filePath = [[paths objectAtIndex:0] stringByAppendingPathComponent:imageName];
    
    UIImage *lastRemoteVideoImage = [self getImage];
    [UIImageJPEGRepresentation(lastRemoteVideoImage, 1.0) writeToFile:filePath atomically:YES];
    return filePath;
}

@end
