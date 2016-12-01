#import <Foundation/Foundation.h>
#import <AWSCore/AWSCore.h>
#import <AWSCore/AWSTask.h>

#import "RCTEventDispatcher.h"
#import "RCTBridgeModule.h"
#import "AWSRNHelper.h"

@interface AWSRNCognitoIdentityUserPool : NSObject <RCTBridgeModule, AWSCognitoIdentityPasswordAuthentication>
-(void) getPasswordAuthenticationDetails: (AWSCognitoIdentityPasswordAuthenticationInput *) authenticationInput
        passwordAuthenticationCompletionSource: (AWSTaskCompletionSource<AWSCognitoIdentityPasswordAuthenticationDetails *> *) passwordAuthenticationCompletionSource;

-(void) didCompletePasswordAuthenticationStepWithError:(NSError*) error;
@end
