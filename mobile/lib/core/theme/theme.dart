import 'package:flutter/material.dart';

import 'trypto_colors.dart';

/// 웹 색 토큰(`frontend/src/index.css:54-90`)의 단일 출처 (§8.1.3).
abstract final class TryptoPalette {
  static const background = Color(0xFFF8F7F4);
  static const foreground = Color(0xFF1A1A2E);
  static const card = Color(0xFFFFFFFF);
  static const primary = Color(0xFF6C5CE7);
  static const primaryForeground = Color(0xFFFFFFFF);

  /// `--secondary` · `--muted` · `--accent` · `--input` 네 토큰의 값이 같다.
  static const secondary = Color(0xFFF0EEEB);
  static const mutedForeground = Color(0xFF7C7C8A);
  static const destructive = Color(0xFFE85D75);
  static const border = Color(0xFFE8E6E1);
}

/// 폰트 파일은 저장소에 없다. `assets/fonts/README.md` 의 목록대로 파일을 넣고 pubspec 의
/// `fonts:` 선언을 해제하기 전까지 아래 패밀리는 시스템 폰트로 폴백된다.
abstract final class TryptoFonts {
  static const sans = 'Pretendard';
  static const display = 'NotoSansKR';
  static const mono = 'RobotoMono';
}

/// §8.3.1 — 기준값 `--radius: 10px`.
abstract final class TryptoRadius {
  static const xs = 2.0;
  static const base = 4.0;
  static const sm = 6.0;

  /// 버튼·입력의 기본 반경.
  static const md = 8.0;
  static const lg = 10.0;

  /// 카드·테이블 컨테이너의 기본 반경.
  static const xl = 14.0;

  /// 바텀시트 상단.
  static const xxl = 18.0;
}

/// §8.3.2 — Tailwind 4px 단위 스케일.
abstract final class TryptoSpacing {
  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 12.0;
  static const lg = 16.0;
  static const xl = 20.0;
  static const xxl = 24.0;

  /// 모바일 좌우 패딩. 웹의 `max-w-6xl` 제약은 버리고 이것만 유지한다.
  static const screen = 16.0;
}

/// §8.3.3 — `shadow-card` 의 1px 링은 그림자가 아니라 테두리이므로 여기엔 블러 성분만 담는다.
abstract final class TryptoShadows {
  static const xs = [
    BoxShadow(color: Color(0x0D000000), blurRadius: 2, offset: Offset(0, 1)),
  ];
  static const sm = [
    BoxShadow(color: Color(0x1A000000), blurRadius: 3, offset: Offset(0, 1)),
    BoxShadow(
      color: Color(0x1A000000),
      blurRadius: 2,
      offset: Offset(0, 1),
      spreadRadius: -1,
    ),
  ];
  static const cardHover = [
    BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
  ];
  static const cardActive = [
    BoxShadow(color: Color(0x0F000000), blurRadius: 12, offset: Offset(0, 4)),
  ];
}

/// §8.4 — 웹 애니메이션 상수.
abstract final class TryptoMotion {
  static const enterCurve = Cubic(0.22, 1.0, 0.36, 1.0);
  static const enter = Duration(milliseconds: 500);
  static const enterStagger = Duration(milliseconds: 80);
  static const dialog = Duration(milliseconds: 200);
  static const sheet = Duration(milliseconds: 300);
  static const colorTransition = Duration(milliseconds: 150);

  /// 가격 플래시. 페이드 없이 켜고 끈다 (§3.3.3).
  static const priceFlash = Duration(milliseconds: 100);
}

/// 웹 body 의 `font-feature-settings: "cv02","cv03","cv04","cv11"` 재현 (§8.2.2).
const _sansFeatures = [
  FontFeature('cv02'),
  FontFeature('cv03'),
  FontFeature('cv04'),
  FontFeature('cv11'),
];

TextStyle _sans(
  double size,
  double height,
  FontWeight weight, {
  double tracking = -0.01,
}) {
  return TextStyle(
    fontFamily: TryptoFonts.sans,
    fontSize: size,
    height: height,
    fontWeight: weight,
    letterSpacing: size * tracking,
    fontFeatures: _sansFeatures,
  );
}

/// 7개 페이지 h1 — `font-display text-3xl tracking-tight` (§8.6.3).
const _displayHeadline = TextStyle(
  fontFamily: TryptoFonts.display,
  fontSize: 30,
  height: 1.2,
  fontWeight: FontWeight.w800,
  letterSpacing: -0.75,
  color: TryptoPalette.foreground,
);

