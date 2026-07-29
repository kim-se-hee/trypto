import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/market/market_controller.dart';
import 'package:trypto/features/wallet/transfer_wizard/transfer_draft.dart';
import 'package:trypto/features/wallet/wallet_assets.dart';
import 'package:trypto/models/exchange_coin.dart';
import 'package:trypto/models/wallet.dart';

CoinEntry _coin(int coinId, String symbol, String name, double price) =>
    CoinEntry(
      ExchangeCoin(
        exchangeCoinId: coinId * 10,
        coinId: coinId,
        coinSymbol: symbol,
        coinName: name,
        price: price,
        changeRate: 0,
        volume: 0,
      ),
    );

WalletBalances _balances(
  List<CoinBalance> balances, {
  double available = 0,
  double locked = 0,
  String base = 'KRW',
}) => WalletBalances(
  exchangeId: 1,
  baseCurrencySymbol: base,
  baseCurrencyAvailable: available,
  baseCurrencyLocked: locked,
  balances: balances,
);

final _coins = [
  _coin(1, 'BTC', '비트코인', 50000000),
  _coin(2, 'ETH', '이더리움', 3000000),
  _coin(3, 'XRP', '리플', 700),
];

void main() {
  group('buildWalletAssets', () {
    test('코인 현재가는 상장 목록의 price 다 (웹은 0 고정 — R9-3)', () {
      final assets = buildWalletAssets(
        balances: _balances([
          const CoinBalance(coinId: 1, available: 0.5, locked: 0),
        ], available: 1000000),
        coins: _coins,
      );

      final btc = assets.firstWhere((asset) => asset.symbol == 'BTC');
      expect(btc.currentPrice, 50000000);
      expect(btc.totalValue, 25000000);
      // 총자산이 기준통화 잔고만 반영하는 웹의 결함(1,000,000)이 재현되지 않는다.
      expect(totalAssetValue(assets), 26000000);
    });

    test('기준통화 행이 맨 앞이고 현재가는 1 이다', () {
      final assets = buildWalletAssets(
        balances: _balances(const [], available: 5000, locked: 1000),
        coins: _coins,
      );

      final base = assets.first;
      expect(base.isBase, isTrue);
      expect(base.coinId, isNull);
      expect(base.symbol, 'KRW');
      expect(base.name, '원화');
      expect(base.currentPrice, 1);
      expect(base.total, 6000);
      expect(base.totalValue, 6000);
    });

    test('잔고가 없는 상장 코인도 0 으로 포함한다', () {
      final assets = buildWalletAssets(
        balances: _balances(const []),
        coins: _coins,
      );

      expect(assets, hasLength(4)); // 기준통화 + 코인 3
      expect(assets.every((asset) => !asset.isBase ? !asset.hasBalance : true), isTrue);
    });

    test('코인은 상장 목록 순서를 그대로 둔다 — 표시 순서는 sortWalletAssets 가 정한다', () {
      final assets = buildWalletAssets(
        balances: _balances(const [
          CoinBalance(coinId: 1, available: 0.001, locked: 0), // 50,000
          CoinBalance(coinId: 2, available: 1, locked: 0), // 3,000,000
          CoinBalance(coinId: 3, available: 100, locked: 0), // 70,000
        ]),
        coins: _coins,
      );

      expect([for (final asset in assets) asset.symbol], [
        'KRW',
        'BTC',
        'ETH',
        'XRP',
      ]);
    });

    test('잠금 잔고도 총 수량·평가액에 들어간다', () {
      final assets = buildWalletAssets(
        balances: _balances(const [
          CoinBalance(coinId: 3, available: 100, locked: 50),
        ]),
        coins: _coins,
      );

      final xrp = assets.firstWhere((asset) => asset.symbol == 'XRP');
      expect(xrp.total, 150);
      expect(xrp.totalValue, 105000);
    });
  });

  group('applyWalletFilter', () {
    final assets = buildWalletAssets(
      balances: _balances(const [
        CoinBalance(coinId: 1, available: 0.5, locked: 0), // 25,000,000
        CoinBalance(coinId: 3, available: 1, locked: 0), // 700 → 소액
      ], available: 3000),
      coins: _coins,
    );

    test('심볼·한글명·초성으로 찾는다', () {
      expect(
        [
          for (final asset in applyWalletFilter(
            assets,
            query: 'btc',
            baseCurrency: 'KRW',
          ))
            asset.symbol,
        ],
        ['BTC'],
      );
      expect(
        [
          for (final asset in applyWalletFilter(
            assets,
            query: '리플',
            baseCurrency: 'KRW',
          ))
            asset.symbol,
        ],
        ['XRP'],
      );
      expect(
        [
          for (final asset in applyWalletFilter(
            assets,
            query: 'ㅂㅌ',
            baseCurrency: 'KRW',
          ))
            asset.symbol,
        ],
        ['BTC'],
      );
    });

    test('소액 제외는 평가액 1,000원 미만을 걷어낸다', () {
      final filtered = applyWalletFilter(
        assets,
        hideSmall: true,
        baseCurrency: 'KRW',
      );

      // XRP(700원)와 잔고 0 코인이 빠지고 기준통화·BTC 만 남는다.
      expect([for (final asset in filtered) asset.symbol], ['KRW', 'BTC']);
    });

    test('검색어가 없으면 전량을 돌려준다', () {
      expect(applyWalletFilter(assets, baseCurrency: 'KRW'), hasLength(4));
    });
  });

  group('sortWalletAssets', () {
    // 평가액과 수량의 순서가 서로 다르도록 짠 표다.
    // KRW  가용 500,000 / 잠금 200,000 / 총 700,000
    // BTC  가용  50,000 / 잠금 100,000 / 총 150,000
    // ETH  가용 3,000,000 / 잠금 1,500,000 / 총 4,500,000
    // XRP  가용  70,000 / 잠금 700,000 / 총 770,000
    final assets = buildWalletAssets(
      balances: _balances(
        const [
          CoinBalance(coinId: 1, available: 0.001, locked: 0.002),
          CoinBalance(coinId: 2, available: 1, locked: 0.5),
          CoinBalance(coinId: 3, available: 100, locked: 1000),
        ],
        available: 500000,
        locked: 200000,
      ),
      coins: _coins,
    );

    List<String> symbolsOf(WalletSort sort) => [
      for (final asset in sortWalletAssets(assets, sort)) asset.symbol,
    ];

    test('기본은 평가액 내림차순이고 기준통화도 함께 정렬된다', () {
      expect(symbolsOf(const WalletSort()), ['ETH', 'XRP', 'KRW', 'BTC']);
    });

    test('이름 정렬은 코인명이 아니라 심볼 사전순이다', () {
      expect(
        symbolsOf(const WalletSort(key: WalletSortKey.name, descending: false)),
        ['BTC', 'ETH', 'KRW', 'XRP'],
      );
    });

    test('사용가능·잠금은 수량이 아니라 평가액 기준이다', () {
      // 수량 순이었다면 둘 다 KRW·XRP·ETH·BTC 가 된다.
      expect(symbolsOf(const WalletSort(key: WalletSortKey.available)), [
        'ETH',
        'KRW',
        'XRP',
        'BTC',
      ]);
      expect(symbolsOf(const WalletSort(key: WalletSortKey.locked)), [
        'ETH',
        'XRP',
        'KRW',
        'BTC',
      ]);
    });

    test('오름차순은 내림차순을 뒤집은 것이다', () {
      expect(
        symbolsOf(const WalletSort(key: WalletSortKey.total, descending: false)),
        ['BTC', 'KRW', 'XRP', 'ETH'],
      );
    });

    test('원본 목록은 건드리지 않는다', () {
      final before = [for (final asset in assets) asset.symbol];
      sortWalletAssets(assets, const WalletSort(key: WalletSortKey.name));
      expect([for (final asset in assets) asset.symbol], before);
    });
  });

  group('WalletSort.sortBy', () {
    test('같은 키를 다시 고르면 방향만 뒤집는다', () {
      const sort = WalletSort();
      final ascending = sort.sortBy(WalletSortKey.total);
      expect(ascending.key, WalletSortKey.total);
      expect(ascending.descending, isFalse);
      expect(ascending.sortBy(WalletSortKey.total).descending, isTrue);
    });

    test('다른 키를 고르면 방향이 내림차순으로 돌아간다', () {
      final ascendingName = const WalletSort()
          .sortBy(WalletSortKey.name)
          .sortBy(WalletSortKey.name);
      expect(ascendingName.descending, isFalse);

      final total = ascendingName.sortBy(WalletSortKey.total);
      expect(total.key, WalletSortKey.total);
      expect(total.descending, isTrue);
    });
  });

  group('validateTransferAmount', () {
    test('빈 입력·0·음수는 수량 오류다', () {
      expect(validateTransferAmount('', 10), '수량을 입력해 주세요.');
      expect(validateTransferAmount('0', 10), '수량을 입력해 주세요.');
      expect(validateTransferAmount('abc', 10), '수량을 입력해 주세요.');
    });

    test('가용 잔고를 넘으면 막는다', () {
      expect(validateTransferAmount('10.00000001', 10), '가용 잔고를 초과합니다.');
      expect(validateTransferAmount('10', 10), isNull);
      expect(validateTransferAmount('0.00000001', 10), isNull);
    });

    test('double 오차로 전액 출금이 막히지 않는다', () {
      // 0.1 + 0.2 == 0.30000000000000004 이므로 double 비교였다면 통과하지 못한다.
      expect(validateTransferAmount('0.3', 0.1 + 0.2), isNull);
    });
  });

  group('transferAmountOfRatio', () {
    test('최대는 잔고를 그대로 쓴다', () {
      expect(transferAmountOfRatio(1.23456789, 1), '1.23456789');
    });

    test('비율은 8자리에서 내림한다 — 반올림하면 잔고를 넘을 수 있다', () {
      expect(transferAmountOfRatio(1, 0.25), '0.25');
      expect(transferAmountOfRatio(0.00000003, 0.5), '0.00000001');
      expect(validateTransferAmount(transferAmountOfRatio(3.7, 0.5), 3.7), isNull);
    });
  });
}
