import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/auth/session_store.dart';
import '../../models/enums.dart';
import '../mypage/user_repository.dart';
import 'auth_repository.dart';
import 'social_login.dart';

/// `idle → authorizing(외부 브라우저) → exchanging(POST /login) → authenticated | failed`
/// 여기에 부팅 시 세션 복구 구간인 [restoring] 이 앞에 붙는다.
enum AuthStatus {
  restoring,
  idle,
  authorizing,
  exchanging,
  authenticated,
  failed,
}

/// 웹의 `AuthProvider.user` 와 같은 최소 신원(§2.3.2). 프로필 상세는 마이페이지가 따로 읽는다.
class AuthUser {
  const AuthUser({required this.userId, required this.nickname});

  final int userId;
  final String nickname;
}

class AuthState {
  const AuthState({
    required this.status,
    this.user,
    this.provider,
    this.errorMessage,
  });

  const AuthState.restoring() : this(status: AuthStatus.restoring);

  const AuthState.signedOut() : this(status: AuthStatus.idle);

  final AuthStatus status;
  final AuthUser? user;

  /// 진행 중이거나 실패한 제공자. 교환 오버레이와 오류 배너가 읽는다.
  final SocialProvider? provider;
  final String? errorMessage;

  bool get isLoading => status == AuthStatus.restoring;

  bool get isAuthenticated => user != null;

  bool get isBusy =>
      status == AuthStatus.authorizing || status == AuthStatus.exchanging;
}

class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() {
    final expiry = ref.watch(sessionExpiryProvider);
    expiry.addListener(_onSessionExpired);
    ref.onDispose(() => expiry.removeListener(_onSessionExpired));

    scheduleMicrotask(_restore);
    return const AuthState.restoring();
  }

  /// 앱 부팅. 저장된 세션이 있으면 `/api/users/me` 로 인증을 복구하고, 실패하면 세션을 폐기한다.
  /// 어떤 오류든 미인증으로 간주한다(사양서 §2.3.2 — 401 을 따로 구분하지 않는다).
  Future<void> _restore() async {
    // 콜드 스타트는 인가 세션이 이미 소실됐다는 뜻이다. 남은 PKCE 비밀값은 전부 오염값이다.
    await ref.read(oauthSecretsProvider).clear();

    final session = ref.read(sessionStoreProvider);
    if (!session.hasSession) {
      state = const AuthState.signedOut();
      return;
    }
    try {
      final profile = await ref.read(userRepositoryProvider).getMe();
      state = AuthState(
        status: AuthStatus.authenticated,
        user: AuthUser(userId: profile.userId, nickname: profile.nickname),
      );
    } catch (_) {
      await session.clear();
      state = const AuthState.signedOut();
    }
  }

  Future<void> login(SocialProvider provider) async {
    if (state.isBusy) return;
    state = AuthState(status: AuthStatus.authorizing, provider: provider);

    try {
      // 인가 흐름은 제공자별로 다르다(카카오 SDK 토큰 / 구글 인가 코드). 서버 교환에 넣을
      // LoginRequest 를 통째로 돌려받으므로 여기서는 제공자를 구분하지 않는다.
      final request = await ref.read(socialLoginProvider).authorize(provider);

      state = AuthState(status: AuthStatus.exchanging, provider: provider);
      final response = await ref
          .read(authRepositoryProvider)
          .login(provider, request);

      // 세션은 SessionInterceptor 가 Set-Cookie 에서 회수해 저장했다. 화면 이동은 하지 않는다 —
      // 인증 상태가 바뀌면 라우터의 redirect 가 /market 으로 보낸다.
      state = AuthState(
        status: AuthStatus.authenticated,
        user: AuthUser(userId: response.userId, nickname: response.nickname),
      );
    } on SocialLoginException catch (error) {
      state = _failed(provider, error.message);
    } on ApiException catch (error) {
      state = _failed(provider, error.userMessage);
    } catch (_) {
      state = _failed(provider, '로그인 처리 중 오류가 발생했습니다.');
    }
  }

  /// 서버 호출 실패와 무관하게 로컬 인증 상태를 비운다(사양서 §2.3.1-3). 서버 로그아웃은 멱등이다.
  Future<void> logout() async {
    try {
      await ref.read(authRepositoryProvider).logout();
    } on ApiException {
      // 네트워크가 끊겨도 로그아웃은 사용자 입장에서 성공해야 한다.
    }
    await ref.read(sessionStoreProvider).clear();
    state = const AuthState.signedOut();
  }

  /// 닉네임 변경 성공 후 로컬 신원을 갱신한다. 앱바·마이페이지가 이 값을 읽는다.
  void updateNickname(String nickname) {
    final user = state.user;
    if (user == null) return;
    state = AuthState(
      status: state.status,
      user: AuthUser(userId: user.userId, nickname: nickname),
    );
  }

  /// 회원 탈퇴(사양서 R11). 성공하면 `null`, 실패하면 사용자에게 보일 메시지를 돌려준다.
  ///
  /// 서버가 전 기기 세션을 지우고 만료 쿠키를 내리므로 [SessionInterceptor] 가 로컬 세션도 함께
  /// 폐기하지만, 쿠키가 오지 않는 경우를 대비해 여기서 한 번 더 지운다. 화면 이동은 하지 않는다 —
  /// 인증 상태가 비면 redirect 가 `/login` 으로 보낸다.
  Future<String?> deleteAccount() async {
    try {
      await ref.read(userRepositoryProvider).deleteAccount();
    } on ApiException catch (error) {
      return error.userMessage;
    }
    await ref.read(sessionStoreProvider).clear();
    state = const AuthState.signedOut();
    return null;
  }

  /// 오류 배너를 닫는다.
  void dismissError() {
    if (state.status == AuthStatus.failed) state = const AuthState.signedOut();
  }

  /// 401 인터셉터가 세션을 폐기한 뒤 방송한다. 세션 폐기는 이미 끝나 있다.
  void _onSessionExpired() {
    if (!state.isAuthenticated) return;
    state = const AuthState.signedOut();
  }

  AuthState _failed(SocialProvider provider, String message) =>
      AuthState(status: AuthStatus.failed, provider: provider, errorMessage: message);
}

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
