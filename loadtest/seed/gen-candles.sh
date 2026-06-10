#!/bin/bash
# 1분봉 캔들 시드 생성 → InfluxDB line protocol 을 stdout 으로 출력.
# 사용: gen-candles.sh [DAYS] | influx write --bucket ticker --org trypto --token ... --precision s
#
# UPBIT × 코인 10종 × (DAYS*1440) 분봉. 코인 1종당 시작가에서 분당 ±0.1% 랜덤워크.
# coin 태그는 CandleQueryAdapter 가 조회하는 "SYM/KRW" 형식(서버의 coin+baseCurrency 변환과 일치).
# 가장 최신 캔들이 현재 분에 맞춰지므로 cursor 없는 최신 조회가 빈 배열을 받지 않는다.
set -euo pipefail

DAYS="${1:-30}"
NOW="$(date +%s)" # systime() 은 mawk 에 없어 호스트에서 epoch 를 구해 주입한다

awk -v days="$DAYS" -v now="$NOW" 'BEGIN {
  srand();
  # 심볼:시작가 — reset.sh my_holdings 시드 현재가와 동일한 수준
  ncoin = split("BTC:50000000 ETH:5200000 XRP:4800 SOL:200000 ADA:600 DOGE:300 AVAX:40000 DOT:9000 LINK:25000 POL:700", arr, " ");
  minutes = days * 1440;
  endMin = now - (now % 60);              # 현재 분으로 정렬
  startSec = endMin - (minutes - 1) * 60; # 가장 오래된 분의 epoch(초)

  for (c = 1; c <= ncoin; c++) {
    split(arr[c], kv, ":");
    sym = kv[1];
    price = kv[2] + 0;
    for (m = 0; m < minutes; m++) {
      ts = startSec + m * 60;
      drift = price * (rand() - 0.5) * 0.002;  # 분당 ±0.1%
      op = price;
      cl = price + drift;
      if (cl < 0.0001) cl = 0.0001;
      hi = (op > cl ? op : cl) * (1 + rand() * 0.0008);
      lo = (op < cl ? op : cl) * (1 - rand() * 0.0008);
      printf "candle_1m,exchange=UPBIT,coin=%s/KRW open=%.4f,high=%.4f,low=%.4f,close=%.4f %d\n",
             sym, op, hi, lo, cl, ts;
      price = cl;
    }
  }
}'
