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

  Future<bool?> actionRegisterRequest(String accessToken) async {
    final bool? isRegister = await _channel
        .invokeMethod('actionRegisterRequest', {"access_token": accessToken});
    return isRegister;
  }

  Future<String?> actionSignInRequest(String userName) async {
    final String? version = await _channel
        .invokeMethod('actionSignInRequest', {"user_name": userName});
    return version;
  }
}
