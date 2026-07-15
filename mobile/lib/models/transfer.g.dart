// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transfer.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Map<String, dynamic> _$TransferCoinRequestToJson(
  TransferCoinRequest instance,
) => <String, dynamic>{
  'idempotencyKey': instance.idempotencyKey,
  'fromWalletId': instance.fromWalletId,
  'toWalletId': instance.toWalletId,
  'coinId': instance.coinId,
  'amount': instance.amount,
};

TransferCoinResponse _$TransferCoinResponseFromJson(
  Map<String, dynamic> json,
) => TransferCoinResponse(
  transferId: (json['transferId'] as num).toInt(),
  status: $enumDecode(
    _$TransferStatusEnumMap,
    json['status'],
    unknownValue: TransferStatus.unknown,
  ),
);

const _$TransferStatusEnumMap = {
  TransferStatus.success: 'SUCCESS',
  TransferStatus.unknown: 'UNKNOWN',
};

TransferHistoryItem _$TransferHistoryItemFromJson(Map<String, dynamic> json) =>
    TransferHistoryItem(
      transferId: (json['transferId'] as num).toInt(),
      type: $enumDecode(
        _$TransferTypeEnumMap,
        json['type'],
        unknownValue: TransferType.unknown,
      ),
      coinId: (json['coinId'] as num).toInt(),
      coinSymbol: json['coinSymbol'] as String,
      amount: (json['amount'] as num).toDouble(),
      status: $enumDecode(
        _$TransferStatusEnumMap,
        json['status'],
        unknownValue: TransferStatus.unknown,
      ),
      createdAt: const KstDateTimeConverter().fromJson(
        json['createdAt'] as String,
      ),
      completedAt: const NullableKstDateTimeConverter().fromJson(
        json['completedAt'] as String?,
      ),
    );

const _$TransferTypeEnumMap = {
  TransferType.all: 'ALL',
  TransferType.deposit: 'DEPOSIT',
  TransferType.withdraw: 'WITHDRAW',
  TransferType.unknown: 'UNKNOWN',
};
