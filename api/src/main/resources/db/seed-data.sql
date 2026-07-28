-- 인수 테스트용 마스터 데이터 시드.
-- 컨텍스트 부팅 시 1회만 적재되고, DatabaseCleanupHook 의 TRUNCATE 대상에서 영구 제외된다.
-- 시나리오는 이 데이터를 수정하지 않는다는 약속하에 동작한다.

INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES
    (1, 'KRW', '원화'),
    (2, 'BTC', '비트코인'),
    (3, 'ETH', '이더리움'),
    (4, 'USDT', '테더');

-- 바이낸스의 기축통화는 USDT 다 (application.yml 의 exchanges 설정과 동일).
-- KRW 로 시드하면 원화 환산이 필요한 시나리오가 환산 없이도 통과해 버려 검증이 헛돈다.
INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate) VALUES
    (1, 'UPBIT', 'DOMESTIC', 1, 0.000500),
    (2, 'BITHUMB', 'DOMESTIC', 1, 0.002500),
    (3, 'BINANCE', 'OVERSEAS', 4, 0.001000);

-- 빗썸(2)에 ETH 는 상장하지 않는다. 거래소마다 상장 코인이 다른 상황을 시나리오에서 재현하기 위함이다.
-- 빗썸 USDT(13) 는 USDT 시드머니·긴급 충전의 원화 환산 시세 소스다.
-- 업비트가 아닌 빗썸에 붙인 이유: find-exchange-coins 시나리오가 업비트 상장 코인 개수를 단언한다.
-- status 는 기본값이 없는 ENUM 이라 생략하면 MySQL 이 목록의 첫 값인 SUSPENDED 를 넣는다.
-- 거래 정지 상태로 시드되면 주문·평가가 전부 막히므로 TRADING 을 명시한다.
INSERT IGNORE INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name, status) VALUES
    (10, 1, 2, '비트코인', 'TRADING'),
    (11, 1, 3, '이더리움', 'TRADING'),
    (12, 2, 2, '비트코인', 'TRADING'),
    (13, 2, 4, '테더', 'TRADING');
