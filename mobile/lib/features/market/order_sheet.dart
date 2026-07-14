import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:uuid/uuid.dart';

import '../../core/api/api_exception.dart';
import '../../core/format/formatters.dart';
import '../../core/json/decimal_x.dart';
import '../../core/realtime/ticker_store.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import 'market_controller.dart';
import 'order_form.dart';
import 'order_repository.dart';
import 'order_target.dart';

/// 웹의 360px 고정 사이드바를 바텀시트로 옮긴 것(계획서 §4.6.1). 입력 3개 + 비율 버튼 + 안내가
/// 세로로 길어 시트를 크게 연다.
Future<void> showOrderSheet(
  BuildContext context, {
  required OrderTarget target,
  required CoinEntry entry,
  required Side side,
  required VoidCallback onPlaced,
}) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (context) => DraggableScrollableSheet(
      initialChildSize: 0.75,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      expand: false,
      builder: (context, controller) => OrderSheet(
        target: target,
        entry: entry,
        initialSide: side,
        scrollController: controller,
        onPlaced: onPlaced,
      ),
    ),
  );
}

class OrderSheet extends ConsumerStatefulWidget {
  const OrderSheet({
    super.key,
    required this.target,
    required this.entry,
    required this.initialSide,
    required this.scrollController,
    required this.onPlaced,
  });

  final OrderTarget target;
  final CoinEntry entry;
  final Side initialSide;
  final ScrollController scrollController;

  /// 체결 반영은 REST 재조회다(사양서 §4.4.8) — 거래내역·주문 가능 금액을 다시 읽게 한다.
  final VoidCallback onPlaced;

  @override
  ConsumerState<OrderSheet> createState() => _OrderSheetState();
}

class _OrderSheetState extends ConsumerState<OrderSheet> {
  final TextEditingController _priceInput = TextEditingController();
  final TextEditingController _quantityInput = TextEditingController();
  final TextEditingController _totalInput = TextEditingController();

  late OrderForm _form = OrderForm.empty(side: widget.initialSide);

  /// 멱등키. **재시도 시 같은 값을 재사용**해야 중복 주문이 생기지 않는다(사양서 §4.4.7).
  /// 성공한 뒤에만 새로 만든다.
  String _clientOrderId = const Uuid().v4();

  Decimal _availableBuy = Decimal.zero;
  Decimal _availableSell = Decimal.zero;
  late Decimal _restPrice = widget.entry.price.dec;

  bool _loading = true;
  bool _submitting = false;
  String? _availabilityError;

  @override
  void initState() {
    super.initState();
    _loadAvailability();
  }

  @override
  void dispose() {
    _priceInput.dispose();
    _quantityInput.dispose();
    _totalInput.dispose();
    super.dispose();
  }

  /// 시장가 계산의 기준가. 티커는 Riverpod 그래프를 타지 않으므로 **행동 시점에 읽는다** —
  /// 구독하면 초당 수백 번 시트가 리빌드된다.
  Decimal get _currentPrice {
    final live = ref.read(tickerStoreProvider).quote(widget.entry.symbol)?.price;
    if (live != null && live > 0) return live.dec;
    return _restPrice;
  }

  OrderContext get _ctx => OrderContext(
    exchange: widget.target.exchange,
    currentPrice: _currentPrice,
    availableBuy: _availableBuy,
    availableSell: _availableSell,
  );

  /// BUY/SELL 을 **병렬 2회** 호출한다(사양서 §4.4.4). 실패하면 두 값을 0 으로 두고 알린다.
  Future<void> _loadAvailability() async {
    setState(() {
      _loading = true;
      _availabilityError = null;
    });
    final repository = ref.read(orderRepositoryProvider);
    try {
      final results = await Future.wait([
        for (final side in [Side.buy, Side.sell])
          repository.getAvailability(
            walletId: widget.target.walletId,
            exchangeCoinId: widget.target.exchangeCoinId,
            side: side,
          ),
      ]);
      if (!mounted) return;
      setState(() {
        _availableBuy = results[0].available.dec;
        _availableSell = results[1].available.dec;
        if (results[0].currentPrice > 0) _restPrice = results[0].currentPrice.dec;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _availableBuy = Decimal.zero;
        _availableSell = Decimal.zero;
        _availabilityError = error.userMessage;
        _loading = false;
      });
    }
  }

