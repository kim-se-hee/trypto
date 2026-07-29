import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/format/formatters.dart';
import '../../core/format/hangul.dart';
import '../../models/cursor_page.dart';
import '../../models/transfer.dart';
import '../../models/wallet.dart';
import '../market/market_controller.dart';
import '../round/round_controller.dart';
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
/// 기준통화가 맨 앞이다 — 요약 카드가 [WalletSnapshot.baseAsset] 로 읽는다.
/// 화면에 보이는 순서는 [sortWalletAssets] 가 정한다. 기준통화도 함께 정렬된다.
List<WalletAsset> buildWalletAssets({
  required WalletBalances balances,
  required List<CoinEntry> coins,
}) {
  final byCoinId = {
    for (final balance in balances.balances) balance.coinId: balance,
  };
  final base = balances.baseCurrencySymbol;
  final baseName = base == 'KRW' ? '원화' : base;

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
  ];
}

/// 보유 자산 목록의 정렬 기준. 웹 표의 네 열과 같다(WalletAssetTable.tsx:95-100).
enum WalletSortKey {
  name('코인'),
  total('보유수량'),
  available('사용가능'),
  locked('잠금');

  const WalletSortKey(this.label);

  final String label;
}

/// 정렬 상태. 같은 키를 다시 고르면 방향을 뒤집고, 다른 키를 고르면 그 키로 바꾸며 방향을
/// 내림차순으로 되돌린다(웹 useSort.ts:34-41). 기본은 평가액 내림차순이다.
class WalletSort {
  const WalletSort({
    this.key = WalletSortKey.total,
    this.descending = true,
  });

  final WalletSortKey key;
  final bool descending;

  WalletSort sortBy(WalletSortKey next) =>
      WalletSort(key: next, descending: next == key ? !descending : true);
}

/// 웹 WalletAssetTable 의 4키 비교식(:69-81). `available`·`locked` 는 **수량이 아니라
/// 평가액**이다 — 값이 큰 코인의 잠금 1개가 잔코인 1,000개보다 위에 온다.
List<WalletAsset> sortWalletAssets(List<WalletAsset> assets, WalletSort sort) {
  final sign = sort.descending ? -1 : 1;
  return [...assets]..sort((a, b) => sign * _compareAssets(a, b, sort.key));
}

int _compareAssets(WalletAsset a, WalletAsset b, WalletSortKey key) =>
    switch (key) {
      // 코인명이 아니라 심볼 사전순이다.
      WalletSortKey.name => a.symbol.compareTo(b.symbol),
      WalletSortKey.total => a.totalValue.compareTo(b.totalValue),
      WalletSortKey.available => (a.available * a.currentPrice).compareTo(
        b.available * b.currentPrice,
      ),
      WalletSortKey.locked => (a.locked * a.currentPrice).compareTo(
        b.locked * b.currentPrice,
      ),
    };

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

/// 라운드 지갑이 걸쳐 있는 거래소들의 상장 코인 집합(거래소 → coinId). 도착 거래소가 취급하지
/// 않는 코인은 서버가 `COIN_NOT_LISTED_ON_EXCHANGE` 로 막으므로 후보 단계에서 걸러낸다
/// (웹 RoundProvider `isCoinListed`).
///
/// 아직 목록이 오지 않은 거래소는 키가 없다 — 웹과 같이 '미상장' 으로 본다. 카탈로그는
/// `marketCoinsProvider` 가 거래소별로 캐시하고 있어 지갑 화면이 다시 내려받지 않는다.
final listedCoinIdsProvider = Provider<Map<int, Set<int>>>((ref) {
  final round = ref.watch(
    roundControllerProvider.select((state) => state.activeRound),
  );
  if (round == null) return const <int, Set<int>>{};

  return <int, Set<int>>{
    for (final wallet in round.wallets)
      if (ref.watch(marketCoinsProvider(wallet.exchangeId)).valueOrNull
          case final coins?)
        wallet.exchangeId: <int>{
          for (final entry in coins) entry.coin.coinId,
        },
  };
});
