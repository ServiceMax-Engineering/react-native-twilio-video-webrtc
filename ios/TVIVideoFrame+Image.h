//
//  TVIVideoFrame+Image.h
//  react-native-twilio-video-webrtc
//
//  Created by Puneet Pal Singh on 4/4/23.
//

#import <TwilioVideo/TwilioVideo.h>

NS_ASSUME_NONNULL_BEGIN

@interface TVIVideoFrame (Image)

- (UIImage *)getImage:(BOOL)isLocal isMirroring:(BOOL)isMirroring;

- (NSString *)getImagePath:(BOOL)isLocal isMirroring:(BOOL)isMirroring;

@end

NS_ASSUME_NONNULL_END
