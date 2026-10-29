export type HobbyDifficulty = '쉬움' | '보통' | '어려움';

export type HobbySummary = {
  id: number;
  name: string;
  description: string;
  category: string;
  relation: '관심' | '연관' | '추천';
  duration: string;
  cost: string;
  difficulty: HobbyDifficulty;
  place: '실내' | '실외' | '상관없음';
  social: '혼자' | '함께' | '상관없음';
  tags: string[];
};

export const hobbyFixtures: HobbySummary[] = [
  { id: 1, name: '클라이밍', description: '벽을 오르며 전신을 쓰는 실내 운동', category: '운동·야외', relation: '관심', duration: '1회 1~2시간', cost: '월 10~15만 원', difficulty: '보통', place: '실내', social: '상관없음', tags: ['성취감', '전신 운동', '집중'] },
  { id: 2, name: '요가', description: '호흡과 움직임으로 몸과 마음을 돌보는 활동', category: '운동·야외', relation: '관심', duration: '1회 30분~1시간', cost: '월 5~10만 원', difficulty: '쉬움', place: '실내', social: '혼자', tags: ['건강', '유연성', '마음챙김'] },
  { id: 3, name: '도예', description: '손으로 흙을 빚어 나만의 그릇 만들기', category: '공예·만들기', relation: '추천', duration: '1회 2~3시간', cost: '월 15~20만 원', difficulty: '보통', place: '실내', social: '상관없음', tags: ['창작', '집중', '결과물'] },
  { id: 4, name: '러닝', description: '신발 한 켤레로 바로 시작하는 유산소 운동', category: '운동·야외', relation: '연관', duration: '1회 30분~1시간', cost: '월 0~3만 원', difficulty: '쉬움', place: '실외', social: '혼자', tags: ['체력', '야외', '습관'] },
  { id: 5, name: '드로잉', description: '펜 하나로 일상을 기록하는 그림 취미', category: '음악·예술', relation: '추천', duration: '1회 30분~1시간', cost: '월 1~3만 원', difficulty: '쉬움', place: '상관없음', social: '혼자', tags: ['창작', '기록', '집중'] },
  { id: 6, name: '홈베이킹', description: '집에서 향긋한 빵과 디저트 만들기', category: '요리·생활', relation: '연관', duration: '1회 2~3시간', cost: '월 5~10만 원', difficulty: '보통', place: '실내', social: '혼자', tags: ['요리', '결과물', '나눔'] },
  { id: 7, name: '등산·트레킹', description: '자연 속 길을 걸으며 체력과 기분을 함께 돌보는 야외 활동', category: '운동·야외', relation: '연관', duration: '1회 3~5시간', cost: '초기 10만 원~', difficulty: '보통', place: '실외', social: '상관없음', tags: ['자연', '체력', '성취감'] },
  { id: 8, name: '명상', description: '호흡에 집중하며 몸과 마음을 차분하게 돌보는 활동', category: '운동·야외', relation: '연관', duration: '1회 10~30분', cost: '무료~', difficulty: '쉬움', place: '상관없음', social: '혼자', tags: ['마음챙김', '집중', '휴식'] },
];

export const hobbyCategories = ['전체', '운동·야외', '공예·만들기', '음악·예술', '요리·생활'] as const;

