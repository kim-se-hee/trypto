import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'transfer.g.dart';

/// `POST /api/transfers`
///
/// [idempotencyKey] 는 **UUID v4 형식이 필수**다. 형식이 어긋나면 서버가 400 이 아니라 500 을
/// 낸다(사양서 R8-1). 중복이면 서버가 기존 `transferId` 를 `SUCCESS` 로 되돌려 준다.
///
/// 송금 API 는 `exchangeCoinId` 가 아니라 **`coinId`** 를 받는다.
@JsonSerializable(createFactory: false)
class TransferCoinRequest {
  const TransferCoinRequest({
    required this.idempotencyKey,
    required this.fromWalletId,
    required this.toWalletId,
    required this.coinId,
    required this.amount,
  });

  final String idempotencyKey;
  final int fromWalletId;
  final int toWalletId;
  final int coinId;

  /// `0 < amount <= available`
  final double amount;

  Map<String, dynamic> toJson() => _$TransferCoinRequestToJson(this);
}

@JsonSerializable(createToJson: false)
class TransferCoinResponse {
  const TransferCoinResponse({required this.transferId, required this.status});

  factory TransferCoinResponse.fromJson(Map<String, dynamic> json) =>
      _$TransferCoinResponseFromJson(json);

  final int transferId;

  @JsonKey(unknownEnumValue: TransferStatus.unknown)
  final TransferStatus status;
}

/// `GET /api/wallets/{walletId}/transfers` 의 항목.
///
/// [type] 은 **조회자 지갑 기준**으로 서버가 계산한다(출발 지갑이면 `WITHDRAW`, 아니면 `DEPOSIT`).
/// 응답에 상대 지갑·거래소 정보는 없다. 송금은 동기 완료라 `completedAt == createdAt` 이다.
@JsonSerializable(createToJson: false)
class TransferHistoryItem {
  const TransferHistoryItem({
    required this.transferId,
    required this.type,
    required this.coinId,
    required this.coinSymbol,
    required this.amount,
    required this.status,
    required this.createdAt,
    this.completedAt,
  });

  factory TransferHistoryItem.fromJson(Map<String, dynamic> json) =>
      _$TransferHistoryItemFromJson(json);

  final int transferId;

  @JsonKey(unknownEnumValue: TransferType.unknown)
  final TransferType type;

  final int coinId;
  final String coinSymbol;
  final double amount;

  @JsonKey(unknownEnumValue: TransferStatus.unknown)
  final TransferStatus status;

  @KstDateTimeConverter()
  final DateTime createdAt;

  @NullableKstDateTimeConverter()
  final DateTime? completedAt;
}
