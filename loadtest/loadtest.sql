-- 부하테스트 최소 시드
-- Hibernate DDL(create) 이후 spring.sql.init으로 실행됨
-- 모든 ID 하드코딩(=1)하여 k6 시나리오에서 그대로 참조

INSERT INTO user (user_id, email, nickname, portfolio_public, created_at, updated_at)
VALUES (1, 'loadtest@trypto.local', 'loadtest', true, NOW(), NOW());

INSERT INTO coin (coin_id, symbol, name) VALUES
  (1, 'KRW', 'Korean Won'),
  (2, 'BTC', 'Bitcoin');

INSERT INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id, fee_rate)
VALUES (1, 'UPBIT', 'DOMESTIC', 1, 0.000500);

INSERT INTO exchange_coin (exchange_coin_id, exchange_id, coin_id, display_name)
VALUES (1, 1, 2, 'BTC');

INSERT INTO investment_round (
  round_id, version, user_id, round_number, initial_seed,
  emergency_funding_limit, emergency_charge_count, status, started_at, ended_at
) VALUES (
  1, 0, 1, 1, 10000000000.00000000,
  0.00000000, 0, 'ACTIVE', NOW(), NULL
);

INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)
VALUES (1, 1, 1, 10000000000.00000000, NOW());

INSERT INTO wallet_balance (balance_id, wallet_id, coin_id, available, locked)
VALUES (1, 1, 1, 10000000000.00000000, 0.00000000);
