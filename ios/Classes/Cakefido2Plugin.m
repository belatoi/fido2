#import "Cakefido2Plugin.h"
#if __has_include(<cakefido2/cakefido2-Swift.h>)
#import <cakefido2/cakefido2-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "cakefido2-Swift.h"
#endif

@implementation Cakefido2Plugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftCakefido2Plugin registerWithRegistrar:registrar];
}
@end
