import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'user.g.dart';

/// `POST /api/auth/{provider}/login`
///
/// [clientType] 은 백엔드가 받기로 한 필드다(계획서 §9-2 — 구글의 Android·iOS 클라이언트 ID 가
/// 별개라 서버가 플랫폼을 알아야 토큰 교환의 client_id 를 맞출 수 있다).
/// **현행 서버 `LoginRequest` 에는 아직 이 필드가 없으므로** null 이면 바디에서 통째로 뺀다.
/// 값을 채우는 플랫폼 판별은 인증 단위에서 붙인다.
@JsonSerializable(createFactory: false, includeIfNull: false)
class LoginRequest {
  const LoginRequest({
    required this.code,
    required this.codeVerifier,
    this.clientType,
  });

  final String code;
  final String codeVerifier;
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
