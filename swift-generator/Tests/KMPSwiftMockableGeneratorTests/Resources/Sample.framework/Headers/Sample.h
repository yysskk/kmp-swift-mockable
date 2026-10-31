#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

// Shaped like a Kotlin/Native export: an interface becomes a protocol without
// NSObject, and a suspend function takes a completion handler.
__attribute__((swift_name("Greeter")))
@protocol SampleGreeter
@required
- (NSString *)greetName:(NSString *)name __attribute__((swift_name("greet(name:)")));
- (void)loadId:(int32_t)id completionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("load(id:completionHandler:)")));
@end

NS_ASSUME_NONNULL_END
