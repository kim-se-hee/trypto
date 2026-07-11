-- 부하테스트 시드
-- Hibernate DDL(create) 이후 spring.sql.init으로 실행됨
-- k6 시나리오: walletId 1~1000, exchangeCoinId 1~10
-- DB 모델: wallet UK(round_id, exchange_id), round 는 user 당 round_number 유니크
--         → 1000 wallet 을 위해 user/round 도 1000개씩 1:1 매핑

SET SESSION cte_max_recursion_depth = 5000;

-- 1=KRW(quote), 2~11=거래 코인 10종
INSERT INTO coin (coin_id, symbol, name) VALUES
  (1,  'KRW',   'Korean Won'),
  (2,  'BTC',   'Bitcoin'),
  (3,  'ETH',   'Ethereum'),
  (4,  'XRP',   'Ripple'),
  (5,  'SOL',   'Solana'),
  (6,  'ADA',   'Cardano'),
  (7,  'DOGE',  'Dogecoin'),
  (8,  'AVAX',  'Avalanche'),
  (9,  'DOT',   'Polkadot'),
  (10, 'LINK',  'Chainlink'),
  (11, 'POL', 'Polygon');

INSERT INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate)
VALUES (1, 'UPBIT', 'DOMESTIC', 1, 0.000500);

-- exchange_coin_id 1~10 → coin_id 2~11
-- display_name 은 base 값과 동일하게 둔다 — collector EngineInboxPublisher 가
-- NormalizedTicker.base() 를 displayName 필드로 보내고, engine ExchangeCoinResolver 는
-- (exchange_name, display_name) 으로 매핑하기 때문.
INSERT INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name) VALUES
  (1,  1, 2,  'BTC'),
  (2,  1, 3,  'ETH'),
  (3,  1, 4,  'XRP'),
  (4,  1, 5,  'SOL'),
  (5,  1, 6,  'ADA'),
  (6,  1, 7,  'DOGE'),
  (7,  1, 8,  'AVAX'),
  (8,  1, 9,  'DOT'),
  (9,  1, 10, 'LINK'),
  (10, 1, 11, 'POL');

-- user 1~1000
INSERT INTO user (user_id, nickname, portfolio_public, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT n, CONCAT('loadtest', n), true, NOW(), NOW() FROM seq;

-- investment_round 1~1000 (user n → round n)
INSERT INTO investment_round (
  round_id, version, user_id, round_number, initial_seed,
  emergency_funding_limit, emergency_charge_count, status, started_at, ended_at
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT n, 0, n, 1, 10000000000.00000000, 0.00000000, 0, 'ACTIVE', NOW(), NULL FROM seq;

-- wallet 1~1000 (round n → wallet n)
INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT n, n, 1, 10000000000.00000000, NOW() FROM seq;

-- 각 wallet 의 KRW(coin_id=1) 잔고
INSERT INTO wallet_balance (balance_id, wallet_id, coin_id, available, locked)
WITH RECURSIVE seq AS (
  SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT n, n, 1, 10000000000.00000000, 0.00000000 FROM seq;

-- ===== 전 기능 부하 시드 확장 (기존 1~1000 / UPBIT 1:1 스파인 보존, 더하기만) =====

-- 2번째 거래소 (송금: 같은 라운드 거래소 간 이체용). DOMESTIC + KRW 기축.
INSERT INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate)
VALUES (2, 'BITHUMB', 'DOMESTIC', 1, 0.000500);

-- 2번째 거래소 상장 코인 (exchange_coin_id 11~20 → coin 2~11)
INSERT INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name)
SELECT 9 + c.coin_id, 2, c.coin_id, c.symbol FROM coin c WHERE c.coin_id BETWEEN 2 AND 11;

-- 라운드당 2번째 지갑 (wallet 1001~2000 → round n, exchange 2): 송금 src/dst 쌍
INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)
WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
SELECT 1000 + n, n, 2, 10000000000.00000000, NOW() FROM seq;

-- 2번째 지갑 KRW 잔고 (balance_id 1001~2000)
INSERT INTO wallet_balance (balance_id, wallet_id, coin_id, available, locked)
WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
SELECT 1000 + n, 1000 + n, 1, 10000000000.00000000, 0.00000000 FROM seq;

-- 라운드 없는 유저 풀 (user 1001~1200): 라운드 생성 부하용 (start→end 자급자족, 동시성 여유로 200명)
INSERT INTO user (user_id, nickname, portfolio_public, created_at, updated_at)
WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 200)
SELECT 1000 + n, CONCAT('loadtest', 1000 + n), true, NOW(), NOW() FROM seq;

-- 긴급충전 부하용: 한도 0 → 100만(도메인 MAX_EMERGENCY_FUNDING_LIMIT), 충전 횟수 0 → 100만(run 중 소진 방지)
UPDATE investment_round SET emergency_funding_limit = 1000000.00000000, emergency_charge_count = 1000000 WHERE round_id BETWEEN 1 AND 1000;

-- TradingRules.inspect() 의 일일 주문 수 조회용 인덱스
CREATE INDEX idx_orders_wallet_created ON orders (wallet_id, created_at);
