export type HobbyCardModel = {
  id: number;
  title: string;
  description: string;
  relation?: string;
  duration: string;
  budget: string;
  difficulty: '쉬움' | '보통';
  badge?: string;
};

export const popularHobbies: HobbyCardModel[] = [
  { id: 1, title: '클라이밍', description: '벽을 오르며 전신을 쓰는 실내 운동', duration: '1회 1~2시간', budget: '월 10~15만 원', difficulty: '보통' },
  { id: 2, title: '도예', description: '손으로 빚어 나만의 그릇 만들기', duration: '1회 2~3시간', budget: '월 15~20만 원', difficulty: '보통' },
  { id: 3, title: '러닝', description: '러닝화 하나로 바로 시작', duration: '1회 30분~1시간', budget: '월 0~3만 원', difficulty: '쉬움' },
  { id: 4, title: '드로잉', description: '펜 하나로 일상을 기록해요', duration: '1회 30분~1시간', budget: '월 1~3만 원', difficulty: '쉬움' },
];

export const recommendedHobbies: HobbyCardModel[] = [
  { id: 1, title: '클라이밍', description: '관심 취미 · 좋아요 3회', relation: '관심', duration: '1회 1~2시간', budget: '월 10~15만 원', difficulty: '보통' },
  { id: 2, title: '요가', description: '관심 취미 · 좋아요 2회', relation: '관심', duration: '1회 30분~1시간', budget: '월 0~12만 원', difficulty: '보통' },
  { id: 3, title: '등산·트레킹', description: '연관 취미 · 클라이밍과 비슷해요', relation: '연관', duration: '1회 3~5시간', budget: '초기 10만 원~', difficulty: '보통' },
  { id: 4, title: '명상', description: '연관 취미 · 요가와 비슷해요', relation: '연관', duration: '1회 10~30분', budget: '무료~', difficulty: '쉬움' },
];
