import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/enums.dart';
import '../../models/user.dart';

class AuthRepository {
  const AuthRepository(this._dio);

  final Dio _dio;

  /// 세션 쿠키는 응답의 `Set-Cookie` 에서 [SessionInterceptor] 가 회수해 저장한다.
  /// 이 401(`SOCIAL_LOGIN_FAILED`)은 세션 만료가 아니므로 저장된 세션을 건드리지 않는다.
  Future<LoginResponse> login(
    SocialProvider provider,
    LoginRequest request,
  ) => apiCall(() async {
    final response = await _dio.post(
      '/api/auth/${provider.wire}/login',
      data: request.toJson(),
    );
    return LoginResponse.fromJson(response.data as Map<String, dynamic>);
  });

  /// 바디를 넘기지 않는다 → `Content-Type` 이 붙지 않는다(사양서 §1.11.7).
  /// 세션이 없어도 성공하며, 서버가 만료 쿠키를 내려 세션이 폐기된다.
  Future<void> logout() =>
      apiCall(() async => _dio.post<void>('/api/auth/logout'));
}

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(ref.watch(dioProvider)),
);
