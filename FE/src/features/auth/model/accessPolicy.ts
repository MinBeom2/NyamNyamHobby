const memberMenus: Array<[string, string]> = [
  ['/hobbies', '취미 찾기'],
  ['/places', '장소 찾기'],
  ['/info-feed', '취미 정보'],
  ['/community', '커뮤니티'],
  ['/my-hobbies', '내 취미'],
  ['/mypage', '마이페이지'],
  ['/notifications', '알림'],
  ['/support/inquiries', '고객 문의'],
  ['/home', '회원 홈'],
];

export function getMemberMenu(pathname: string) {
  return memberMenus.find(([path]) => pathname === path || pathname.startsWith(`${path}/`))?.[1] ?? null;
}
