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
import '../../models/order.dart';
import 'order_repository.dart';
import 'order_target.dart';

const int _kPageSize = 20;

/// 코인 상세의 두 번째 하단 탭(계획서 §4.6.1). 바텀시트 안에 커서 무한 스크롤 목록을 넣으면
/// 시트 드래그와 목록 스크롤 제스처가 충돌하므로 주문 시트에서 분리했다.
///
/// **체결 반영은 REST 재조회다**(사양서 §4.4.8, R3). 화면 진입·주문 제출·포그라운드 복귀·
/// 당김 새로고침에서 다시 읽는다. [revision] 이 바뀌면 주문이 나갔다는 뜻이다.
class OrderHistoryTab extends ConsumerStatefulWidget {
  const OrderHistoryTab({
    super.key,
    required this.target,
    required this.symbol,
    required this.revision,
  });

  final OrderTarget target;
  final String symbol;
  final int revision;

  @override
  ConsumerState<OrderHistoryTab> createState() => _OrderHistoryTabState();
}

class _OrderHistoryTabState extends ConsumerState<OrderHistoryTab> {
  final ScrollController _scroll = ScrollController();

  /// 응답에 `status` 가 없다(사양서 R4-9). 요청 필터값을 그대로 표시에 쓴다.
  OrderStatus _status = OrderStatus.filled;

  List<OrderHistoryItem> _items = [];
  int? _cursor;
  bool _hasNext = false;
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  int _generation = 0;

  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: _reload);
    _scroll.addListener(_onScroll);
    _reload();
  }

  @override
  void didUpdateWidget(OrderHistoryTab oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.revision != oldWidget.revision ||
        widget.target != oldWidget.target) {
      _reload();
    }
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scroll.hasClients || !_hasNext || _loadingMore || _loading) return;
    if (_scroll.position.pixels < _scroll.position.maxScrollExtent - 200) return;
    _loadMore();
  }

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

  Future<CursorPage<OrderHistoryItem>> _fetch({required int? cursor}) => ref
      .read(orderRepositoryProvider)
      .getOrderHistory(
        walletId: widget.target.walletId,
        exchangeCoinId: widget.target.exchangeCoinId,
        status: _status,
        cursorOrderId: cursor,
        size: _kPageSize,
      );

  void _setStatus(OrderStatus status) {
    if (_status == status) return;
    setState(() => _status = status);
    _reload();
  }

  Future<void> _cancel(OrderHistoryItem item) async {
    try {
      await ref
          .read(orderRepositoryProvider)
          .cancelOrder(
            orderId: item.orderId,
            walletId: widget.target.walletId,
          );
      if (!mounted) return;
      // 목록을 다시 읽지 않는다 — 취소된 주문은 미체결 필터에서 사라진다. 풀린 잠금 잔고는
      // 주문 시트가 열릴 때마다 `available` 을 새로 읽으므로 저절로 맞는다.
      setState(
        () => _items = [
          for (final order in _items)
            if (order.orderId != item.orderId) order,
        ],
      );
      showAppSnackbar(context, '주문을 취소했습니다.');
    } on ApiException catch (error) {
      if (mounted) showAppSnackbar(context, error.userMessage, isError: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            TryptoSpacing.screen,
            TryptoSpacing.sm,
            TryptoSpacing.screen,
            TryptoSpacing.sm,
          ),
          child: ExchangeSegment<OrderStatus>(
            items: const [
              SegmentItem(OrderStatus.filled, '체결'),
              SegmentItem(OrderStatus.pending, '미체결'),
            ],
            value: _status,
            onChanged: _setStatus,
          ),
        ),
        Expanded(child: _body()),
      ],
    );
  }

  Widget _body() {
    if (_loading) return const Center(child: CircularProgressIndicator());

    final error = _error;
    if (error != null) {
      return EmptyView(
        icon: LucideIcons.circleAlert,
        message: error,
        action: OutlinedButton(
          onPressed: _reload,
          child: const Text('다시 시도'),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _reload,
      child: _items.isEmpty
          ? ListView(
              children: [
                const SizedBox(height: 80),
                EmptyView(
                  icon: LucideIcons.receipt,
                  message: _status == OrderStatus.filled
                      ? '체결 내역이 없습니다.'
                      : '미체결 주문이 없습니다.',
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
                final item = _items[index];
                return _OrderTile(
                  item: item,
                  status: _status,
                  symbol: widget.symbol,
                  baseCurrency: widget.target.exchange.baseCurrency,
                  onCancel: _status == OrderStatus.pending
                      ? () => _cancel(item)
                      : null,
                );
              },
            ),
    );
  }
}

class _OrderTile extends StatelessWidget {
  const _OrderTile({
    required this.item,
    required this.status,
    required this.symbol,
    required this.baseCurrency,
    this.onCancel,
  });

  final OrderHistoryItem item;
  final OrderStatus status;
  final String symbol;
  final String baseCurrency;
  final VoidCallback? onCancel;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final buy = item.side == Side.buy;
    // 미체결 주문은 체결가·체결금액이 없다. 지정가를 대신 보여준다.
    final price = item.filledPrice ?? item.price ?? 0;
    final amount = item.orderAmount ?? price * item.quantity;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.screen,
        vertical: TryptoSpacing.md,
      ),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: TryptoPalette.border)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              TryptoBadge(
                label: buy ? '매수' : '매도',
                foreground: buy ? colors.positive : colors.negative,
                background: (buy ? colors.positive : colors.negative).withValues(
                  alpha: 0.15,
                ),
              ),
              const SizedBox(width: TryptoSpacing.xs),
              Text(
                item.orderType == OrderType.market ? '시장가' : '지정가',
                style: theme.textTheme.labelMedium,
              ),
              const SizedBox(width: TryptoSpacing.xs),
              if (status == OrderStatus.pending)
                const WarningBadge(label: '대기')
              else
                Text(
                  '체결',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              const Spacer(),
              Text(
                ServerTime.relative(item.createdAt),
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: TryptoSpacing.sm),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: _Cell(
                  label: '가격',
                  value:
                      '${getCurrencySymbol(baseCurrency)}'
                      '${formatPrice(price, baseCurrency)}',
                ),
              ),
              Expanded(
                child: _Cell(
                  label: '수량',
                  value: '${formatQuantity(item.quantity)} $symbol',
                ),
              ),
              Expanded(
                child: _Cell(
                  label: '금액',
                  value: formatCurrencyCompact(amount, baseCurrency),
                ),
              ),
              if (onCancel != null)
                TextButton(
                  style: TryptoButtons.link,
                  onPressed: onCancel,
                  child: Text(
                    '취소',
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: theme.colorScheme.error,
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Cell extends StatelessWidget {
  const _Cell({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 2),
        NumericText(value, size: 12, weight: FontWeight.w500),
      ],
    );
  }
}
