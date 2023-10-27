//
//  RCTTWLocalVideoViewManager.m
//  Black
//
//  Created by Martín Fernández on 6/13/17.
//
//

#import "RCTTWLocalVideoViewManager.h"
#import "UIView+Extra.h"
#import "RCTTWVideoModule.h"

@interface RCTTWLocalVideoViewManager() <TVIVideoViewDelegate>
@end

@implementation RCTTWLocalVideoViewManager

RCT_EXPORT_MODULE()

RCT_CUSTOM_VIEW_PROPERTY(scalesType, NSInteger, TVIVideoView) {
  view.subviews[0].contentMode = [RCTConvert NSInteger:json];
}

RCT_EXPORT_VIEW_PROPERTY(onFrameDimensionsChanged, RCTDirectEventBlock)

- (UIView *)view {
  UIView *container = [[UIView alloc] init];
  TVIVideoView *inner = [[TVIVideoView alloc] init];
  inner.delegate = self;
  inner.autoresizingMask = (UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth);
  [container addSubview:inner];
  return container;
}

RCT_CUSTOM_VIEW_PROPERTY(enabled, BOOL, TVIVideoView) {
  if (json) {
    RCTTWVideoModule *videoModule = [self.bridge moduleForName:@"TWVideoModule"];
    BOOL isEnabled = [RCTConvert BOOL:json];

    if (isEnabled) {
      [videoModule addLocalView:view.subviews[0]];
    } else {
      [videoModule removeLocalView:view.subviews[0]];
    }
  }
}

# pragma mark - TVIVideoViewDelegate
- (void)videoViewDidReceiveData:(TVIVideoView *)view {
    if (view.onFrameDimensionsChanged) {
        view.onFrameDimensionsChanged(@{@"width": [NSNumber numberWithInt:view.videoDimensions.width], @"height": [NSNumber numberWithInt:view.videoDimensions.height]});
    }
}

@end
