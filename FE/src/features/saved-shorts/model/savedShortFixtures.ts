export type SavedShortFixture = {
  id: number;
  hobby: '클라이밍' | '요가' | '도예' | '홈베이킹';
  title: string;
  savedAt: string;
  playable: boolean;
};

export const savedShortFixtures: SavedShortFixture[] = [
  { id: 1, hobby: '클라이밍', title: '퇴근 후 1시간, 볼더링 첫 도전기', savedAt: '10.12', playable: true },
  { id: 2, hobby: '요가', title: '아침 10분 요가 루틴', savedAt: '10.11', playable: true },
  { id: 3, hobby: '도예', title: '물레 위 첫 그릇 만들기', savedAt: '10.10', playable: true },
  { id: 4, hobby: '클라이밍', title: '초보 암벽화 고르는 법', savedAt: '10.09', playable: false },
  { id: 5, hobby: '홈베이킹', title: '노오븐 쿠키 15분', savedAt: '10.08', playable: true },
  { id: 6, hobby: '클라이밍', title: '홀드 잡는 기본 자세', savedAt: '10.07', playable: true },
  { id: 7, hobby: '요가', title: '잠들기 전 스트레칭', savedAt: '10.06', playable: true },
  { id: 8, hobby: '도예', title: '도자기 유약의 기초', savedAt: '10.05', playable: true },
  { id: 9, hobby: '클라이밍', title: '클라이밍 안전 수칙', savedAt: '10.04', playable: true },
  { id: 10, hobby: '요가', title: '허리 긴장을 푸는 자세', savedAt: '10.03', playable: true },
  { id: 11, hobby: '도예', title: '손으로 빚는 작은 접시', savedAt: '10.02', playable: true },
  { id: 12, hobby: '홈베이킹', title: '초보 스콘 만들기', savedAt: '10.01', playable: true },
];

