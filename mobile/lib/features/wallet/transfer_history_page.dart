import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api/api_exception.dart';
import '../../core/format/formatters.dart';
import '../../core/format/server_time.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import '../../models/cursor_page.dart';
import '../../models/enums.dart';
import '../../models/transfer.dart';
import 'transfer_repository.dart';

const int _kPageSize = 20;

/// 전체 송금 내역. 웹은 코인을 선택해야만 내역을 볼 수 있어 접근성이 나빴다(사양서 §5.4.5).
///
/// 웹의 결함 셋을 여기서 걷어낸다.
/// - 거래소 필터를 두지 않는다. 내역은 이미 `walletId` 로 조회되며, 웹의 필터는 항상
///   공집합이 되는 결함이었다(R9-1).
/// - 유형 필터는 클라이언트가 아니라 **서버 파라미터 `type`** 으로 넘긴다.
/// - 상태 필터를 두지 않는다. 서버 `TransferStatus` 는 `SUCCESS` 단일값이다(R9-2).
class TransferHistoryPage extends ConsumerStatefulWidget {
  const TransferHistoryPage({
    super.key,
    required this.walletId,
    required this.exchangeName,
  });

  final int walletId;
  final String exchangeName;

  @override
  ConsumerState<TransferHistoryPage> createState() =>
      _TransferHistoryPageState();
}

class _TransferHistoryPageState extends ConsumerState<TransferHistoryPage> {
  final ScrollController _scroll = ScrollController();

  TransferType _type = TransferType.all;
  List<TransferHistoryItem> _items = [];
  int? _cursor;
  bool _hasNext = false;
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  int _generation = 0;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _reload();
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scroll.hasClients || !_hasNext || _loadingMore || _loading) return;
    if (_scroll.position.pixels < _scroll.position.maxScrollExtent - 200) return;
    _loadMore();
  }

  /// 커서 이름은 **`cursorTransferId`(int)** 다. 웹은 `cursor`(문자열)를 보내 페이지네이션이
  /// 아예 동작하지 않았다(R9-4).
  Future<CursorPage<TransferHistoryItem>> _fetch({required int? cursor}) => ref
      .read(transferRepositoryProvider)
      .getTransferHistory(
        walletId: widget.walletId,
        type: _type == TransferType.all ? null : _type,
        cursorTransferId: cursor,
        size: _kPageSize,
      );

  Future<void> _reload() async {
    final generation = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await _fetch(cursor: null);
      if (!mounted || generation != _generation) return;
      setState(() {
        _items = page.content;
        _cursor = page.nextCursor;
        _hasNext = page.hasNext;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (!mounted || generation != _generation) return;
      setState(() {
        _items = [];
        _hasNext = false;
        _error = error.userMessage;
        _loading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    final generation = _generation;
    setState(() => _loadingMore = true);
    try {
      final page = await _fetch(cursor: _cursor);
      if (!mounted || generation != _generation) return;
      setState(() {
        _items = [..._items, ...page.content];
        _cursor = page.nextCursor;
        _hasNext = page.hasNext;
        _loadingMore = false;
      });
    } on ApiException catch (error) {
      if (!mounted || generation != _generation) return;
      setState(() => _loadingMore = false);
      showAppSnackbar(context, error.userMessage, isError: true);
    }
  }

  void _setType(TransferType type) {
    if (_type == type) return;
    setState(() => _type = type);
    _reload();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('${widget.exchangeName} 입출금 내역')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(TryptoSpacing.screen),
            child: ExchangeSegment<TransferType>(
              items: const [
                SegmentItem(TransferType.all, '전체'),
                SegmentItem(TransferType.deposit, '입금'),
                SegmentItem(TransferType.withdraw, '출금'),
              ],
              value: _type,
              onChanged: _setType,
            ),
          ),
          Expanded(child: _body()),
        ],
      ),
    );
  }

  Widget _body() {
    if (_loading) return const Center(child: CircularProgressIndicator());

    final error = _error;
    if (error != null) {
      return EmptyView(
        icon: LucideIcons.circleAlert,
        message: error,
        action: OutlinedButton(onPressed: _reload, child: const Text('다시 시도')),
      );
    }

    return RefreshIndicator(
      onRefresh: _reload,
      child: _items.isEmpty
          ? ListView(
              children: const [
                SizedBox(height: 80),
                EmptyView(
                  icon: LucideIcons.arrowLeftRight,
                  message: '입출금 내역이 없습니다.',
                ),
              ],
            )
          : ListView.builder(
              controller: _scroll,
              itemCount: _items.length + (_hasNext ? 1 : 0),
              itemBuilder: (context, index) {
                if (index == _items.length) {
                  return const Padding(
                    padding: EdgeInsets.all(TryptoSpacing.lg),
                    child: Center(
                      child: SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    ),
                  );
                }
                return TransferTile(item: _items[index]);
              },
            ),
    );
  }
}

