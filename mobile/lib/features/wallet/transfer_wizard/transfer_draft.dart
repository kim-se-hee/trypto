import 'package:decimal/decimal.dart';

import '../../../core/api/api_exception.dart';
import '../../../core/constants/exchanges.dart';
import '../../../core/json/decimal_x.dart';

/// 도착 후보. 라운드가 가진 지갑 중 현재 거래소를 제외한 전부다(사양서 §5.2.6).
class TransferDestination {
  const TransferDestination({required this.walletId, required this.exchange});

  final int walletId;
  final Exchange exchange;
}

/// 마법사 3단계가 공유하는 입력값. 코인만 송금할 수 있다 — 기준통화는 `coinId` 가 없다.
class TransferDraft {
  const TransferDraft({
    required this.fromWalletId,
    required this.fromExchange,
    required this.coinId,
    required this.symbol,
    required this.available,
    required this.destinations,
  });

  final int fromWalletId;
  final Exchange fromExchange;

  /// 송금 API 는 `exchangeCoinId` 가 아니라 **`coinId`** 를 받는다.
  final int coinId;

  final String symbol;
  final double available;
  final List<TransferDestination> destinations;
}

class TransferOutcome {
  const TransferOutcome({
    required this.transferId,
    required this.amount,
    required this.symbol,
  });

  final int transferId;
  final double amount;
  final String symbol;
}

Decimal? parseTransferAmount(String raw) => Decimal.tryParse(raw.trim());

/// 웹은 제출을 한 번 시도한 뒤에만 오류를 보여준다(사양서 §5.2.6). 여기서는 입력 즉시 검증하고
/// 위반이면 다음 단계 버튼을 비활성화한다.
String? validateTransferAmount(String raw, double available) {
  final amount = parseTransferAmount(raw);
  if (amount == null || amount <= Decimal.zero) return '수량을 입력해 주세요.';
  if (amount > available.dec) return '가용 잔고를 초과합니다.';
  return null;
}

/// 비율 버튼 `[25%] [50%] [최대]` 의 입력값. 반올림이 가용 잔고를 넘기지 않도록 **내림**한다
/// (코인 수량은 소수 8자리다). 최대는 잔고를 그대로 쓴다.
String transferAmountOfRatio(double available, double ratio) {
  if (ratio >= 1) return available.dec.toString();
  return (available.dec * ratio.dec).floor(scale: 8).toString();
}

/// 송금 화면 하나만 문맥 문구를 갖는다(계획서 §4.1.4). 표에 없는 코드는 서버 메시지를 그대로
/// 보여준다 — 이미 한국어 완성문이다.
const Map<String, String> _kTransferErrors = {
  'INSUFFICIENT_BALANCE': '가용 잔고가 부족합니다.',
  'SAME_WALLET_TRANSFER': '같은 거래소로는 송금할 수 없습니다.',
  'DIFFERENT_ROUND_TRANSFER': '다른 라운드의 지갑으로는 송금할 수 없습니다.',
  'WALLET_NOT_OWNED': '접근 권한이 없는 지갑입니다.',
  'WALLET_ACCESS_DENIED': '접근 권한이 없는 지갑입니다.',
};

String transferErrorMessage(ApiException error) =>
    _kTransferErrors[error.code] ?? error.userMessage;
