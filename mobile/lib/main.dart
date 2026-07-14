import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/auth/session_store.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 세션을 먼저 읽어 들인다. 인터셉터의 onRequest 는 보안 저장소를 await 하지 않으므로
  // (요청마다 플랫폼 채널을 왕복하면 프레임에 얹힌다) 첫 요청 전에 동기 캐시가 채워져 있어야
  // 한다. 읽기 실패는 '세션 없음' 으로 강등된다.
  final sessionStore = SessionStore();
  await sessionStore.load();

  runApp(
    ProviderScope(
      overrides: [sessionStoreProvider.overrideWithValue(sessionStore)],
      child: const TryptoApp(),
    ),
  );
}
