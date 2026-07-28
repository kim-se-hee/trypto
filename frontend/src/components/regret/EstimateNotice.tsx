import { useState } from "react";
import { CircleHelp } from "lucide-react";

/**
 * 복기 그래프가 추정값이라는 안내. 손가락에는 hover 가 없어 올려도, 눌러도 열리게 한다.
 */
export function EstimateNotice() {
  const [open, setOpen] = useState(false);

  return (
    <span
      className="relative inline-flex"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        type="button"
        aria-label="복기 그래프 유의 사항"
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
        onBlur={() => setOpen(false)}
        onKeyDown={(e) => {
          if (e.key === "Escape") setOpen(false);
        }}
        className="text-muted-foreground transition-colors hover:text-foreground focus-visible:text-foreground focus-visible:outline-none"
      >
        <CircleHelp className="size-3.5" />
      </button>

      {open && (
        <div
          role="tooltip"
          className="absolute left-0 top-6 z-20 w-[280px] rounded-lg border border-border bg-popover p-3 text-popover-foreground shadow-lg sm:w-[320px]"
        >
          <p className="text-xs font-semibold">유의 사항</p>
          <ul className="mt-2 list-disc space-y-1 pl-4 text-[11px] leading-relaxed text-muted-foreground">
            <li>
              규칙 준수 시 자산은 <span className="font-medium text-foreground">추정값</span>으로 참고용입니다.
            </li>
            <li>바이낸스 지갑의 금액은 1 USDT = 1,400원 고정 환율로 환산됩니다.</li>
          </ul>
        </div>
      )}
    </span>
  );
}
