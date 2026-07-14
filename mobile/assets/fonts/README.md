# 폰트 에셋

웹은 CDN 으로 폰트를 받지만 앱은 번들이 필요하다(web-spec §8.6.3, plan §9-7).
아래 7개 파일을 이 디렉토리에 넣은 뒤 `pubspec.yaml` 의 `fonts:` 선언 주석을 해제한다.

| 파일 | 패밀리 | weight | 출처 |
|---|---|---|---|
| `Pretendard-Regular.otf` | Pretendard | 400 | https://github.com/orioncactus/pretendard (OFL 1.1) |
| `Pretendard-Medium.otf` | Pretendard | 500 | 동일 |
| `Pretendard-SemiBold.otf` | Pretendard | 600 | 동일 |
| `Pretendard-Bold.otf` | Pretendard | 700 | 동일 |
| `NotoSansKR-ExtraBold.otf` | NotoSansKR | 800 | https://fonts.google.com/noto/specimen/Noto+Sans+KR (OFL 1.1) |
| `RobotoMono-Medium.ttf` | RobotoMono | 500 | https://fonts.google.com/specimen/Roboto+Mono (Apache 2.0) |
| `RobotoMono-Bold.ttf` | RobotoMono | 700 | 동일 |

파일이 없는 동안 `core/theme/theme.dart` 의 `TryptoFonts` 패밀리는 시스템 폰트로 폴백된다.
빌드는 깨지지 않지만 타이포그래피가 웹과 어긋나므로, 화면 작업 전에 채워 넣어야 한다.
