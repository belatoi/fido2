import 'dart:async';
import 'package:flutter/services.dart';

class Cakefido2 {
  static Cakefido2 _instance = Cakefido2._();

  Cakefido2._();

  static Cakefido2 get instance => _instance;

  static const MethodChannel _channel = const MethodChannel('cakefido2');

  Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  Future<bool> setHeader(Map header) async {
    var isHeader = await _channel.invokeMethod('actionSetHeader', {"header": header});
    return isHeader;
  }

  Future<bool> actionSetEnvironment(String env) async {
    var isHeader = await _channel.invokeMethod('actionSetEnvironment', {"environment": env});
    return isHeader;
  }

  Future<String?> actionRegisterRequest(String accessToken) async {
    final String? isRegister = await _channel
        .invokeMethod('actionRegisterRequest', {"access_token": accessToken});
    return isRegister;
  }

  Future<String?> actionSignInRequest(String userName) async {
    final String? version = await _channel
        .invokeMethod('actionSignInRequest', {"user_name": userName});
    return version;
  }
  
  void trackingDone(Function tracking){
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case "tracking_done":
          {
            tracking.call();
          }
          break;
        default:
          break;
      }
    });
  }
}
