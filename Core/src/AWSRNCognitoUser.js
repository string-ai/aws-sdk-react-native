import React, { Component } from 'react';
import {
  Platform,
  NativeModules,
  NativeAppEventEmitter,
  DeviceEventEmitter
} from 'react-native';

const nativeModule = NativeModules.AWSRNCognitoUser;
const listener = (Platform.OS === 'ios') ? NativeAppEventEmitter : DeviceEventEmitter;

export default class AWSRNCognitoUser{
 /*
  * Represents a AWSRNCognitoUser class
  * @constructor
  */
  constructor(){

  }

  /*
   *
  */
  authenticateUser(email, password){
    return nativeModule.authenticateUser(email, password);
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
