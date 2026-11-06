import { useEffect, useRef } from 'react';

interface UseWheelStepNavigationOptions {
  enabled?: boolean;
  onNext: () => void;
  onPrevious?: () => void;
}

export function useWheelStepNavigation({
  enabled = true,
  onNext,
  onPrevious,
}: UseWheelStepNavigationOptions) {
  const nextRef = useRef(onNext);
  const previousRef = useRef(onPrevious);
  const wheelLockedRef = useRef(false);
  const unlockTimerRef = useRef<number | null>(null);

  nextRef.current = onNext;
  previousRef.current = onPrevious;

  useEffect(() => {
    if (!enabled) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    const previousHtmlOverflow = document.documentElement.style.overflow;
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';

    const handleWheel = (event: WheelEvent) => {
      if (Math.abs(event.deltaY) < 18 || Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return;

      event.preventDefault();
      if (wheelLockedRef.current) return;

      wheelLockedRef.current = true;
      if (event.deltaY > 0) nextRef.current();
      else previousRef.current?.();

      unlockTimerRef.current = window.setTimeout(() => {
        wheelLockedRef.current = false;
      }, 500);
    };

    window.addEventListener('wheel', handleWheel, { passive: false });

    return () => {
      window.removeEventListener('wheel', handleWheel);
      if (unlockTimerRef.current !== null) window.clearTimeout(unlockTimerRef.current);
      wheelLockedRef.current = false;
      document.body.style.overflow = previousBodyOverflow;
      document.documentElement.style.overflow = previousHtmlOverflow;
    };
  }, [enabled]);
}