/// §8.6.3 의 매핑표 그대로.
final _textTheme = TextTheme(
  displaySmall: _sans(36, 1.1111, FontWeight.w700),
  headlineMedium: _displayHeadline,
  headlineSmall: _sans(24, 1.3333, FontWeight.w700),
  titleLarge: _sans(18, 1.5556, FontWeight.w600),
  titleMedium: _sans(16, 1.5, FontWeight.w600),
  bodyLarge: _sans(16, 1.5, FontWeight.w400),
  bodyMedium: _sans(14, 1.4286, FontWeight.w400),
  labelLarge: _sans(14, 1.4286, FontWeight.w500),
  bodySmall: _sans(13, 1.4, FontWeight.w600),
  labelMedium: _sans(12, 1.3333, FontWeight.w500),
  labelSmall: _sans(11, 1.36, FontWeight.w400),
);

/// TextTheme 슬롯에 없는 커스텀 스타일.
abstract final class TryptoText {
  /// `text-[10px]` — 초소형 배지.
  static const micro = TextStyle(
    fontFamily: TryptoFonts.sans,
    fontSize: 10,
    height: 1.4,
    fontWeight: FontWeight.w500,
    letterSpacing: -0.10,
    fontFeatures: _sansFeatures,
  );

  /// 코인 심볼 — `text-[13px] tracking-wide` (§8.2.6).
  static const symbol = TextStyle(
    fontFamily: TryptoFonts.sans,
    fontSize: 13,
    height: 1.4,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.325,
    fontFeatures: _sansFeatures,
  );

  /// 모든 수치 표시. 웹은 `font-mono` + `tabular-nums` 를 항상 함께 쓴다 (§8.2.1).
  static TextStyle numeric({
    double size = 14,
    FontWeight weight = FontWeight.w600,
    Color? color,
  }) {
    return TextStyle(
      fontFamily: TryptoFonts.mono,
      fontSize: size,
      fontWeight: weight,
      color: color,
      fontFeatures: const [FontFeature.tabularFigures()],
      letterSpacing: 0,
    );
  }
}

const _label14 = TextStyle(
  fontFamily: TryptoFonts.sans,
  fontSize: 14,
  height: 1.4286,
  fontWeight: FontWeight.w500,
  letterSpacing: -0.14,
  fontFeatures: _sansFeatures,
);

/// §8.6.1 — light 전용. 다크 토큰은 웹에 존재하지 않는다 (R13).
const _scheme = ColorScheme(
  brightness: Brightness.light,
  primary: TryptoPalette.primary,
  onPrimary: TryptoPalette.primaryForeground,
  secondary: TryptoPalette.secondary,
  onSecondary: TryptoPalette.foreground,
  error: TryptoPalette.destructive,
  onError: Color(0xFFFFFFFF),
  surface: TryptoPalette.card,
  onSurface: TryptoPalette.foreground,
  surfaceContainerLowest: TryptoPalette.card,
  surfaceContainerLow: TryptoPalette.background,
  surfaceContainer: TryptoPalette.secondary,
  surfaceContainerHigh: TryptoPalette.secondary,
  surfaceContainerHighest: TryptoPalette.secondary,
  onSurfaceVariant: TryptoPalette.mutedForeground,
  outline: TryptoPalette.border,
  outlineVariant: TryptoPalette.border,
);

/// 웹 Button 변형 중 M3 기본 슬롯에 대응이 없는 것들 (§8.6.4).
abstract final class TryptoButtons {
  static ButtonStyle get secondary => FilledButton.styleFrom(
    backgroundColor: TryptoPalette.secondary,
    foregroundColor: TryptoPalette.foreground,
    disabledBackgroundColor: TryptoPalette.secondary.withValues(alpha: 0.5),
    disabledForegroundColor: TryptoPalette.foreground.withValues(alpha: 0.5),
  ).merge(_buttonBase);

  static ButtonStyle get destructive => FilledButton.styleFrom(
    backgroundColor: TryptoPalette.destructive,
    foregroundColor: const Color(0xFFFFFFFF),
    disabledBackgroundColor: TryptoPalette.destructive.withValues(alpha: 0.5),
    disabledForegroundColor: const Color(0x80FFFFFF),
  ).merge(_buttonBase);

  static ButtonStyle get link => TextButton.styleFrom(
    foregroundColor: TryptoPalette.primary,
    minimumSize: const Size(0, 36),
    padding: const EdgeInsets.symmetric(horizontal: 8),
    textStyle: _label14,
    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
  );

  static final _buttonBase = FilledButton.styleFrom(
    elevation: 0,
    minimumSize: const Size(0, 36),
    padding: const EdgeInsets.symmetric(horizontal: 16),
    iconSize: 16,
    textStyle: _label14,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
    ),
    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
  );
}

