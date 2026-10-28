export type NoticeCategory = 'GUIDE' | 'MAINTENANCE' | 'UPDATE';

export type Notice = {
  id: number;
  category: NoticeCategory;
  title: string;
  content: string;
  publishedAt: string;
  pinned: boolean;
};

export const noticeCategoryLabels: Record<NoticeCategory, string> = {
  GUIDE: '안내',
  MAINTENANCE: '점검',
  UPDATE: '업데이트',
};

// GET /api/v1/notices 응답으로 교체하기 전 사용하는 P3 개발용 fixture다.
export const noticeFixtures: Notice[] = [
  {
    id: 5,
    category: 'MAINTENANCE',
    title: '10월 서비스 점검 안내',
    publishedAt: '2026.10.14',
    pinned: true,
    content: '보다 안정적인 서비스를 위해 아래 시간 동안 점검을 진행해요.\n\n· 일시: 2026년 10월 24일(토) 02:00~04:00\n\n· 영향: 점검 중에는 로그인과 숏폼 탐색을 이용할 수 없어요.\n\n· 진행 중인 TODO 기록은 그대로 유지돼요.',
  },
  { id: 4, category: 'UPDATE', title: '주차별 TODO 메모 기능이 추가됐어요', publishedAt: '2026.10.10', pinned: false, content: '주차별 TODO에 활동 메모를 남길 수 있는 기능이 추가됐어요.' },
  { id: 3, category: 'GUIDE', title: '개인정보 처리방침 개정 안내', publishedAt: '2026.10.05', pinned: false, content: '개인정보 처리방침이 개정되어 주요 변경 내용을 안내드려요.' },
  { id: 2, category: 'GUIDE', title: '냠냠취미 오픈 베타를 시작해요', publishedAt: '2026.10.01', pinned: false, content: '냠냠취미 오픈 베타에 참여해 주셔서 감사해요.' },
  { id: 1, category: 'UPDATE', title: '관심사 직접 선택에 분류가 늘었어요', publishedAt: '2026.09.28', pinned: false, content: '관심 취미 선택 화면에 새로운 취미 분류가 추가됐어요.' },
];
