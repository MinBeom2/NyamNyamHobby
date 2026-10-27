export type ExploreShort = {
  id: string;
  hobby: string;
  title: string;
  description: string;
  tags: string[];
  duration: string;
};

export const exploreShorts: ExploreShort[] = [
  { id: 'short-01', hobby: '클라이밍', title: '처음 암장에 가면 꼭 배우는 기본 자세', description: '발에 힘을 싣는 법부터 천천히 확인해 보세요.', tags: ['운동', '실내', '함께'], duration: '00:42' },
  { id: 'short-02', hobby: '요가', title: '아침을 깨우는 5분 요가', description: '매트 한 장으로 가볍게 하루를 시작해요.', tags: ['운동', '실내', '혼자'], duration: '00:36' },
  { id: 'short-03', hobby: '도예', title: '물레 위에서 첫 그릇 만들기', description: '흙이 그릇이 되는 순간을 짧게 만나 보세요.', tags: ['공예', '실내', '정적'], duration: '00:51' },
  { id: 'short-04', hobby: '러닝', title: '초보 러너의 첫 3km 페이스', description: '숨이 차지 않는 속도로 달리는 방법을 소개해요.', tags: ['운동', '야외', '혼자'], duration: '00:47' },
  { id: 'short-05', hobby: '홈베이킹', title: '노오븐 쿠키를 만드는 가장 쉬운 방법', description: '작은 도구만으로 달콤한 결과를 만들어요.', tags: ['요리', '실내', '혼자'], duration: '00:58' },
  { id: 'short-06', hobby: '등산', title: '가벼운 산행 전 배낭 챙기기', description: '첫 산행에 꼭 필요한 준비물만 골랐어요.', tags: ['운동', '야외', '함께'], duration: '00:45' },
  { id: 'short-07', hobby: '드로잉', title: '선 하나로 시작하는 여행 드로잉', description: '잘 그리려는 부담 없이 관찰부터 시작해요.', tags: ['예술', '실내', '혼자'], duration: '00:39' },
  { id: 'short-08', hobby: '사진', title: '휴대폰으로 분위기 있는 사진 찍기', description: '빛과 구도 두 가지만 바꿔도 사진이 달라져요.', tags: ['예술', '야외', '혼자'], duration: '00:44' },
  { id: 'short-09', hobby: '수영', title: '자유형 호흡이 편해지는 연습', description: '물속에서 서두르지 않는 호흡을 익혀요.', tags: ['운동', '실내', '혼자'], duration: '00:49' },
  { id: 'short-10', hobby: '뜨개질', title: '코바늘로 만드는 작은 컵받침', description: '한 가지 뜨기만 익혀 첫 작품을 완성해요.', tags: ['공예', '실내', '혼자'], duration: '00:55' },
  { id: 'short-11', hobby: '커피', title: '집에서 향을 살리는 핸드드립', description: '물줄기와 시간을 맞춰 취향을 찾아보세요.', tags: ['생활', '실내', '혼자'], duration: '00:41' },
  { id: 'short-12', hobby: '플랜테리어', title: '초보도 키우기 쉬운 식물 세 가지', description: '공간과 햇빛에 맞는 반려 식물을 골라요.', tags: ['생활', '실내', '혼자'], duration: '00:46' },
];

export function getExploreShorts(preferredHobbies: string[]) {
  if (preferredHobbies.length === 0) return exploreShorts;
  const preferred = new Set(preferredHobbies);
  return [...exploreShorts].sort((a, b) => Number(preferred.has(b.hobby)) - Number(preferred.has(a.hobby)));
}
