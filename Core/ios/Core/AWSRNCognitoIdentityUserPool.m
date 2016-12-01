//
// Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License").
// You may not use this file except in compliance with the License.
// A copy of the License is located at
//
// http://aws.amazon.com/apache2.0
//
// or in the "license" file accompanying this file. This file is distributed
// on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
// express or implied. See the License for the specific language governing
// permissions and limitations under the License.
//
#import "AWSRNCognitoIdentityUserPool.h"

@interface AWSRNCognitoIdentityUserPool()
@end

@implementation AWSRNCognitoIdentityUserPool{
    NSMutableDictionary *options;
    AWSCognitoIdentityUserPool *pool;
}

@synthesize bridge = _bridge;

typedef void (^ Block)(id, int);

RCT_EXPORT_MODULE(AWSRNCognitoIdentityUserPool)

-(instancetype)init{
    self = [super init];
    if (self) {
        [AWSServiceConfiguration
          addGlobalUserAgentProductToken:[NSString stringWithFormat:@"aws-sdk-react-native/%@",[helper getSDKVersion]]];
    }
    return self;
}

#pragma mark - Exposed Methods

RCT_EXPORT_METHOD(getSession:(NSString*)username withPassword:(NSString*)password resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){


}

RCT_EXPORT_METHOD(getIdentityIDAsync:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    [[credentialProvider getIdentityId] continueWithBlock:^id(AWSTask *task) {
        if (task.exception){
            dispatch_async(dispatch_get_main_queue(), ^{
                @throw [NSException exceptionWithName:task.exception.name reason:task.exception.reason userInfo:task.exception.userInfo];
            });
        }
        if (task.error) {
            reject([NSString stringWithFormat:@"%ld",task.error.code],task.error.description,task.error);
        }
        else {
            resolve(@{@"identityId":task.result});
        }
        return nil;
    }];
}

RCT_EXPORT_METHOD(isAuthenticated:(RCTResponseSenderBlock)callback){
    BOOL isAuth = [credentialProvider.identityProvider isAuthenticated];
    NSNumber* value = [NSNumber numberWithBool:isAuth];
    callback(@[[NSNull null],value]);
}


RCT_EXPORT_METHOD(initWithOptions:(NSDictionary *)inputOptions)
{
  //setup service config
  AWSServiceConfiguration *serviceConfiguration = [AWSServiceManager defaultServiceManager].defaultServiceConfiguration;

  //create a pool
  AWSCognitoIdentityUserPoolConfiguration *configuration = [[AWSCognitoIdentityUserPoolConfiguration alloc] initWithClientId:inputOptions[@"CLIENT_ID"]
                                                                                                                clientSecret:inputOptions[@"CLIENT_SECRET"]
                                                                                                                      poolId:inputOptions[@"USER_POOL_ID"]
                                                                                                                      ];
  [AWSCognitoIdentityUserPool registerCognitoIdentityUserPoolWithConfiguration:serviceConfiguration userPoolConfiguration:configuration forKey:@"Parking"];
  self.pool = [AWSCognitoIdentityUserPool CognitoIdentityUserPoolForKey:@"Parking"];
  
}

#pragma mark - AWSIdentityProviderManager

- (AWSTask<NSDictionary<NSString *, NSString *> *> *)logins{
    return [[AWSTask taskWithResult:nil] continueWithSuccessBlock:^id _Nullable(AWSTask * _Nonnull task) {
        __block NSArray* arr;

        dispatch_semaphore_t sendMessageSemaphore = dispatch_semaphore_create(0);

        [self sendMessage:[[NSMutableDictionary alloc]init] toChannel:@"LoginsRequestedEvent" semaphore:sendMessageSemaphore withCallback:^(NSArray* response) {
            arr = response;
        }];

        dispatch_semaphore_wait(sendMessageSemaphore, DISPATCH_TIME_FOREVER);

        if (![[arr objectAtIndex:0]isKindOfClass:[NSDictionary class]]){
            return [[NSDictionary alloc]init];
        }
        return [self setLogins:[arr objectAtIndex:0]];
    }];
}

