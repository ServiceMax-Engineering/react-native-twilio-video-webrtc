//
//  TVIVideoFrame+Image.h
//  RNTwilioVideoWebRTC
//
//  Created by Puneet Pal Singh on 4/4/23.
//  Copyright © 2023 Employ. All rights reserved.
//

#import <TwilioVideo/TwilioVideo.h>

NS_ASSUME_NONNULL_BEGIN

@interface TVIVideoFrame (Image)

- (UIImage *)getImage;
- (NSString *)getImagePath;

@end

NS_ASSUME_NONNULL_END