/// 웹 Slider 의 썸은 흰 원 + 1px primary 링이다 (§8.6.4). M3 기본 핸들에는 링이 없다.
class _RingThumbShape extends SliderComponentShape {
  const _RingThumbShape();

  @override
  Size getPreferredSize(bool isEnabled, bool isDiscrete) => const Size(16, 16);

  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required Animation<double> activationAnimation,
    required Animation<double> enableAnimation,
    required bool isDiscrete,
    required TextPainter labelPainter,
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required TextDirection textDirection,
    required double value,
    required double textScaleFactor,
    required Size sizeWithOverflow,
  }) {
    final canvas = context.canvas;
    canvas.drawCircle(center, 8, Paint()..color = TryptoPalette.card);
    canvas.drawCircle(
      center,
      7.5,
      Paint()
        ..color = TryptoPalette.primary
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1,
    );
  }
}

ThemeData buildTryptoTheme() {
  final base = ThemeData(
    useMaterial3: true,
    colorScheme: _scheme,
    fontFamily: TryptoFonts.sans,
    scaffoldBackgroundColor: TryptoPalette.background,
    canvasColor: TryptoPalette.background,
    focusColor: TryptoPalette.primary,
    highlightColor: TryptoPalette.primary.withValues(alpha: 0.03),
    splashColor: TryptoPalette.primary.withValues(alpha: 0.04),
    extensions: const [TryptoColors.light],
    iconTheme: const IconThemeData(
      size: 16,
      color: TryptoPalette.mutedForeground,
    ),
    appBarTheme: const AppBarThemeData(
      toolbarHeight: 56,
      backgroundColor: TryptoPalette.background,
      foregroundColor: TryptoPalette.foreground,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      titleSpacing: TryptoSpacing.screen,
      titleTextStyle: TextStyle(
        fontFamily: TryptoFonts.sans,
        fontSize: 18,
        height: 1.5556,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.18,
        color: TryptoPalette.foreground,
      ),
      iconTheme: IconThemeData(size: 20, color: TryptoPalette.foreground),
      shape: Border(bottom: BorderSide(color: TryptoPalette.border)),
    ),
    cardTheme: const CardThemeData(
      color: TryptoPalette.card,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.xl)),
        side: BorderSide(color: TryptoPalette.border),
      ),
    ),
    dividerTheme: const DividerThemeData(
      color: TryptoPalette.border,
      thickness: 1,
      space: 1,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: TryptoPalette.primary,
        foregroundColor: TryptoPalette.primaryForeground,
        disabledBackgroundColor: TryptoPalette.primary.withValues(alpha: 0.5),
        disabledForegroundColor: const Color(0x80FFFFFF),
      ).merge(TryptoButtons._buttonBase),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        backgroundColor: TryptoPalette.background,
        foregroundColor: TryptoPalette.foreground,
        disabledForegroundColor: TryptoPalette.foreground.withValues(
          alpha: 0.5,
        ),
        side: const BorderSide(color: TryptoPalette.border),
        minimumSize: const Size(0, 36),
        padding: const EdgeInsets.symmetric(horizontal: 16),
        iconSize: 16,
        textStyle: _label14,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
        ),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: TryptoPalette.foreground,
        disabledForegroundColor: TryptoPalette.foreground.withValues(
          alpha: 0.5,
        ),
        backgroundColor: Colors.transparent,
        minimumSize: const Size(0, 36),
        padding: const EdgeInsets.symmetric(horizontal: 16),
        iconSize: 16,
        textStyle: _label14,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
        ),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
    ),
    inputDecorationTheme: InputDecorationThemeData(
      filled: false,
      isDense: true,
      constraints: const BoxConstraints(minHeight: 36),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      hintStyle: _sans(
        16,
        1.5,
        FontWeight.w400,
      ).copyWith(color: TryptoPalette.mutedForeground),
      enabledBorder: const OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
        borderSide: BorderSide(color: TryptoPalette.secondary),
      ),
      focusedBorder: const OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
        borderSide: BorderSide(color: TryptoPalette.primary),
      ),
      errorBorder: const OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
        borderSide: BorderSide(color: TryptoPalette.destructive),
      ),
      focusedErrorBorder: const OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
        borderSide: BorderSide(color: TryptoPalette.destructive),
      ),
      disabledBorder: OutlineInputBorder(
        borderRadius: const BorderRadius.all(Radius.circular(TryptoRadius.md)),
        borderSide: BorderSide(
          color: TryptoPalette.secondary.withValues(alpha: 0.5),
        ),
      ),
    ),
    tabBarTheme: TabBarThemeData(
      indicatorSize: TabBarIndicatorSize.tab,
      indicator: const UnderlineTabIndicator(
        borderSide: BorderSide(color: TryptoPalette.foreground, width: 2),
      ),
      labelColor: TryptoPalette.foreground,
      unselectedLabelColor: TryptoPalette.foreground.withValues(alpha: 0.6),
      labelStyle: _label14,
      unselectedLabelStyle: _label14,
      dividerColor: TryptoPalette.border,
      overlayColor: const WidgetStatePropertyAll(Colors.transparent),
    ),
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: TryptoPalette.card,
      surfaceTintColor: Colors.transparent,
      modalBackgroundColor: TryptoPalette.card,
      elevation: 0,
      modalElevation: 0,
      showDragHandle: true,
      dragHandleColor: TryptoPalette.border,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(TryptoRadius.xxl),
        ),
      ),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: TryptoPalette.card,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      barrierColor: const Color(0x80000000),
      insetPadding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.screen,
        vertical: TryptoSpacing.xxl,
      ),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.lg)),
        side: BorderSide(color: TryptoPalette.border),
      ),
      titleTextStyle: _sans(18, 1.5556, FontWeight.w600).copyWith(
        color: TryptoPalette.foreground,
      ),
      contentTextStyle: _sans(14, 1.4286, FontWeight.w400).copyWith(
        color: TryptoPalette.mutedForeground,
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: TryptoPalette.foreground,
      actionTextColor: TryptoPalette.card,
      elevation: 0,
      insetPadding: const EdgeInsets.all(TryptoSpacing.screen),
      contentTextStyle: _sans(
        14,
        1.4286,
        FontWeight.w400,
      ).copyWith(color: TryptoPalette.card),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(TryptoRadius.md)),
      ),
    ),
    sliderTheme: SliderThemeData(
      trackHeight: 6,
      activeTrackColor: TryptoPalette.primary,
      inactiveTrackColor: TryptoPalette.secondary,
      thumbColor: TryptoPalette.card,
      overlayColor: TryptoPalette.primary.withValues(alpha: 0.1),
      thumbShape: const _RingThumbShape(),
      overlayShape: const RoundSliderOverlayShape(overlayRadius: 12),
      trackShape: const RoundedRectSliderTrackShape(),
      showValueIndicator: ShowValueIndicator.never,
    ),
    switchTheme: SwitchThemeData(
      thumbColor: const WidgetStatePropertyAll(TryptoPalette.background),
      trackColor: WidgetStateProperty.resolveWith(
        (states) => states.contains(WidgetState.selected)
            ? TryptoPalette.primary
            : TryptoPalette.secondary,
      ),
      trackOutlineColor: const WidgetStatePropertyAll(Colors.transparent),
      trackOutlineWidth: const WidgetStatePropertyAll(0),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: TryptoPalette.secondary,
      selectedColor: TryptoPalette.primary.withValues(alpha: 0.18),
      side: const BorderSide(color: TryptoPalette.border),
      labelStyle: _sans(12, 1.3333, FontWeight.w500),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      showCheckmark: false,
      shape: const StadiumBorder(),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: TryptoPalette.card,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      height: 64,
      indicatorColor: TryptoPalette.primary.withValues(alpha: 0.1),
      indicatorShape: const StadiumBorder(),
      labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
      labelTextStyle: WidgetStatePropertyAll(_sans(11, 1.36, FontWeight.w500)),
      iconTheme: WidgetStateProperty.resolveWith(
        (states) => IconThemeData(
          size: 20,
          color: states.contains(WidgetState.selected)
              ? TryptoPalette.primary
              : TryptoPalette.mutedForeground,
        ),
      ),
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(
      color: TryptoPalette.primary,
      linearTrackColor: TryptoPalette.secondary,
      circularTrackColor: Colors.transparent,
    ),
    scrollbarTheme: const ScrollbarThemeData(
      thickness: WidgetStatePropertyAll(6),
      radius: Radius.circular(3),
      thumbColor: WidgetStatePropertyAll(Color(0xFFD5D3CE)),
      thumbVisibility: WidgetStatePropertyAll(false),
    ),
    textSelectionTheme: TextSelectionThemeData(
      cursorColor: TryptoPalette.primary,
      selectionColor: TryptoPalette.primary.withValues(alpha: 0.2),
      selectionHandleColor: TryptoPalette.primary,
    ),
  );

  // ThemeData(fontFamily:) 는 TextTheme 전 슬롯의 패밀리를 덮어쓴다. 제목용 NotoSansKR 을
  // 지키려면 textTheme 은 생성자 밖에서 얹어야 한다.
  return base.copyWith(
    textTheme: base.textTheme
        .merge(_textTheme)
        .apply(
          bodyColor: TryptoPalette.foreground,
          displayColor: TryptoPalette.foreground,
        )
        .copyWith(headlineMedium: _displayHeadline),
  );
}
