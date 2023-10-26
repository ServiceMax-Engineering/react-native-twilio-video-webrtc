//
//  TVIVideoView+Extra.h
//  react-native-twilio-video-webrtc
//
//  Created by Puneet Pal Singh on 10/24/23.
//

#import <UIKit/UIKit.h>
#import <React/RCTComponent.h>
#import <objc/runtime.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIView (Extra)
- (RCTDirectEventBlock)onFrameDimensionsChanged;
- (void)setOnFrameDimensionsChanged:(RCTDirectEventBlock)onFrameDimensionsChanged;
@end

NS_ASSUME_NONNULL_END
