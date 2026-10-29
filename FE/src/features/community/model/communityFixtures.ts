export type CommunityPost = {
  id: number;
  category: '후기' | '질문' | '정보 공유' | '모임';
  hobby: string;
  title: string;
  author: string;
  date: string;
  views: number;
  comments: number;
  likes: number;
  content: string;
};

export type CommunityReply = {
  id: number;
  author: string;
  avatar: string;
  content: string;
  date: string;
  likes: number;
  mine?: boolean;
};

export type CommunityComment = {
  id: number;
  author: string;
  avatar: string;
  content: string;
  date: string;
  likes: number;
  mine?: boolean;
  deleted?: boolean;
  replies: CommunityReply[];
};

export const communityPostFixtures: CommunityPost[] = [
  { id: 1, category: '후기', hobby: '클라이밍', title: '클라이밍 입문 한 달 후기', author: '취미찾는사람', date: '10.15', views: 128, comments: 12, likes: 24, content: '처음에는 높은 곳이 무서웠는데 쉬운 문제부터 하나씩 완등하면서 재미가 붙었어요. 암벽화는 첫날부터 구매하기보다 대여로 몇 번 신어 보고 고르는 걸 추천해요.' },
  { id: 2, category: '질문', hobby: '클라이밍', title: '첫 암벽화 사이즈 어떻게 고르세요?', author: '초보클라이머', date: '10.15', views: 64, comments: 8, likes: 11, content: '운동화보다 작게 신는다는 말을 들었는데 너무 아픈 건 정상인가요?' },
  { id: 3, category: '정보 공유', hobby: '요가', title: '매일 열기 좋은 10분 요가 영상', author: '아침루틴', date: '10.14', views: 230, comments: 31, likes: 48, content: '부담 없이 따라 하기 좋은 영상과 동작 순서를 정리했어요.' },
  { id: 4, category: '모임', hobby: '러닝', title: '이번 주 일요일 한강 5km 같이 뛰어요', author: '천천히달려요', date: '10.13', views: 92, comments: 14, likes: 19, content: '대화 가능한 속도로 천천히 달리는 초보 러닝 모임입니다.' },
  { id: 5, category: '후기', hobby: '도예', title: '물레 원데이 클래스 다녀왔어요', author: '흙좋아', date: '10.12', views: 73, comments: 5, likes: 9, content: '생각보다 중심 잡기가 어려웠지만 선생님 도움으로 작은 그릇을 완성했어요.' },
  { id: 6, category: '질문', hobby: '드로잉', title: '아이패드 없이 디지털 드로잉 가능할까요?', author: '그림초보', date: '10.11', views: 101, comments: 9, likes: 13, content: '입문 장비를 고르고 있습니다. 꼭 태블릿이 필요한지 궁금해요.' },
];

export const communityCommentFixtures: CommunityComment[] = [
  { id: 1, author: '홀드요정', avatar: '홀', content: '저도 한 달 차인데 공감돼요! 손가락 테이핑은 어떻게 하세요?', date: '10.15', likes: 2, replies: [{ id: 11, author: '취미찾는사람', avatar: '취', content: '처음에는 얇게 감고, 통증이 있으면 바로 쉬는 편이에요.', date: '10.15', likes: 1, mine: true }] },
  { id: 2, author: '볼더링초보', avatar: '볼', content: '손가락 마디에 감으니 훨씬 덜 아파요. 유튜브에 영상 많아요.', date: '10.15', likes: 2, replies: [] },
  { id: 3, author: '', avatar: '', content: '', date: '', likes: 0, deleted: true, replies: [] },
  { id: 4, author: '클밍러', avatar: '클', content: '마포점은 평일 저녁이 한산해서 연습하기 좋아요.', date: '10.16', likes: 0, mine: true, replies: [] },
];
