import 'dart:convert';
import 'dart:math';

import 'package:crypto/crypto.dart';

/// PKCE 값 생성(사양서 §2.2.1). 웹과 규격이 같다.
///
/// `code_challenge_method` 는 항상 `S256` 이다. 서버는 `code_verifier` 를 그대로 제공자에게
/// 넘길 뿐 형식을 재해석하지 않으므로, 여기서 만든 값이 곧 제공자에게 도착한다.
abstract final class Pkce {
  static final Random _random = Random.secure();

  /// 무작위 32바이트 → base64url(패딩 제거) = 43자.
  static String verifier() => _base64Url(_randomBytes(32));

  /// `SHA-256(verifier)` → base64url.
  static String challenge(String verifier) =>
      _base64Url(sha256.convert(utf8.encode(verifier)).bytes);

  /// 무작위 16바이트 → base64url = 22자.
  static String state() => _base64Url(_randomBytes(16));

  static List<int> _randomBytes(int length) =>
      List<int>.generate(length, (_) => _random.nextInt(256));

  static String _base64Url(List<int> bytes) =>
      base64Url.encode(bytes).replaceAll('=', '');
}