  void _apply(OrderForm next, {OrderField? edited}) {
    setState(() => _form = next);
    // 사용자가 손대고 있는 칸의 텍스트는 건드리지 않는다. 커서가 튄다.
    if (edited != OrderField.price) _sync(_priceInput, _amountText(next.price));
    if (edited != OrderField.quantity) {
      _sync(_quantityInput, _quantityText(next.quantity));
    }
    if (edited != OrderField.total) _sync(_totalInput, _amountText(next.total));
  }

  void _sync(TextEditingController controller, String text) {
    if (controller.text == text) return;
    controller.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }

  String _amountText(Decimal value) => value <= Decimal.zero
      ? ''
      : formatPrice(value.toDouble(), _ctx.baseCurrency);

  String _quantityText(Decimal value) =>
      value <= Decimal.zero ? '' : value.toString();

  bool get _touched =>
      _form.price > Decimal.zero ||
      _form.quantity > Decimal.zero ||
      _form.total > Decimal.zero;

  void _reset() =>
      _apply(OrderForm.empty(side: _form.side, orderType: _form.orderType));

  Future<void> _submit() async {
    final form = _form;
    if (_submitting || form.validate(_ctx) != null) return;

    setState(() => _submitting = true);
    try {
      await ref
          .read(orderRepositoryProvider)
          .placeOrder(
            form.toRequest(
              target: widget.target,
              clientOrderId: _clientOrderId,
            ),
          );
      if (!mounted) return;
      showAppSnackbar(context, form.successMessage);
      _clientOrderId = const Uuid().v4();
      _reset();
      widget.onPlaced();
      await _loadAvailability();
    } on ApiException catch (error) {
      // 키를 바꾸지 않는다. 같은 주문을 다시 눌렀을 때 서버가 중복을 걸러야 한다.
      if (mounted) showAppSnackbar(context, error.userMessage, isError: true);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ctx = _ctx;
    final error = _form.validate(ctx);
    final buy = _form.side == Side.buy;

    // 시트가 자체 ScaffoldMessenger 를 갖는다. 바깥 Scaffold 의 스낵바는 시트 뒤로 가려진다.
    return ScaffoldMessenger(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.viewInsetsOf(context).bottom,
          ),
          child: Column(
            children: [
              Expanded(
                child: ListView(
                  controller: widget.scrollController,
                  padding: const EdgeInsets.fromLTRB(
                    TryptoSpacing.screen,
                    0,
                    TryptoSpacing.screen,
                    TryptoSpacing.lg,
                  ),
                  children: [
                    _header(ctx),
                    const SizedBox(height: TryptoSpacing.md),
                    ExchangeSegment<Side>(
                      items: const [
                        SegmentItem(Side.buy, '매수'),
                        SegmentItem(Side.sell, '매도'),
                      ],
                      value: _form.side,
                      onChanged: (side) => _apply(_form.withSide(side)),
                    ),
                    const SizedBox(height: TryptoSpacing.sm),
                    ExchangeSegment<OrderType>(
                      items: const [
                        SegmentItem(OrderType.limit, '지정가'),
                        SegmentItem(OrderType.market, '시장가'),
                      ],
                      value: _form.orderType,
                      onChanged: (type) =>
                          _apply(_form.withOrderType(type, ctx)),
                    ),
                    const SizedBox(height: TryptoSpacing.md),
                    _available(ctx),
                    const SizedBox(height: TryptoSpacing.md),
                    _priceField(ctx),
                    if (_form.showsQuantity) ...[
                      const SizedBox(height: TryptoSpacing.md),
                      _OrderField(
                        label: '주문 수량',
                        suffix: widget.entry.symbol,
                        controller: _quantityInput,
                        onChanged: (text) => _apply(
                          _form.withQuantity(parseAmountInput(text), ctx),
                          edited: OrderField.quantity,
                        ),
                      ),
                    ],
                    if (_form.showsTotal) ...[
                      const SizedBox(height: TryptoSpacing.md),
                      _OrderField(
                        label: '주문 총액',
                        suffix: ctx.baseCurrency,
                        controller: _totalInput,
                        onChanged: (text) => _apply(
                          _form.withTotal(parseAmountInput(text), ctx),
                          edited: OrderField.total,
                        ),
                      ),
                    ],
                    const SizedBox(height: TryptoSpacing.md),
                    _ratios(ctx),
                    const SizedBox(height: TryptoSpacing.md),
                    _notice(ctx),
                  ],
                ),
              ),
              _footer(ctx, error: error, buy: buy),
            ],
          ),
        ),
      ),
    );
  }

  Widget _header(OrderContext ctx) {
    final theme = Theme.of(context);
    final row = ref.watch(tickerStoreProvider).row(widget.entry.symbol);

    Widget price(double value) => NumericText(
      value <= 0
          ? '-'
          : '${getCurrencySymbol(ctx.baseCurrency)}'
                '${formatPrice(value, ctx.baseCurrency)}',
      size: 18,
      weight: FontWeight.w700,
    );

    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(widget.entry.symbol, style: TryptoText.symbol),
              const SizedBox(height: 2),
              Text(
                '${widget.entry.name} · ${ctx.exchange.name}',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        if (row == null)
          price(_restPrice.toDouble())
        else
          ValueListenableBuilder<CoinRowState>(
            valueListenable: row,
            builder: (context, state, child) => price(state.price),
          ),
      ],
    );
  }

  Widget _available(OrderContext ctx) {
    final theme = Theme.of(context);
    final buy = _form.side == Side.buy;
    final error = _availabilityError;

    return Container(
      padding: const EdgeInsets.all(TryptoSpacing.md),
      decoration: BoxDecoration(
        color: TryptoPalette.secondary,
        borderRadius: BorderRadius.circular(TryptoRadius.md),
      ),
      child: Row(
        children: [
          Text(
            buy ? '주문 가능 금액' : '주문 가능 수량',
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const Spacer(),
          if (_loading)
            const SizedBox(
              width: 14,
              height: 14,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          else if (error != null)
            Flexible(
              child: Text(
                error,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
            )
          else
            NumericText(
              buy
                  ? ctx.amountLabel(_availableBuy)
                  : '${formatQuantity(_availableSell.toDouble())} '
                        '${widget.entry.symbol}',
            ),
        ],
      ),
    );
  }

  Widget _priceField(OrderContext ctx) {
    // 시장가는 가격 칸이 현재가로 고정되고 비활성이다(사양서 §4.4.2).
    if (!_form.priceEditable) {
      return _FixedPriceField(
        text: formatPrice(ctx.currentPrice.toDouble(), ctx.baseCurrency),
        suffix: ctx.baseCurrency,
      );
    }

    return _OrderField(
      label: '지정가',
      suffix: ctx.baseCurrency,
      controller: _priceInput,
      onChanged: (text) =>
          _apply(_form.withPrice(parseAmountInput(text), ctx), edited: OrderField.price),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton.outlined(
            icon: const Icon(LucideIcons.minus, size: 14),
            onPressed: () => _apply(_form.stepPrice(-1, ctx)),
          ),
          const SizedBox(width: TryptoSpacing.xs),
          IconButton.outlined(
            icon: const Icon(LucideIcons.plus, size: 14),
            onPressed: () => _apply(_form.stepPrice(1, ctx)),
          ),
        ],
      ),
    );
  }

  Widget _ratios(OrderContext ctx) {
    return Row(
      children: [
        for (final ratio in kOrderRatios) ...[
          Expanded(
            child: OutlinedButton(
              onPressed: _loading
                  ? null
                  : () => _apply(_form.applyRatio(ratio, ctx)),
              child: Text('$ratio%'),
            ),
          ),
          if (ratio != kOrderRatios.last) const SizedBox(width: TryptoSpacing.xs),
        ],
      ],
    );
  }

  /// 수수료율·최소 주문 금액은 **거래소별 실제 값**이다. 웹은 둘 다 하드코딩이라 바이낸스에서도
  /// `0.05%` / `최소 주문 5,000 USDT` 로 나온다(사양서 §4.4.5).
  Widget _notice(OrderContext ctx) {
    final theme = Theme.of(context);
    final style = theme.textTheme.labelMedium?.copyWith(
      color: theme.colorScheme.onSurfaceVariant,
    );
    final fee = ctx.exchange.feeRate * 100;

    return Column(
      children: [
        Row(
          children: [
            Text('예상 수수료', style: style),
            const Spacer(),
            NumericText(
              '${ctx.amountLabel(_form.feeOf(ctx))} '
              '(${fee.toStringAsFixed(2)}%)',
              size: 12,
              weight: FontWeight.w500,
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ],
        ),
        const SizedBox(height: TryptoSpacing.xs),
        Row(
          children: [
            Text('최소 주문 금액', style: style),
            const Spacer(),
            NumericText(
              ctx.amountLabel(ctx.minOrderAmount),
              size: 12,
              weight: FontWeight.w500,
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ],
        ),
      ],
    );
  }

  Widget _footer(OrderContext ctx, {required String? error, required bool buy}) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final ready = error == null && !_loading;

    return Container(
      padding: const EdgeInsets.fromLTRB(
        TryptoSpacing.screen,
        TryptoSpacing.md,
        TryptoSpacing.screen,
        TryptoSpacing.md,
      ),
      decoration: const BoxDecoration(
        color: TryptoPalette.card,
        border: Border(top: BorderSide(color: TryptoPalette.border)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 아무것도 입력하지 않은 첫 화면에서 "수량을 입력해 주세요" 를 붉게 띄우지 않는다.
          if (error != null && _touched) ...[
            Text(
              error,
              textAlign: TextAlign.center,
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.error,
              ),
            ),
            const SizedBox(height: TryptoSpacing.sm),
          ],
          Row(
            children: [
              TextButton(
                onPressed: _submitting ? null : _reset,
                child: const Text('초기화'),
              ),
              const SizedBox(width: TryptoSpacing.sm),
              Expanded(
                child: SizedBox(
                  height: 48,
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      backgroundColor: buy ? colors.positive : colors.negative,
                      foregroundColor: Colors.white,
                      disabledBackgroundColor:
                          (buy ? colors.positive : colors.negative).withValues(
                            alpha: 0.4,
                          ),
                      disabledForegroundColor: Colors.white,
                    ),
                    onPressed: ready && !_submitting ? _submit : null,
                    child: _submitting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : Text(buy ? '매수' : '매도'),
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

/// 시장가의 가격 칸. `TextField` 를 비활성으로 두면 build 마다 컨트롤러를 새로 만들어야 한다.
class _FixedPriceField extends StatelessWidget {
  const _FixedPriceField({required this.text, required this.suffix});

  final String text;
  final String suffix;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '주문 가격 (현재가)',
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: TryptoSpacing.xs),
        Container(
          height: 36,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(TryptoRadius.md),
            border: Border.all(
              color: TryptoPalette.secondary.withValues(alpha: 0.5),
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              NumericText(
                text,
                size: 16,
                color: theme.colorScheme.onSurfaceVariant,
              ),
              const SizedBox(width: TryptoSpacing.xs),
              Text(
                suffix,
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _OrderField extends StatelessWidget {
  const _OrderField({
    required this.label,
    required this.suffix,
    required this.controller,
    required this.onChanged,
    this.trailing,
  });

  final String label;
  final String suffix;
  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: TryptoSpacing.xs),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: controller,
                onChanged: onChanged,
                textAlign: TextAlign.right,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
                ],
                style: TryptoText.numeric(size: 16),
                decoration: InputDecoration(
                  hintText: '0',
                  suffixText: suffix,
                  suffixStyle: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ),
            if (trailing != null) ...[
              const SizedBox(width: TryptoSpacing.sm),
              trailing!,
            ],
          ],
        ),
      ],
    );
  }
}
