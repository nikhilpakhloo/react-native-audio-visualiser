#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(AudioVisualizerViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(type, NSString)
RCT_EXPORT_VIEW_PROPERTY(color, NSString)
RCT_EXPORT_VIEW_PROPERTY(sensitivity, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(smoothing, NSNumber)

@end
