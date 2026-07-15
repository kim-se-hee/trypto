import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/user.dart';

class UserRepository {
  const UserRepository(this._dio);

  final Dio _dio;

  /// 부팅 시 세션 복구에 쓴다. 성공하면 로그인 상태, 401 이면 비로그인이다.
  Future<UserProfile> getMe() => apiCall(() async {
    final response = await _dio.get('/api/users/me');
    return UserProfile.fromJson(response.data as Map<String, dynamic>);
  });

  /// 2~20자. 입력 단계에서 강제한다(웹은 제출 시점에 실패한다 — R9).
  Future<ChangeNicknameResponse> changeNickname(String nickname) =>
      apiCall(() async {
        final response = await _dio.put(
          '/api/users/me/nickname',
          data: ChangeNicknameRequest(nickname: nickname).toJson(),
        );
        return ChangeNicknameResponse.fromJson(
          response.data as Map<String, dynamic>,
        );
      });

  /// 회원 탈퇴(사양서 R11). 웹에는 호출부가 없다. 서버가 전 기기 세션을 지우고 만료 쿠키를
  /// 내리므로 [SessionInterceptor] 가 로컬 세션도 함께 폐기한다.
  /// 탈퇴 후 30일 재가입 제한이 있어 재로그인 시 403 `SIGNUP_RESTRICTED` 가 날 수 있다.
  Future<void> deleteAccount() =>
      apiCall(() async => _dio.delete<void>('/api/users/me'));
}

final userRepositoryProvider = Provider<UserRepository>(
  (ref) => UserRepository(ref.watch(dioProvider)),
);

/// 마이페이지의 가입일. 부팅 시 세션 복구가 읽는 것과 같은 엔드포인트지만, 그쪽은 인증 여부만
/// 확인하고 프로필을 보관하지 않는다.
final userProfileProvider = FutureProvider<UserProfile>(
  (ref) => ref.watch(userRepositoryProvider).getMe(),
);
