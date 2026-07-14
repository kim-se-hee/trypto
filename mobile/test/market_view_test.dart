import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/market/market_controller.dart';
import 'package:trypto/models/exchange_coin.dart';

/// 검색·필터·정렬은 위젯 없이 고정한다. 특히 초성은 `startsWith`, 자모는 `contains` 라는
/// 비대칭(사양서 §4.2.5)이 깨지면 사용자는 "안 나온다" 로만 체감하고 원인을 못 찾는다.
void main() {
  CoinEntry coin(
    String symbol,
    String name, {
    double price = 100,
    double changeRate = 0,
    double volume = 0,
  }) => CoinEntry(
    ExchangeCoin(
      exchangeCoinId: 1,
      coinId: 1,
      coinSymbol: symbol,
      coinName: name,
      price: price,
      changeRate: changeRate,
      volume: volume,
    ),
  );

  final btc = coin('BTC', '비트코인', price: 96000000, changeRate: 0.012, volume: 900);
  final eth = coin('ETH', '이더리움', price: 5300000, changeRate: -0.004, volume: 500);
  final xrp = coin('XRP', '리플', price: 3200, changeRate: 0, volume: 700);
  final doge = coin('DOGE', '도지코인', price: 300, changeRate: 0.08, volume: 100);
  final all = [btc, eth, xrp, doge];

  List<String> symbols(MarketView view) =>
      applyMarketView(all, view).map((entry) => entry.symbol).toList();

  group('검색', () {
    test('심볼 부분 일치 (대소문자 무시)', () {
      expect(symbols(const MarketView(query: 'bt')), ['BTC']);
      expect(symbols(const MarketView(query: 'og')), ['DOGE']);
    });

    test('초성 질의는 앞부분 일치다', () {
      expect(symbols(const MarketView(query: 'ㅂㅌ')), ['BTC']);
      expect(symbols(const MarketView(query: 'ㄷㅈ')), ['DOGE']);
    });

    test('초성 질의가 중간부터 맞는 것은 잡지 않는다 (contains 가 아니다)', () {
      // '도지코인' → ㄷㅈㅋㅇ. 'ㅈㅋ' 는 contains 면 잡히고 startsWith 면 잡히지 않는다.
      expect(symbols(const MarketView(query: 'ㅈㅋ')), isEmpty);
    });

    test('자모 질의는 부분 일치다', () {
      expect(symbols(const MarketView(query: '비트')), ['BTC']);
      expect(symbols(const MarketView(query: '코인')), ['BTC', 'DOGE']);
    });

    test('조합 중인 글자도 맞아떨어진다', () {
      // '빝' → ㅂㅣㅌ 은 '비트코인' → ㅂㅣㅌㅡ… 의 앞부분이다.
      expect(symbols(const MarketView(query: '빝')), ['BTC']);
    });

    test('빈 질의는 전부 통과시킨다', () {
      expect(symbols(const MarketView()).length, all.length);
    });
  });

  group('필터', () {
    test('상승은 changeRate > 0 만 남긴다', () {
      expect(
        symbols(const MarketView(filter: MarketFilter.rising)).toSet(),
        {'BTC', 'DOGE'},
      );
    });

    test('하락은 changeRate < 0 만 남긴다', () {
      expect(symbols(const MarketView(filter: MarketFilter.falling)), ['ETH']);
    });

    test('changeRate == 0 은 상승·하락 어느 쪽에도 잡히지 않는다', () {
      expect(
        symbols(const MarketView(filter: MarketFilter.rising)),
        isNot(contains('XRP')),
      );
      expect(
        symbols(const MarketView(filter: MarketFilter.falling)),
        isNot(contains('XRP')),
      );
      expect(symbols(const MarketView()), contains('XRP'));
    });
  });

  group('정렬', () {
    test('기본값은 거래대금 내림차순이다', () {
      expect(const MarketView().sortKey, MarketSortKey.volume);
      expect(const MarketView().descending, isTrue);
      expect(symbols(const MarketView()), ['BTC', 'XRP', 'ETH', 'DOGE']);
    });

    test('이름 정렬은 한글명이 아니라 심볼 사전순이다', () {
      expect(
        symbols(const MarketView(sortKey: MarketSortKey.name, descending: false)),
        ['BTC', 'DOGE', 'ETH', 'XRP'],
      );
    });

    test('현재가·변동률 정렬', () {
      expect(
        symbols(const MarketView(sortKey: MarketSortKey.price)),
        ['BTC', 'ETH', 'XRP', 'DOGE'],
      );
      expect(
        symbols(const MarketView(sortKey: MarketSortKey.change)),
        ['DOGE', 'BTC', 'XRP', 'ETH'],
      );
    });

    test('같은 키를 다시 누르면 방향만 뒤집는다', () {
      final toggled = const MarketView().sortBy(MarketSortKey.volume);
      expect(toggled.sortKey, MarketSortKey.volume);
      expect(toggled.descending, isFalse);
      expect(toggled.sortBy(MarketSortKey.volume).descending, isTrue);
    });

    test('다른 키를 누르면 그 키로 바꾸고 방향을 desc 로 되돌린다', () {
      final ascending = const MarketView().sortBy(MarketSortKey.volume);
      final switched = ascending.sortBy(MarketSortKey.price);
      expect(switched.sortKey, MarketSortKey.price);
      expect(switched.descending, isTrue);
    });
  });

  group('거래소 전환', () {
    test('검색어와 필터는 초기화하고 정렬은 유지한다', () {
      const before = MarketView(
        query: '비트',
        filter: MarketFilter.rising,
        sortKey: MarketSortKey.change,
        descending: false,
      );
      final after = before.forExchangeSwitch();

      expect(after.query, isEmpty);
      expect(after.filter, MarketFilter.all);
      expect(after.sortKey, MarketSortKey.change);
      expect(after.descending, isFalse);
    });
  });

  group('검색 → 필터 → 정렬 순서', () {
    test('세 조건이 겹쳐도 결과가 좁혀진다', () {
      expect(
        symbols(
          const MarketView(
            query: '코인',
            filter: MarketFilter.rising,
            sortKey: MarketSortKey.change,
          ),
        ),
        ['DOGE', 'BTC'],
      );
    });
  });
}
