#!/bin/bash
set -e

INFLUX_TOKEN="${DOCKER_INFLUXDB_INIT_ADMIN_TOKEN}"
ORG="${DOCKER_INFLUXDB_INIT_ORG}"
BUCKET="${DOCKER_INFLUXDB_INIT_BUCKET}"

# ticker_raw → candle_1m (매 1분, raw tick price를 OHLC로 집계)
influx task create \
  --org "$ORG" \
  --token "$INFLUX_TOKEN" \
  -f /dev/stdin <<'FLUX'
option task = {name: "aggregate_candle_1m", every: 1m, offset: 10s}

data = from(bucket: "ticker")
  |> range(start: -1m5s)
  |> filter(fn: (r) => r._measurement == "ticker_raw" and r._field == "price")

o = data
  |> aggregateWindow(every: 1m, fn: first, createEmpty: false, timeSrc: "_start")

h = data
  |> aggregateWindow(every: 1m, fn: max, createEmpty: false, timeSrc: "_start")

l = data
  |> aggregateWindow(every: 1m, fn: min, createEmpty: false, timeSrc: "_start")

c = data
  |> aggregateWindow(every: 1m, fn: last, createEmpty: false, timeSrc: "_start")

o |> set(key: "_field", value: "open") |> set(key: "_measurement", value: "candle_1m")
  |> to(bucket: "ticker", org: "trypto")

h |> set(key: "_field", value: "high") |> set(key: "_measurement", value: "candle_1m")
  |> to(bucket: "ticker", org: "trypto")

l |> set(key: "_field", value: "low") |> set(key: "_measurement", value: "candle_1m")
  |> to(bucket: "ticker", org: "trypto")

c |> set(key: "_field", value: "close") |> set(key: "_measurement", value: "candle_1m")
  |> to(bucket: "ticker", org: "trypto")
FLUX

echo "Task created: aggregate_candle_1m"

