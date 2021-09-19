import 'dart:async';
import 'package:flutter/services.dart';

class Cakefido2 {
  static const MethodChannel _channel = const MethodChannel('test1');

  Cakefido2(Map header) {
    _channel.invokeMethod('actionSetHeader', {"header": header});
  }

  Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  Future<String?> actionRegisterRequest(String accessToken) async {
    final String? version = await _channel
        .invokeMethod('actionRegisterRequest', {"access_token": accessToken});
    return version;
  }

  Future<String?> actionSignInRequest(String userName) async {
    final String? version = await _channel
        .invokeMethod('actionSignInRequest', {"user_name": userName});
    return version;
  }
}
