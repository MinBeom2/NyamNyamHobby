export type ActivityPlace = {
  id: number;
  name: string;
  hobby: string;
  address: string;
  hours: string;
  price: string;
  indoor: boolean;
};

export type InfoFeedItem = {
  id: number;
  type: '축제·행사' | '강좌·클래스' | '공연·전시' | '입문 자료';
  hobby: string;
  title: string;
  summary: string;
  source: string;
  location: string;
  period: string;
  ended?: boolean;
};

export const placeFixtures: ActivityPlace[] = [
  { id: 1, name: '더클라임 마포점', hobby: '클라이밍', address: '서울 마포구 양화로 00', hours: '10:00~23:00', price: '일일 이용 25,000원', indoor: true },
  { id: 2, name: '클라이밍파크 홍대', hobby: '클라이밍', address: '서울 마포구 와우산로 00', hours: '12:00~23:00', price: '일일 이용 22,000원', indoor: true },
  { id: 3, name: '숨 요가 스튜디오', hobby: '요가', address: '서울 마포구 동교로 00', hours: '07:00~22:00', price: '1회 체험 20,000원', indoor: true },
];

export const infoFeedFixtures: InfoFeedItem[] = [
  { id: 1, type: '강좌·클래스', hobby: '클라이밍', title: '실내 클라이밍 입문 원데이 클래스', summary: '장비 대여 포함, 2시간 동안 기본 자세와 V0 문제를 함께 풀어 봐요.', source: '서울 공공서비스예약', location: '마포구민체육센터', period: '10.20~10.26' },
  { id: 2, type: '축제·행사', hobby: '클라이밍', title: '2026 생활체육 클라이밍 페스티벌', summary: '누구나 참여할 수 있는 볼더링 체험존과 입문자 대회가 열려요.', source: 'TourAPI', location: '서울시체육회', period: '10.25' },
  { id: 3, type: '공연·전시', hobby: '도예', title: '현대 도자 공예전 「흙의 시간」', summary: '신진 작가 20명의 생활 도자기를 전시해요.', source: '문화포털', location: '예술의전당', period: '10.03~11.10' },
  { id: 4, type: '입문 자료', hobby: '클라이밍', title: '처음 암장에 가기 전 알아두면 좋은 볼더링 용어 20', summary: '문제, 홀드, 완등 같은 기본 용어를 그림과 함께 정리했어요.', source: '남남취미 편집팀', location: '게시', period: '10.08' },
  { id: 5, type: '축제·행사', hobby: '요가', title: '한강 선셋 요가 클래스', summary: '종료된 행사 숨기기를 켜면 목록에서 제외돼요.', source: 'TourAPI', location: '마포구', period: '09.28', ended: true },
];

