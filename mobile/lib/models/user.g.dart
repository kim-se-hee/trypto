// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Map<String, dynamic> _$LoginRequestToJson(LoginRequest instance) =>
    <String, dynamic>{
      'code': instance.code,
      'codeVerifier': instance.codeVerifier,
      'clientType': ?_$ClientTypeEnumMap[instance.clientType],
    };

const _$ClientTypeEnumMap = {
  ClientType.web: 'web',
  ClientType.android: 'android',
  ClientType.ios: 'ios',
};

LoginResponse _$LoginResponseFromJson(Map<String, dynamic> json) =>
    LoginResponse(
      userId: (json['userId'] as num).toInt(),
      nickname: json['nickname'] as String,
      newUser: json['newUser'] as bool,
    );

UserProfile _$UserProfileFromJson(Map<String, dynamic> json) => UserProfile(
  userId: (json['userId'] as num).toInt(),
  nickname: json['nickname'] as String,
  createdAt: const KstDateTimeConverter().fromJson(json['createdAt'] as String),
);

Map<String, dynamic> _$ChangeNicknameRequestToJson(
  ChangeNicknameRequest instance,
) => <String, dynamic>{'nickname': instance.nickname};

ChangeNicknameResponse _$ChangeNicknameResponseFromJson(
  Map<String, dynamic> json,
) => ChangeNicknameResponse(
  userId: (json['userId'] as num).toInt(),
  nickname: json['nickname'] as String,
);

Map<String, dynamic> _$SendFeedbackRequestToJson(
  SendFeedbackRequest instance,
) => <String, dynamic>{'content': instance.content};
