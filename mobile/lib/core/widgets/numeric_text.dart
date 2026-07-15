import 'package:flutter/material.dart';

import '../theme/theme.dart';

/// 모든 수치 표시의 단일 통로 (§8.2.1 — 웹은 가격·수량·수익률에 `font-mono` + `tabular-nums`
/// 를 항상 함께 쓴다).
///
/// 줄바꿈을 막고 넘치면 자른다. 티커 행의 숫자 셀은 고정 폭 제약 안에 들어가야 리레이아웃이
/// 부모로 전파되지 않는다 (§4.2.5-4).
class NumericText extends StatelessWidget {
  const NumericText(
    this.text, {
    super.key,
    this.size = 14,
    this.weight = FontWeight.w600,
    this.color,
    this.textAlign,
  });

  final String text;
  final double size;
  final FontWeight weight;
  final Color? color;
  final TextAlign? textAlign;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      textAlign: textAlign,
      maxLines: 1,
      softWrap: false,
      overflow: TextOverflow.clip,
      style: TryptoText.numeric(size: size, weight: weight, color: color),
    );
  }
}
