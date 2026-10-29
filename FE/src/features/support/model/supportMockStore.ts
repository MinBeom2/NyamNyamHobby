import { create } from 'zustand';

export type InquiryCategory = 'PLAN' | 'EXPLORATION' | 'HOBBY' | 'ACCOUNT' | 'ETC';
export type InquiryStatus = 'PENDING' | 'ANSWERED';

export type Inquiry = {
  id: number;
  category: InquiryCategory;
  title: string;
  content: string;
  status: InquiryStatus;
  createdAt: string;
  answer: null | {
    content: string;
    answeredAt: string;
  };
};

export type AppNotification = {
  id: number;
  type: 'COMMENT' | 'TODO' | 'INQUIRY_ANSWER' | 'NOTICE' | 'INTEREST' | 'REPORT';
  title: string;
  description: string;
  createdAt: string;
  read: boolean;
  inquiryId?: number;
};

type SupportMockState = {
  inquiries: Inquiry[];
  notifications: AppNotification[];
  addInquiry: (inquiry: Pick<Inquiry, 'category' | 'title' | 'content'>) => number;
  markNotificationRead: (id: number) => void;
  markAllNotificationsRead: () => void;
};

export const inquiryCategoryLabels: Record<InquiryCategory, string> = {
  PLAN: '계획',
  EXPLORATION: '숏폼 탐색',
  HOBBY: '취미 찾기',
  ACCOUNT: '계정',
  ETC: '기타',
};

const inquiryFixtures: Inquiry[] = [
  { id: 3, category: 'PLAN', title: '계획대로 이어가기 질문할게요', content: '일시중지한 취미를 다시 이어가면 기존 진행 기록도 유지되나요?', status: 'PENDING', createdAt: '2026.10.15', answer: null },
  { id: 2, category: 'EXPLORATION', title: '추천 결과가 바뀌지 않아요', content: '숏폼을 모두 봤는데 추천 결과가 계속 같아요. 새 결과를 확인하고 싶어요.', status: 'ANSWERED', createdAt: '2026.10.14', answer: { content: '추천 결과는 다시 탐색을 누르면 새로 받아볼 수 있어요. 관심 취미와 이용 상황을 수정하면 변경한 조건이 다음 추천에 반영됩니다.', answeredAt: '2026.10.15' } },
  { id: 1, category: 'HOBBY', title: '낙법 연습 저장 방법 알려주세요', content: 'TODO에서 낙법 연습을 완료한 뒤 기록을 저장하는 방법이 궁금해요.', status: 'ANSWERED', createdAt: '2026.10.13', answer: { content: '활동을 완료한 뒤 체크 버튼을 누르면 자동으로 저장됩니다.', answeredAt: '2026.10.14' } },
];

const notificationFixtures: AppNotification[] = [
  { id: 6, type: 'COMMENT', title: '내 글에 댓글이 달렸어요', description: '클라이밍 입문 한 달 후기에 새 댓글이 등록됐어요.', createdAt: '10분 전', read: false },
  { id: 5, type: 'TODO', title: '이번 주 TODO가 준비됐어요', description: '클라이밍 2주차 활동 3개 · 요가 1주차 활동 2개', createdAt: '5시간 전', read: false },
  { id: 4, type: 'INQUIRY_ANSWER', title: '문의에 답변이 등록됐어요', description: '추천 결과가 바뀌지 않아요 문의에 답변했어요.', createdAt: '1일 전', read: false, inquiryId: 2 },
  { id: 3, type: 'NOTICE', title: '새 공지', description: '10월 서비스 점검 안내', createdAt: '10.14', read: true },
  { id: 2, type: 'INTEREST', title: '관심 취미 새 소식 3건', description: '도예 · 클라이밍 관련 행사와 클래스가 올라왔어요.', createdAt: '10.13', read: true },
  { id: 1, type: 'REPORT', title: '신고 처리가 완료됐어요', description: '신고하신 댓글을 검토해 숨김 처리했어요.', createdAt: '10.12', read: true },
];

export const useSupportMockStore = create<SupportMockState>((set, get) => ({
  inquiries: inquiryFixtures,
  notifications: notificationFixtures,
  addInquiry: (inquiry) => {
    const id = Math.max(0, ...get().inquiries.map((item) => item.id)) + 1;
    set((state) => ({ inquiries: [{ ...inquiry, id, status: 'PENDING', createdAt: '2026.10.28', answer: null }, ...state.inquiries] }));
    return id;
  },
  markNotificationRead: (id) => set((state) => ({ notifications: state.notifications.map((item) => item.id === id ? { ...item, read: true } : item) })),
  markAllNotificationsRead: () => set((state) => ({ notifications: state.notifications.map((item) => ({ ...item, read: true })) })),
}));
