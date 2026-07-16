import 'package:flutter/material.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import '../../models/transfer.dart';
import 'transfer_history_page.dart';
import 'transfer_wizard/step1_destination.dart';
import 'transfer_wizard/transfer_draft.dart';
import 'wallet_assets.dart';

/// 자산 카드 탭 → 상세. 송금이 성공하면 그 결과를 돌려주고 닫힌다(지갑 화면이 재조회한다).
Future<TransferOutcome?> showAssetDetailSheet(
  BuildContext context, {
  required WalletAsset asset,
  required Exchange exchange,
  required int walletId,
  required List<TransferDestination> destinations,
  required List<TransferHistoryItem> transfers,
}) {
  return showModalBottomSheet<TransferOutcome>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (context) => AssetDetailSheet(
      asset: asset,
      exchange: exchange,
      walletId: walletId,
      destinations: destinations,
      transfers: transfers,
    ),
  );
}

class AssetDetailSheet extends StatelessWidget {
  const AssetDetailSheet({
    super.key,
    required this.asset,
    required this.exchange,
    required this.walletId,
    required this.destinations,
    required this.transfers,
  });

  final WalletAsset asset;
  final Exchange exchange;
  final int walletId;
  final List<TransferDestination> destinations;

  /// 최근 송금 20건. 여기서 이 코인 것만 골라 보여준다 — 서버에 코인 필터가 없다.
  final List<TransferHistoryItem> transfers;

  Future<void> _withdraw(BuildContext context) async {
    if (destinations.isEmpty) {
      showAppSnackbar(context, '보낼 수 있는 다른 거래소 지갑이 없습니다.', isError: true);
      return;
    }

    final outcome = await startTransfer(
      context,
      TransferDraft(
        fromWalletId: walletId,
        fromExchange: exchange,
        coinId: asset.coinId!,
        symbol: asset.symbol,
        available: asset.available,
        destinations: destinations,
      ),
    );
    if (outcome != null && context.mounted) {
      Navigator.of(context).pop(outcome);
    }
  }

  void _openHistory(BuildContext context) {
    Navigator.of(context, rootNavigator: true).push(
      MaterialPageRoute<void>(
        builder: (context) => TransferHistoryPage(
          walletId: walletId,
          exchangeName: exchange.name,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final base = exchange.baseCurrency;
    final history = [
      for (final item in transfers)
        if (item.coinId == asset.coinId) item,
    ];

    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.6,
      maxChildSize: 0.9,
      builder: (context, controller) => ListView(
        controller: controller,
        padding: const EdgeInsets.fromLTRB(
          TryptoSpacing.screen,
          0,
          TryptoSpacing.screen,
          TryptoSpacing.screen,
        ),
        children: [
          Row(
            children: [
              Text(asset.symbol, style: theme.textTheme.titleLarge),
              const SizedBox(width: TryptoSpacing.sm),
              Expanded(
                child: Text(
                  asset.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: TryptoSpacing.lg),
          NumericText(
            asset.isBase
                ? formatGrouped(asset.total)
                : formatQuantity(asset.total),
            size: 24,
            weight: FontWeight.w700,
          ),
          if (!asset.isBase) ...[
            const SizedBox(height: TryptoSpacing.xs),
            NumericText(
              formatFiatEstimate(asset.totalValue, base),
              size: 13,
              weight: FontWeight.w500,
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ],
          const SizedBox(height: TryptoSpacing.lg),
          // 출금은 코인만 가능하다. 기준통화(KRW/USDT)는 송금할 수 없고, 입금 버튼은 웹에도
          // 서버에도 없다(사양서 §5.2.5).
          if (!asset.isBase)
            SizedBox(
              width: double.infinity,
              height: 44,
              child: FilledButton(
                onPressed: asset.available > 0
                    ? () => _withdraw(context)
                    : null,
                child: const Text('출금'),
              ),
            ),
          const SizedBox(height: TryptoSpacing.xl),
          Text('잔고 상세', style: theme.textTheme.titleMedium),
          const SizedBox(height: TryptoSpacing.sm),
          _BalanceRow(
            label: '사용 가능',
            value: asset.isBase
                ? formatGrouped(asset.available)
                : formatQuantity(asset.available),
          ),
          _BalanceRow(
            label: '잠금',
            value: asset.locked > 0
                ? (asset.isBase
                      ? formatGrouped(asset.locked)
                      : formatQuantity(asset.locked))
                : '—',
            locked: asset.locked > 0,
          ),
          const SizedBox(height: TryptoSpacing.xl),
          Row(
            children: [
              Text('입출금 내역', style: theme.textTheme.titleMedium),
              const Spacer(),
              TextButton(
                style: TryptoButtons.link,
                onPressed: () => _openHistory(context),
                child: const Text('전체 보기'),
              ),
            ],
          ),
          if (history.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: TryptoSpacing.lg),
              child: Text(
                '최근 입출금 내역이 없습니다.',
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            )
          else
            for (final item in history) TransferTile(item: item),
        ],
      ),
    );
  }
}

class _BalanceRow extends StatelessWidget {
  const _BalanceRow({
    required this.label,
    required this.value,
    this.locked = false,
  });

  final String label;
  final String value;
  final bool locked;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: TryptoSpacing.xs),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: theme.textTheme.labelLarge?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          if (locked) ...[
            const WarningBadge(label: '주문 대기'),
            const SizedBox(width: TryptoSpacing.sm),
          ],
          NumericText(
            value,
            size: 13,
            weight: FontWeight.w600,
            color: locked ? context.tryptoColors.warning : null,
          ),
        ],
      ),
    );
  }
}