create_ohlc_task() {
  local task_name=$1
  local every=$2
  local offset=$3
  local range_start=$4
  local source=$5
  local measurement=$6
  local window_offset=${7:-}
  local exchange_pred=${8:-}   # 거래소 필터 Flux 식(예: 'r.exchange == "BITHUMB"'). 비우면 전 거래소.
  local tz=${9:-}              # 윈도우 정렬 타임존(예: Asia/Seoul). 비우면 UTC.
  local cron=${10:-}           # 실행 시각을 경계에 맞출 cron 식(UTC). 비우면 every 주기로 실행.

  local agg_offset=""
  if [ -n "$window_offset" ]; then
    agg_offset=", offset: ${window_offset}"
  fi

  local ex_filter=""
  if [ -n "$exchange_pred" ]; then
    ex_filter=" and (${exchange_pred})"
  fi

  # 일·주·월·4시간 경계는 UTC 자정이 아니라 해당 지역 자정에 맞춰야 하는 거래소가 있다(빗썸=00:00 KST).
  # location 은 윈도우 경계만 그 지역 시각으로 옮긴다. 실행 시각은 따라오지 않고 UTC 주기로 남으므로
  # 경계와 어긋나며, 이때 range 안에 경계가 안 들어와 잘린 윈도우가 엉뚱한 시각에 기록된다.
  # 그래서 지역 경계를 쓰는 태스크는 cron 으로 실행 시각을 경계에 맞춘다. KST 는 서머타임이 없어 항상 UTC+9.
  local tz_header=""
  if [ -n "$tz" ]; then
    tz_header="import \"timezone\"
option location = timezone.location(name: \"${tz}\")
"
  fi

  local schedule="every: ${every}"
  if [ -n "$cron" ]; then
    schedule="cron: \"${cron}\""
  fi

  influx task create \
    --org "$ORG" \
    --token "$INFLUX_TOKEN" \
    -f /dev/stdin <<FLUX
${tz_header}option task = {name: "${task_name}", ${schedule}, offset: ${offset}}

data = from(bucket: "${BUCKET}")
  |> range(start: ${range_start})
  |> filter(fn: (r) => r._measurement == "${source}"${ex_filter})

o = data
  |> filter(fn: (r) => r._field == "open")
  |> aggregateWindow(every: ${every}, fn: first, createEmpty: false, timeSrc: "_start"${agg_offset})
  |> last()

h = data
  |> filter(fn: (r) => r._field == "high")
  |> aggregateWindow(every: ${every}, fn: max, createEmpty: false, timeSrc: "_start"${agg_offset})
  |> last()

l = data
  |> filter(fn: (r) => r._field == "low")
  |> aggregateWindow(every: ${every}, fn: min, createEmpty: false, timeSrc: "_start"${agg_offset})
  |> last()

c = data
  |> filter(fn: (r) => r._field == "close")
  |> aggregateWindow(every: ${every}, fn: last, createEmpty: false, timeSrc: "_start"${agg_offset})
  |> last()

union(tables: [o, h, l, c])
  |> set(key: "_measurement", value: "${measurement}")
  |> to(bucket: "${BUCKET}", org: "${ORG}")
FLUX

  echo "Task created: ${task_name}"
}

# 1분·5분·1시간봉은 KST(UTC+9)와 UTC 경계가 겹쳐 시간대 분리가 필요 없다(전 거래소 한 태스크).
#            task_name                every  offset  range_start  source       measurement   window_offset
create_ohlc_task "aggregate_candle_5m"   "5m"   "30s"  "-5m30s"     "candle_1m"  "candle_5m"
create_ohlc_task "aggregate_candle_1h"   "1h"   "1m"   "-1h5m"      "candle_1m"  "candle_1h"

# 4시간·일·주·월봉은 경계가 시간대에 따라 갈린다. 업비트·바이낸스(UTC 자정=09시 KST)와
# 빗썸(00:00 KST=UTC 15시)을 별도 태스크로 나눈다. 나머지 = UTC, 빗썸 = Asia/Seoul.
#
# 빗썸 태스크에는 cron 으로 실행 시각을 KST 경계에 맞춘다(cron 은 UTC 기준):
#   4시간 = 00·04·08·12·16·20시 KST = 15·19·23·03·07·11시 UTC
#   일    = 00시 KST = 전날 15시 UTC
#   주    = 월요일 00시 KST = 일요일 15시 UTC
# 월봉은 경계가 매월 말일이라 cron 으로 표현되지 않는다. 대신 range 가 32일로 넉넉해
# 항상 경계가 걸린 온전한 윈도우가 잡히므로 every 주기를 그대로 둔다.
#            task_name                    every  offset  range_start  source       measurement   window_offset  exchange_pred                tz            cron
create_ohlc_task "aggregate_candle_4h"     "4h"   "2m"   "-4h5m"      "candle_1h"  "candle_4h"   ""     'r.exchange != "BITHUMB"'
create_ohlc_task "aggregate_candle_4h_kst" "4h"   "2m"   "-4h5m"      "candle_1h"  "candle_4h"   ""     'r.exchange == "BITHUMB"'   "Asia/Seoul"  "0 3,7,11,15,19,23 * * *"
create_ohlc_task "aggregate_candle_1d"     "1d"   "2m"   "-1d5m"      "candle_1h"  "candle_1d"   ""     'r.exchange != "BITHUMB"'
create_ohlc_task "aggregate_candle_1d_kst" "1d"   "2m"   "-1d5m"      "candle_1h"  "candle_1d"   ""     'r.exchange == "BITHUMB"'   "Asia/Seoul"  "0 15 * * *"
create_ohlc_task "aggregate_candle_1w"     "1w"   "3m"   "-1w5m"      "candle_1d"  "candle_1w"   "4d"   'r.exchange != "BITHUMB"'
create_ohlc_task "aggregate_candle_1w_kst" "1w"   "3m"   "-1w5m"      "candle_1d"  "candle_1w"   "4d"   'r.exchange == "BITHUMB"'   "Asia/Seoul"  "0 15 * * 0"
create_ohlc_task "aggregate_candle_1M"     "1mo"  "3m"   "-32d"       "candle_1d"  "candle_1M"   ""     'r.exchange != "BITHUMB"'
create_ohlc_task "aggregate_candle_1M_kst" "1mo"  "3m"   "-32d"       "candle_1d"  "candle_1M"   ""     'r.exchange == "BITHUMB"'   "Asia/Seoul"

echo "All InfluxDB aggregation tasks created."
