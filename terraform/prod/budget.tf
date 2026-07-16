# 예산 알람. 이 계정은 전부 Always Free 한도 내 구성이라 정상 상태의 월 요금은 0원이다.
# 따라서 1달러라도 잡히면 그 자체가 이상 신호다 — 10% 룰이 조기 경보 역할을 한다.

resource "oci_budget_budget" "monthly" {
  compartment_id = var.tenancy_ocid
  display_name   = "trypto-monthly"
  description    = "월 예산 10달러. 정상 운영 시 0원이어야 한다"
  amount         = 10
  reset_period   = "MONTHLY"
  target_type    = "COMPARTMENT"
  targets        = [var.tenancy_ocid]
}

# 실제 청구액이 1달러(10%)를 넘는 순간 — "뭔가 과금되기 시작했다" 조기 경보
resource "oci_budget_alert_rule" "actual_early" {
  budget_id      = oci_budget_budget.monthly.id
  display_name   = "actual-1usd"
  type           = "ACTUAL"
  threshold      = 10
  threshold_type = "PERCENTAGE"
  recipients     = "kshee848@gmail.com"
  message        = "[trypto] 오라클 실제 청구액이 1달러를 넘었다. 무료 한도 밖 리소스가 생겼는지 확인할 것."
}

# 월말 예측치가 10달러(100%)를 넘을 때
resource "oci_budget_alert_rule" "forecast_full" {
  budget_id      = oci_budget_budget.monthly.id
  display_name   = "forecast-10usd"
  type           = "FORECAST"
  threshold      = 100
  threshold_type = "PERCENTAGE"
  recipients     = "kshee848@gmail.com"
  message        = "[trypto] 오라클 월말 예상 요금이 예산 10달러를 초과할 전망이다."
}
