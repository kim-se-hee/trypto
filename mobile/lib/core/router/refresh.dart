import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/auth_controller.dart';
import '../../features/round/round_controller.dart';

/// provider 의 변경을 `Listenable` 로 옮기는 다리.
class _ProviderBridge<T> extends ChangeNotifier {
  _ProviderBridge(Ref ref, ProviderListenable<T> provider) {
    ref.listen<T>(provider, (_, _) => notifyListeners());
  }
}

/// go_router 의 `refreshListenable`. **auth·round 둘만** 물린다 — 티커를 연결하면 초당 수십 번
/// redirect 가 재평가된다(계획서 §4.4).
final routerRefreshProvider = Provider<Listenable>((ref) {
  final auth = _ProviderBridge(ref, authControllerProvider);
  final round = _ProviderBridge(ref, roundControllerProvider);
  ref.onDispose(() {
    auth.dispose();
    round.dispose();
  });
  return Listenable.merge([auth, round]);
});