/// 송금 내역 한 줄. 전체 내역 화면과 자산 상세 시트가 같은 줄을 쓴다.
class TransferTile extends StatelessWidget {
  const TransferTile({super.key, required this.item});

  final TransferHistoryItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final deposit = item.type == TransferType.deposit;

    return ListTile(
      contentPadding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.screen,
        vertical: TryptoSpacing.xs,
      ),
      title: NumericText(
        '${formatQuantity(item.amount)} ${item.coinSymbol}',
        size: 14,
        weight: FontWeight.w600,
      ),
      subtitle: Text(
        '${deposit ? '입금' : '출금'} · '
        '${ServerTime.formatDateTime(item.createdAt)}',
        style: theme.textTheme.labelSmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
      trailing: TransferStatusBadge(status: item.status),
      onTap: () => _showDetail(context, item),
    );
  }
}

/// 서버 상태는 `SUCCESS` 단일값이다. 웹이 가정한 6종(대기·처리중·반환·지연…)은 존재하지
/// 않으며, 그 매핑은 전부 `undefined` 로 떨어졌다(사양서 §5.2.8-2).
class TransferStatusBadge extends StatelessWidget {
  const TransferStatusBadge({super.key, required this.status});

  final TransferStatus status;

  @override
  Widget build(BuildContext context) {
    final colors = context.tryptoColors;
    if (status != TransferStatus.success) {
      return const WarningBadge(label: '확인 필요');
    }
    return TryptoBadge(
      label: '완료',
      foreground: colors.positive,
      background: colors.positive.withValues(alpha: 0.15),
    );
  }
}

Future<void> _showDetail(BuildContext context, TransferHistoryItem item) {
  return showModalBottomSheet<void>(
    context: context,
    useSafeArea: true,
    builder: (context) => _TransferDetailSheet(item: item),
  );
}

class _TransferDetailSheet extends StatelessWidget {
  const _TransferDetailSheet({required this.item});

  final TransferHistoryItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final deposit = item.type == TransferType.deposit;
    final completedAt = item.completedAt;

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          TryptoSpacing.screen,
          0,
          TryptoSpacing.screen,
          TryptoSpacing.screen,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  '${item.coinSymbol} ${deposit ? '입금' : '출금'} 상세',
                  style: theme.textTheme.titleLarge,
                ),
                const Spacer(),
                TransferStatusBadge(status: item.status),
              ],
            ),
            const SizedBox(height: TryptoSpacing.lg),
            _DetailRow(
              label: '수량',
              value: '${formatQuantity(item.amount)} ${item.coinSymbol}',
            ),
            _DetailRow(
              label: '요청 시각',
              value: ServerTime.formatDateTime(item.createdAt),
            ),
            // 송금은 동기·즉시 완료라 completedAt == createdAt 이다.
            if (completedAt != null)
              _DetailRow(
                label: '완료 시각',
                value: ServerTime.formatDateTime(completedAt),
              ),
            const SizedBox(height: TryptoSpacing.sm),
            // 응답에 상대 지갑 정보가 없다. 웹은 빈 칸을 그려 놓았다(사양서 §5.2.8-5).
            Text(
              '상대 거래소 정보는 서버 응답에 포함되지 않습니다.',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

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
          NumericText(value, size: 13, weight: FontWeight.w600),
        ],
      ),
    );
  }
}
