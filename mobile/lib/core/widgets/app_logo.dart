import 'package:flutter/material.dart';

/// trypto 심벌(종이비행기 라운드 타일). 웹의 `favicon.png` 로고와 같은 원본을 쓴다.
///
/// 라운드 모서리는 이미지에 이미 들어 있으므로 별도로 잘라내지 않는다.
class AppLogo extends StatelessWidget {
  const AppLogo({super.key, this.size = 32});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/logo.png',
      width: size,
      height: size,
      filterQuality: FilterQuality.medium,
      semanticLabel: 'trypto',
    );
  }
}
