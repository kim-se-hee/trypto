import { cn } from "@/lib/utils";

/**
 * 안드로이드 앱 직접 설치(APK) 안내.
 *
 * Play 스토어 심사를 통과하기 전이라 스토어 링크를 걸 수 없어, GitHub Releases 에 올린
 * APK 를 그대로 내려준다. 자산 이름을 버전과 무관하게 고정해 두면 `releases/latest` 경로가
 * 늘 최신 릴리스를 가리키므로, 앱을 새로 올릴 때 이 파일을 손댈 필요가 없다.
 */
const APK_URL = "https://github.com/kim-se-hee/trypto/releases/latest/download/trypto-android.apk";

const INSTALL_STEPS = [
  "위 버튼을 눌러 APK 파일을 내려받습니다",
  "브라우저가 묻는 '이 출처의 앱 설치 허용' 을 켭니다",
  "내려받은 파일을 열어 설치를 진행합니다",
];

/** 안드로이드 로봇 마크. 돔형 머리에 안테나와 눈만 남긴 축약형이다. */
function AndroidMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <g stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" fill="none">
        <path d="M8 10.2 5.9 6.4" />
        <path d="M16 10.2 18.1 6.4" />
      </g>
      <path
        fill="currentColor"
        fillRule="evenodd"
        clipRule="evenodd"
        d="M3.6 17.2a8.4 8.4 0 0 1 16.8 0 .7.7 0 0 1-.7.7H4.3a.7.7 0 0 1-.7-.7Zm5-5.35a1.15 1.15 0 1 0 0 2.3 1.15 1.15 0 0 0 0-2.3Zm6.8 0a1.15 1.15 0 1 0 0 2.3 1.15 1.15 0 0 0 0-2.3Z"
      />
    </svg>
  );
}

/**
 * APK 다운로드 버튼.
 *
 * `tone` 은 버튼이 놓인 배경을 뜻한다. `surface` 는 일반 배경 위의 외곽선 버튼,
 * `primary` 는 브랜드색 카드 위에 얹는 반투명 흰색 버튼이다.
 */
export function AndroidDownloadButton({
  tone = "surface",
  className,
}: {
  tone?: "surface" | "primary";
  className?: string;
}) {
  return (
    <a
      href={APK_URL}
      rel="noopener"
      className={cn(
        "flex h-14 items-center gap-2.5 rounded-full border px-7 text-base font-extrabold transition-all duration-150 hover:-translate-y-0.5 hover:shadow-lg active:scale-[0.98]",
        tone === "surface"
          ? "border-border bg-card text-foreground shadow-sm hover:border-foreground/25"
          : "border-white/40 bg-white/10 text-white backdrop-blur-sm hover:bg-white/20",
        className,
      )}
    >
      <AndroidMark className="h-5 w-5 shrink-0" />
      안드로이드 앱 받기
    </a>
  );
}

/**
 * 펼치면 나오는 설치 절차 안내.
 *
 * 스토어를 거치지 않는 설치라 경고 화면을 만나게 되므로, 절차를 미리 밝혀
 * 사용자가 설치를 중간에 포기하지 않게 한다.
 */
export function AndroidInstallGuide() {
  return (
    <details className="group inline-block text-center">
      <summary className="cursor-pointer list-none text-[13px] font-bold text-muted-foreground underline decoration-dotted underline-offset-4 transition-colors hover:text-foreground">
        설치 방법 보기
        <span className="ml-1 inline-block transition-transform group-open:rotate-180">▾</span>
      </summary>

      <ol className="mt-3 max-w-xs space-y-2 rounded-2xl border border-border bg-card px-4 py-3.5 text-left text-[13px] leading-relaxed text-muted-foreground">
        {INSTALL_STEPS.map((step, index) => (
          <li key={step} className="flex gap-2.5">
            <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand/10 text-[11px] font-extrabold text-brand">
              {index + 1}
            </span>
            <span>{step}</span>
          </li>
        ))}
      </ol>
    </details>
  );
}