#pragma mark - Helper Methods

-(AWSCognitoCredentialsProvider*)getCredentialsProvider{
    return credentialProvider;
}

-(void)identityDidChange:(NSNotification*)notification {
    NSMutableDictionary *dict = [[NSMutableDictionary alloc]init];
    [dict setValue:[notification.userInfo valueForKey:AWSCognitoNotificationPreviousId] forKey:@"Previous"];
    [dict setValue:[notification.userInfo valueForKey:AWSCognitoNotificationNewId] forKey:@"Current"];
    [self sendMessage:dict toChannel:@"IdentityChange"];
}


RCT_EXPORT_METHOD(sendCallbackResponse:(NSString *)callbackId response:(NSArray *)response){
    NSDictionary* callbackInfo = [callbacks objectForKey:callbackId];
    if(callbackInfo) {
        RCTResponseSenderBlock callback = callbackInfo[@"callback"];
        dispatch_semaphore_t semaphore = callbackInfo[@"semaphore"];
        [callbacks removeObjectForKey:callbackId];

        callback(response);
        dispatch_semaphore_signal(semaphore);
    }
    else{
        NSLog(@"WARN callback id not found!");
    }
}

-(NSString*) registerCallBack:(RCTResponseSenderBlock)callback semaphore:(dispatch_semaphore_t)semaphore {
    if (!callbacks){
        callbacks = [@{} mutableCopy];
    }
    NSString* callbackId = [[NSUUID UUID] UUIDString];
    callbacks[callbackId] = @{
                              @"callback": callback ? callback : (^(NSArray *response) { }),
                              @"semaphore":semaphore
                              };
    return callbackId;
}

-(void)sendMessage:(NSMutableDictionary*)info toChannel:(NSString*)channel{
    [self.bridge.eventDispatcher
     sendAppEventWithName:channel
     body:[info copy]
     ];
}

-(void)sendMessage:(NSMutableDictionary*)info toChannel:(NSString*)channel semaphore:(dispatch_semaphore_t)semaphore withCallback:(RCTResponseSenderBlock)callback  {
    NSString * callbackId = [self registerCallBack:callback semaphore:semaphore];
    [info setValue:callbackId forKey:@"callbackId"];
    [self sendMessage:info toChannel:channel];
}

-(NSMutableDictionary*)setLogins:(NSMutableDictionary*)reactLogins{
    NSMutableDictionary *logins = [[NSMutableDictionary alloc]init];
    for (NSString* key in reactLogins){
        if ([key isEqualToString:@"FacebookProvider"]){
            [logins setValue:[reactLogins objectForKey:key] forKey:AWSIdentityProviderFacebook];
            continue;
        }else if ([key isEqualToString:@"DigitsProvider"]){
            [logins setValue:[reactLogins objectForKey:key] forKey:AWSIdentityProviderDigits];
            continue;
        }else if ([key isEqualToString:@"GoogleProvider"]){
            [logins setValue:[reactLogins objectForKey:key] forKey:AWSIdentityProviderGoogle];
            continue;
        }else if ([key isEqualToString:@"AmazonProvider"]){
            [logins setValue:[reactLogins objectForKey:key] forKey:AWSIdentityProviderLoginWithAmazon];
            continue;
        }else if ([key isEqualToString:@"TwitterProvider"]){
            [logins setValue:[reactLogins objectForKey:key] forKey:AWSIdentityProviderTwitter];
            continue;
        }else if ([key isEqualToString:@"CognitoProvider"]){
            [logins setValue:[reactLogins objectForKey:key] forKey:AWSIdentityProviderAmazonCognitoIdentity];
            continue;
        }else{
            [logins setValue:[reactLogins objectForKey:key] forKey:key];
            continue;
        }
    }
    return logins;
}

@end
