import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'user.g.dart';

/// `POST /api/auth/{provider}/login`
///
/// 앱은 두 제공자 모두 공식 SDK 가 받은 토큰을 같은 채널(`accessToken`)로 보낸다
/// (`{accessToken, clientType}`). 서버가 받는 인가 코드 흐름(`{code, codeVerifier}`)은
/// 웹 전용이라 앱 모델에 두지 않는다 — 웹은 `frontend/src/lib/auth/social.ts` 가 쓴다.
///
/// [clientType] 은 서버가 플랫폼별 제공자 자격증명을 고르는 값이다.
/// `includeIfNull: false` 라 값이 없는 필드는 바디에서 통째로 빠진다.
@JsonSerializable(createFactory: false, includeIfNull: false)
class LoginRequest {
  /// 카카오: 공식 SDK 가 앱에서 받은 액세스 토큰.
  const LoginRequest.kakao({required this.accessToken, this.clientType});

  /// 구글(앱): 공식 SDK(google_sign_in)가 받은 ID 토큰. 카카오와 같은 토큰 채널(`accessToken`)로
  /// 보낸다 — 서버는 이 값을 tokeninfo 로 검증해 신원을 확인한다.
  const LoginRequest.googleToken({required String idToken, this.clientType})
    : accessToken = idToken;

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
