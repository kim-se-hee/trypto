import 'package:flutter/material.dart';

/// Material 3 `ColorScheme` 에 대응 슬롯이 없는 웹 토큰 (§8.6.2).
///
/// 상승은 초록 `#2ECC87`, 하락은 붉은 분홍 `#E85D75` 다. 한국 관행(빨강 상승·파랑 하락)이
/// 아니라 국제 관행을 따른다 (§8.1.2).
@immutable
class TryptoColors extends ThemeExtension<TryptoColors> {
  const TryptoColors({
    required this.positive,
    required this.negative,
    required this.warning,
    required this.chart1,
    required this.chart2,
    required this.chart3,
    required this.chart4,
    required this.chart5,
    required this.flashNeutral,
    required this.scrollbarThumb,
  });

  static const light = TryptoColors(
    positive: Color(0xFF2ECC87),
    negative: Color(0xFFE85D75),
    warning: Color(0xFFF0A030),
    chart1: Color(0xFF6C5CE7),
    chart2: Color(0xFF0ABFBC),
    chart3: Color(0xFFE85D75),
    chart4: Color(0xFFF0A030),
    chart5: Color(0xFF2ECC87),
    flashNeutral: Color(0x667C7C8A),
    scrollbarThumb: Color(0xFFD5D3CE),
  );

  final Color positive;
  final Color negative;
  final Color warning;
  final Color chart1;
  final Color chart2;
  final Color chart3;
  final Color chart4;
  final Color chart5;
  final Color flashNeutral;
  final Color scrollbarThumb;

  @override
  TryptoColors copyWith({
    Color? positive,
    Color? negative,
    Color? warning,
    Color? chart1,
    Color? chart2,
    Color? chart3,
    Color? chart4,
    Color? chart5,
    Color? flashNeutral,
    Color? scrollbarThumb,
  }) {
    return TryptoColors(
      positive: positive ?? this.positive,
      negative: negative ?? this.negative,
      warning: warning ?? this.warning,
      chart1: chart1 ?? this.chart1,
      chart2: chart2 ?? this.chart2,
      chart3: chart3 ?? this.chart3,
      chart4: chart4 ?? this.chart4,
      chart5: chart5 ?? this.chart5,
      flashNeutral: flashNeutral ?? this.flashNeutral,
      scrollbarThumb: scrollbarThumb ?? this.scrollbarThumb,
    );
  }

  @override
  TryptoColors lerp(ThemeExtension<TryptoColors>? other, double t) {
    if (other is! TryptoColors) return this;
    return TryptoColors(
      positive: Color.lerp(positive, other.positive, t)!,
      negative: Color.lerp(negative, other.negative, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      chart1: Color.lerp(chart1, other.chart1, t)!,
      chart2: Color.lerp(chart2, other.chart2, t)!,
      chart3: Color.lerp(chart3, other.chart3, t)!,
      chart4: Color.lerp(chart4, other.chart4, t)!,
      chart5: Color.lerp(chart5, other.chart5, t)!,
      flashNeutral: Color.lerp(flashNeutral, other.flashNeutral, t)!,
      scrollbarThumb: Color.lerp(scrollbarThumb, other.scrollbarThumb, t)!,
    );
  }
}

extension TryptoColorsX on BuildContext {
  TryptoColors get tryptoColors =>
      Theme.of(this).extension<TryptoColors>() ?? TryptoColors.light;

  /// 손익·등락 글자색. 양수는 상승색, 음수는 하락색, **0 은 색을 입히지 않는다**
  /// (§5.1.3, §6.2.4 — 웹이 0 을 기본색·무부호로 그린다).
  Color profitColor(num value, {Color? neutral}) {
    if (value > 0) return tryptoColors.positive;
    if (value < 0) return tryptoColors.negative;
    return neutral ?? Theme.of(this).colorScheme.onSurface;
  }
}
