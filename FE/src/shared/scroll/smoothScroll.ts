import Lenis from 'lenis';
import 'lenis/dist/lenis.css';

// 사이트 전체 휠·트랙패드 스크롤을 부드럽게 감속시킨다(Lenis).
// 운영체제의 '움직임 줄이기'가 켜져 있으면 Lenis가 감속 없이 즉시 스크롤로 바꾼다(기본 동작).
// 모달 배경처럼 안에서 따로 스크롤되는 영역은 allowNestedScroll로 그대로 둔다.
let lenis: Lenis | null = null;

// 화면 단위로 넘기는 페이지(취미 상세)가 휠 입력을 먼저 받아 처리할 수 있게 한다.
// false를 돌려주면 Lenis는 그 입력을 무시한다(직접 preventDefault 후 scrollTo로 이동).
type WheelInput = { deltaY: number; event: WheelEvent | TouchEvent };
type WheelInterceptor = (input: WheelInput) => boolean;
let wheelInterceptor: WheelInterceptor | null = null;
// 코드로 이동하는 중에는 휠 입력을 막아 한 번에 한 화면만 넘어가게 한다
let moving = false;

export function startSmoothScroll() {
  if (!lenis) {
    lenis = new Lenis({
      autoRaf: true,
      allowNestedScroll: true,
      autoToggle: true,
      stopInertiaOnNavigate: true,
      virtualScroll: ({ deltaY, event }) => {
        if (moving) {
          if (event.cancelable) event.preventDefault();
          return false;
        }
        return wheelInterceptor ? wheelInterceptor({ deltaY, event }) : true;
      },
    });
  }
  return () => {
    lenis?.destroy();
    lenis = null;
  };
}

export function setWheelInterceptor(interceptor: WheelInterceptor) {
  wheelInterceptor = interceptor;
  return () => {
    if (wheelInterceptor === interceptor) wheelInterceptor = null;
  };
}

type ScrollOptions = { immediate?: boolean };

function moveTo(target: number | HTMLElement, immediate: boolean) {
  if (!lenis) return false;
  moving = !immediate;
  // Lenis는 요소로 이동할 때 그 요소의 scroll-margin-top(고정 헤더 높이)을 스스로 반영한다
  lenis.scrollTo(target, { immediate, force: true, lock: !immediate, onComplete: () => { moving = false; } });
  return true;
}

export function scrollToElement(element: HTMLElement | null, { immediate = false }: ScrollOptions = {}) {
  if (!element) return;
  if (moveTo(element, immediate)) return;
  element.scrollIntoView({ block: 'start', behavior: immediate ? 'auto' : 'smooth' });
}

export function scrollToTop({ immediate = false }: ScrollOptions = {}) {
  if (moveTo(0, immediate)) return;
  window.scrollTo({ top: 0, left: 0, behavior: immediate ? 'auto' : 'smooth' });
}
