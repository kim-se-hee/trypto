import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/format/formatters.dart';
import '../../core/format/hangul.dart';
import '../../models/cursor_page.dart';
import '../../models/transfer.dart';
import '../../models/wallet.dart';
import '../market/market_controller.dart';
import 'transfer_repository.dart';
import 'wallet_repository.dart';

/// 지갑 목록의 한 행. 잔고 응답에는 심볼도 이름도 현재가도 없다 — 거래소 코인 목록과
/// `coinId` 로 합쳐야 화면이 된다.
class WalletAsset {
  const WalletAsset({
    required this.coinId,
    required this.symbol,
    required this.name,
    required this.index,
    required this.available,
    required this.locked,
    required this.currentPrice,
  });

  /// 기준통화 행은 `null` 이다. 기준통화는 송금할 수 없다(사양서 §5.2.5).
  final int? coinId;

  final String symbol;
  final String name;
  final HangulIndex index;
  final double available;
  final double locked;

  /// 웹은 코인의 이 값을 **0 으로 고정**해 총자산·환산액·소액 제외·정렬이 전부 무의미해졌다
  /// (사양서 R9-3). 상장 목록(`GET /api/exchanges/{id}/coins`)의 `price` 를 그대로 쓴다.
  final double currentPrice;

  bool get isBase => coinId == null;

  double get total => available + locked;

  double get totalValue => total * currentPrice;

  bool get hasBalance => total > 0;
}

/// 잔고 + 상장 코인 전량. 잔고가 없는 코인도 0 으로 포함한다(사양서 §5.2.2-3).
/// 기준통화가 맨 앞, 이어서 평가액 내림차순이다.
List<WalletAsset> buildWalletAssets({
  required WalletBalances balances,
  required List<CoinEntry> coins,
}) {
  final byCoinId = {
    for (final balance in balances.balances) balance.coinId: balance,
  };
  final base = balances.baseCurrencySymbol;
  final baseName = base == 'KRW' ? '원화' : base;

  final assets = [
    for (final entry in coins)
      WalletAsset(
        coinId: entry.coin.coinId,
        symbol: entry.symbol,
        name: entry.name,
        index: entry.index,
        available: byCoinId[entry.coin.coinId]?.available ?? 0,
        locked: byCoinId[entry.coin.coinId]?.locked ?? 0,
        currentPrice: entry.price,
      ),
  ]..sort((a, b) {
    final byValue = b.totalValue.compareTo(a.totalValue);
    return byValue != 0 ? byValue : a.symbol.compareTo(b.symbol);
  });

  return [
    WalletAsset(
      coinId: null,
      symbol: base,
      name: baseName,
      index: HangulIndex(baseName),
      available: balances.baseCurrencyAvailable,
      locked: balances.baseCurrencyLocked,
      currentPrice: 1,
    ),
    ...assets,
  ];
}

/// 검색(심볼·한글명) + 소액 제외(KRW 1,000 / USDT 1 미만). 기준통화 행도 같은 규칙을 받는다.
List<WalletAsset> applyWalletFilter(
  List<WalletAsset> assets, {
  String query = '',
  bool hideSmall = false,
  required String baseCurrency,
}) {
  final search = HangulQuery(query);
  final threshold = smallAmountThreshold(baseCurrency);

  return [
    for (final asset in assets)
      if (search.matches(asset.symbol, asset.index) &&
          (!hideSmall || asset.totalValue >= threshold))
        asset,
  ];
}

/// 거래소 총 자산 = Σ((사용가능 + 잠금) × 현재가). 기준통화 포함.
double totalAssetValue(List<WalletAsset> assets) =>
    assets.fold(0, (sum, asset) => sum + asset.totalValue);

class WalletSnapshot {
  const WalletSnapshot({
    required this.baseCurrency,
    required this.assets,
    required this.recentTransfers,
  });

  final String baseCurrency;
  final List<WalletAsset> assets;

  /// 자산 상세 시트가 코인별로 걸러 쓴다. 전체 내역은 별도 화면이 커서로 읽는다.
  final List<TransferHistoryItem> recentTransfers;

  double get totalValue => totalAssetValue(assets);

  WalletAsset get baseAsset => assets.first;
}

typedef WalletKey = ({int exchangeId, int walletId});

/// 잔고·상장 코인·최근 송금을 한 번에 읽는다(사양서 §5.2.2). 코인 목록은 마켓 탭이 이미
/// 받아 둔 카탈로그를 그대로 재사용한다 — 같은 거래소를 두 번 내려받지 않는다.
final walletSnapshotProvider = FutureProvider.family<WalletSnapshot, WalletKey>(
  (ref, key) async {
    final results = await Future.wait<Object?>([
      ref.watch(walletRepositoryProvider).getBalances(key.walletId),
      ref.watch(marketCoinsProvider(key.exchangeId).future),
      ref
          .watch(transferRepositoryProvider)
          .getTransferHistory(walletId: key.walletId, size: 20),
    ]);

    final balances = results[0] as WalletBalances;
    final coins = results[1] as List<CoinEntry>;
    final transfers = results[2] as CursorPage<TransferHistoryItem>;

    return WalletSnapshot(
      baseCurrency: balances.baseCurrencySymbol,
      assets: buildWalletAssets(balances: balances, coins: coins),
      recentTransfers: transfers.content,
    );
  },
);
