export type ActivityPlace = {
  id: number;
  name: string;
  hobby: string;
  address: string;
  hours: string;
  price: string;
  indoor: boolean;
};

export const placeFixtures: ActivityPlace[] = [
  { id: 1, name: '더클라임 마포점', hobby: '클라이밍', address: '서울 마포구 양화로 00', hours: '10:00~23:00', price: '일일 이용 25,000원', indoor: true },
  { id: 2, name: '클라이밍파크 홍대', hobby: '클라이밍', address: '서울 마포구 와우산로 00', hours: '12:00~23:00', price: '일일 이용 22,000원', indoor: true },
  { id: 3, name: '숨 요가 스튜디오', hobby: '요가', address: '서울 마포구 동교로 00', hours: '07:00~22:00', price: '1회 체험 20,000원', indoor: true },
];
