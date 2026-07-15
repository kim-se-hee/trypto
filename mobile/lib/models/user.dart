import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'user.g.dart';

/// `POST /api/auth/{provider}/login`
///
/// 제공자별로 바디가 갈린다(확정 와이어 계약). 카카오는 앱이 공식 SDK 로 받은 액세스 토큰을
/// 보내고(`{accessToken, clientType}`), 구글/웹은 기존 인가 코드 흐름을 그대로 쓴다
/// (`{code, codeVerifier, clientType}`). 두 흐름은 상호배타이므로 named 생성자로 나눈다.
///
/// [clientType] 은 서버가 플랫폼별 제공자 자격증명을 고르는 값이다(구글의 Android·iOS 클라이언트
/// ID 가 별개다). `includeIfNull: false` 라 해당 흐름에 없는 필드는 바디에서 통째로 빠진다.
@JsonSerializable(createFactory: false, includeIfNull: false)
class LoginRequest {
  /// 웹: 인가 코드 + PKCE 검증값. 앱은 안드로이드에서 커스텀 스킴이 막혀 이 흐름을 쓰지 않는다.
  const LoginRequest.google({
    required this.code,
    required this.codeVerifier,
    this.clientType,
  }) : accessToken = null;

  /// 카카오: 공식 SDK 가 앱에서 받은 액세스 토큰.
  const LoginRequest.kakao({required this.accessToken, this.clientType})
    : code = null,
      codeVerifier = null;

  /// 구글(앱): 공식 SDK(google_sign_in)가 받은 ID 토큰. 카카오와 같은 토큰 채널(`accessToken`)로
  /// 보낸다 — 서버는 이 값을 tokeninfo 로 검증해 신원을 확인한다.
  const LoginRequest.googleToken({required String idToken, this.clientType})
    : accessToken = idToken,
      code = null,
      codeVerifier = null;

  final String? code;
  final String? codeVerifier;
  final String? accessToken;
  final ClientType? clientType;

  Map<String, dynamic> toJson() => _$LoginRequestToJson(this);
}

@JsonSerializable(createToJson: false)
class LoginResponse {
  const LoginResponse({
    required this.userId,
    required this.nickname,
    required this.newUser,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) =>
      _$LoginResponseFromJson(json);

  final int userId;
  final String nickname;
  final bool newUser;
}

/// `GET /api/users/me` — 서버 DTO 에 `email` 이 없다(사양서 R4-1). 웹 타입 선언에만 있는 사문이다.
@JsonSerializable(createToJson: false)
class UserProfile {
  const UserProfile({
    required this.userId,
    required this.nickname,
    required this.createdAt,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) =>
      _$UserProfileFromJson(json);

  final int userId;
  final String nickname;

  @KstDateTimeConverter()
  final DateTime createdAt;
}

/// `PUT /api/users/me/nickname` — 2~20자.
@JsonSerializable(createFactory: false)
class ChangeNicknameRequest {
  const ChangeNicknameRequest({required this.nickname});

  final String nickname;

  Map<String, dynamic> toJson() => _$ChangeNicknameRequestToJson(this);
}

@JsonSerializable(createToJson: false)
class ChangeNicknameResponse {
  const ChangeNicknameResponse({required this.userId, required this.nickname});

  factory ChangeNicknameResponse.fromJson(Map<String, dynamic> json) =>
      _$ChangeNicknameResponseFromJson(json);

  final int userId;
  final String nickname;
}

/// `POST /api/feedbacks` — 20~1000자. 응답은 201 + `code: SUCCESS` + `data: null`.
@JsonSerializable(createFactory: false)
class SendFeedbackRequest {
  const SendFeedbackRequest({required this.content});

  final String content;

  Map<String, dynamic> toJson() => _$SendFeedbackRequestToJson(this);
}
