import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/format/hangul.dart';
import '../../core/realtime/ticker_store.dart';
import '../../models/exchange_coin.dart';
import 'exchange_coin_repository.dart';

/// 코인 하나의 정적 스냅샷 + 검색 색인.
///
/// 색인은 목록을 받은 시점에 **1회만** 만든다(사양서 §4.2.5) — 시세가 갱신돼도 이름은 바뀌지
/// 않는다. 9단위의 실시간 시세는 이 객체가 아니라 `TickerStore` 가 심볼별로 들고 간다.
class CoinEntry {
  CoinEntry(this.coin) : index = HangulIndex(coin.coinName);

  final ExchangeCoin coin;
  final HangulIndex index;

  String get symbol => coin.coinSymbol;

  String get name => coin.coinName;

  double get price => coin.price;

  /// 비율이다(0.0123 = 1.23%).
  double get changeRate => coin.changeRate;

  double get volume => coin.volume;
}

/// 거래소별 코인 카탈로그. `family` 는 autoDispose 가 아니므로 거래소 탭을 오가도 다시 받지
/// 않는다. 주문 대상 해석(`exchangeCoinId`)도 이 캐시를 읽는다.
final marketCoinsProvider = FutureProvider.family<List<CoinEntry>, int>((
  ref,
  exchangeId,
) async {
  final coins = await ref.watch(exchangeCoinRepositoryProvider).getCoins(
    exchangeId,
  );
  return [for (final coin in coins) CoinEntry(coin)];
});

enum MarketSortKey {
  name('이름'),
  price('현재가'),
  change('변동률'),
  volume('거래대금');

  const MarketSortKey(this.label);

  final String label;
}

/// `changeRate == 0` 인 코인은 상승·하락 어느 쪽에도 잡히지 않는다(사양서 §4.2.4).
enum MarketFilter {
  all('전체'),
  rising('상승'),
  falling('하락');

  const MarketFilter(this.label);

  final String label;

  bool accepts(double changeRate) => switch (this) {
    MarketFilter.all => true,
    MarketFilter.rising => changeRate > 0,
    MarketFilter.falling => changeRate < 0,
  };
}

/// 목록의 표시 조건. 티커가 들어와도 이 값은 바뀌지 않는다 — 9단위의 재정렬 스로틀이 이
/// 객체를 그대로 재사용한다.
class MarketView {
  const MarketView({
    this.query = '',
    this.filter = MarketFilter.all,
    this.sortKey = MarketSortKey.volume,
    this.descending = true,
  });

  final String query;
  final MarketFilter filter;

  /// 기본 정렬은 거래대금 내림차순이다(§4.2.2).
  final MarketSortKey sortKey;
  final bool descending;

  MarketView withQuery(String query) => MarketView(
    query: query,
    filter: filter,
    sortKey: sortKey,
    descending: descending,
  );

  MarketView withFilter(MarketFilter filter) => MarketView(
    query: query,
    filter: filter,
    sortKey: sortKey,
    descending: descending,
  );

  /// 같은 키를 다시 누르면 방향을 뒤집고, 다른 키를 누르면 그 키로 바꾸며 방향을 `desc` 로
  /// 되돌린다(§4.2.2).
  MarketView sortBy(MarketSortKey key) => MarketView(
    query: query,
    filter: filter,
    sortKey: key,
    descending: key == sortKey ? !descending : true,
  );

  /// 거래소 전환 — 검색어와 필터는 초기화하고 **정렬은 유지한다**(§4.1.5).
  MarketView forExchangeSwitch() =>
      MarketView(sortKey: sortKey, descending: descending);
}

/// 실시간 시세 조회. `TickerStore.quote` 를 그대로 넘긴다. 목록 밖 심볼이면 null 이고 그때는
/// REST 스냅샷 값을 쓴다.
typedef QuoteLookup = CoinRowState? Function(String symbol);

/// 검색 → 필터 → 정렬 순으로 적용한다(§4.2.4). 위젯 없이 테스트한다.
///
/// 필터와 정렬은 **최신 시세**를 기준으로 판정해야 한다. 재정렬 캐던스(1초 스로틀 + 스크롤 중
/// 동결)는 호출부가 정한다 — 이 함수는 프레임마다 불리지 않는다.
List<CoinEntry> applyMarketView(
  List<CoinEntry> coins,
  MarketView view, {
  QuoteLookup? quote,
}) {
  final query = HangulQuery(view.query);
  double changeOf(CoinEntry e) => quote?.call(e.symbol)?.changeRate ?? e.changeRate;

  final result = [
    for (final entry in coins)
      if (query.matches(entry.symbol, entry.index) &&
          view.filter.accepts(changeOf(entry)))
        entry,
  ];

  final sign = view.descending ? -1 : 1;
  result.sort((a, b) => sign * _compare(a, b, view.sortKey, quote));
  return result;
}

int _compare(CoinEntry a, CoinEntry b, MarketSortKey key, QuoteLookup? quote) {
  if (key == MarketSortKey.name) {
    // 코인명이 아니라 심볼 사전순이다(§4.2.2).
    return a.symbol.compareTo(b.symbol);
  }
  final qa = quote?.call(a.symbol);
  final qb = quote?.call(b.symbol);
  return switch (key) {
    MarketSortKey.price => (qa?.price ?? a.price).compareTo(qb?.price ?? b.price),
    MarketSortKey.change => (qa?.changeRate ?? a.changeRate).compareTo(
      qb?.changeRate ?? b.changeRate,
    ),
    MarketSortKey.volume => (qa?.volume ?? a.volume).compareTo(
      qb?.volume ?? b.volume,
    ),
    MarketSortKey.name => 0,
  };
}
