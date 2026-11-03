import Lenis from 'lenis';
import 'lenis/dist/lenis.css';

// 사이트 전체 휠·트랙패드 스크롤을 부드럽게 감속시킨다(Lenis).
// 운영체제의 '움직임 줄이기'가 켜져 있으면 Lenis가 감속 없이 즉시 스크롤로 바꾼다(기본 동작).
// 모달 배경처럼 안에서 따로 스크롤되는 영역은 allowNestedScroll로 그대로 둔다.
let lenis: Lenis | null = null;

export function startSmoothScroll() {
  if (!lenis) {
    lenis = new Lenis({ autoRaf: true, allowNestedScroll: true, autoToggle: true, stopInertiaOnNavigate: true });
  }
  return () => {
    lenis?.destroy();
    lenis = null;
  };
}

type ScrollOptions = { immediate?: boolean };

// 고정 헤더만큼 띄우는 scroll-margin-top을 Lenis 이동에도 똑같이 적용한다
function scrollMarginTop(element: HTMLElement) {
  return Number.parseFloat(getComputedStyle(element).scrollMarginTop) || 0;
}

export function scrollToElement(element: HTMLElement | null, { immediate = false }: ScrollOptions = {}) {
  if (!element) return;
  if (lenis) {
    lenis.scrollTo(element, { offset: -scrollMarginTop(element), immediate, force: true });
    return;
  }
  element.scrollIntoView({ block: 'start', behavior: immediate ? 'auto' : 'smooth' });
}

export function scrollToTop({ immediate = false }: ScrollOptions = {}) {
  if (lenis) {
    lenis.scrollTo(0, { immediate, force: true });
    return;
  }
  window.scrollTo({ top: 0, left: 0, behavior: immediate ? 'auto' : 'smooth' });
}
