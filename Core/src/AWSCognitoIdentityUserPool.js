import React, { Component } from 'react';
import {
  Platform,
  NativeModules,
  NativeAppEventEmitter,
  DeviceEventEmitter
} from 'react-native';

const nativeModule = NativeModules.AWSRNCognitoIdentityUserPool;
const listener = (Platform.OS === 'ios') ? NativeAppEventEmitter : DeviceEventEmitter;

export default class AWSCognitoIdentityUserPool{
 /*
  * Represents a AWSCognitoIdentityUserPool class
  * @constructor
  */
  constructor(){

  }

  /*
   *
  */
  async getSession(email, password){
    nativeModule.getSession(email, password);
    return true;
  }

  /*
   *
   */
  initWithOptions(options){
    if(!options.clientId){
      return "Error: No clientId";
    }
    if(!options.clientSecret){
      return "Error: No clientSecret";
    }
    if(!options.userPoolId){
      return "Error: No userPoolId";
    }
    nativeModule.initWithOptions(options);
   }
}
