//
//  TVIVideoView+Extra.m
//  react-native-twilio-video-webrtc
//
//  Created by Puneet Pal Singh on 10/24/23.
//

#import "UIView+Extra.h"

@implementation UIView (Extra)

RCTDirectEventBlock _onFrameDimensionsChanged;

- (RCTDirectEventBlock)onFrameDimensionsChanged {
    return _onFrameDimensionsChanged;
}

- (void)setOnFrameDimensionsChanged:(RCTDirectEventBlock)onFrameDimensionsChanged {
    _onFrameDimensionsChanged = onFrameDimensionsChanged;
}

@end
