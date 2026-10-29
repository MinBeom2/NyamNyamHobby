export type TodoItem = {
  id: number;
  title: string;
  description: string;
  done: boolean;
};

export type ActivityPlan = {
  week: number;
  title: string;
  status: '완료' | '진행 중' | '예정';
  todos: TodoItem[];
};

export type ActiveHobby = {
  id: number;
  name: string;
  week: number;
  totalWeeks: number;
  completed: number;
  totalTodos: number;
  status: '진행 중' | '일시중지';
  plans: ActivityPlan[];
};

export const activeHobbyFixtures: ActiveHobby[] = [
  {
    id: 1, name: '클라이밍', week: 2, totalWeeks: 4, completed: 5, totalTodos: 16, status: '진행 중',
    plans: [
      { week: 1, title: '클라이밍과 친해지기', status: '완료', todos: [{ id: 11, title: '기본 장비 이름 알아보기', description: '암벽화와 초크의 쓰임을 확인해요.', done: true }, { id: 12, title: '가까운 암장 찾아보기', description: '생활 반경 안의 암장을 하나 저장해요.', done: true }] },
      { week: 2, title: '기본 동작 익히기', status: '진행 중', todos: [{ id: 21, title: '안전 수칙 확인하기', description: '낙법과 매트 이용 수칙을 익혀요.', done: true }, { id: 22, title: 'V0 루트 완등하기', description: '쉬운 루트를 한 개 골라 천천히 도전해요.', done: false }, { id: 23, title: '활동 메모 남기기', description: '어려웠던 점과 재미있던 점을 기록해요.', done: false }] },
      { week: 3, title: '나에게 맞는 루트 찾기', status: '예정', todos: [{ id: 31, title: '서로 다른 유형의 루트 도전', description: '밸런스와 힘을 쓰는 루트를 비교해요.', done: false }] },
      { week: 4, title: '첫 목표 완성하기', status: '예정', todos: [{ id: 41, title: '프로젝트 루트 완등', description: '한 주 동안 도전할 목표 루트를 정해요.', done: false }] },
    ],
  },
  {
    id: 2, name: '요가', week: 1, totalWeeks: 4, completed: 1, totalTodos: 10, status: '진행 중',
    plans: [{ week: 1, title: '매트 위 10분', status: '진행 중', todos: [{ id: 51, title: '호흡 3분', description: '편안하게 앉아 호흡에 집중해요.', done: true }, { id: 52, title: '기초 스트레칭', description: '목과 어깨를 부드럽게 풀어요.', done: false }] }],
  },
  {
    id: 3, name: '도예', week: 3, totalWeeks: 4, completed: 4, totalTodos: 12, status: '일시중지',
    plans: [{ week: 3, title: '물레 위 첫 그릇', status: '진행 중', todos: [{ id: 61, title: '물레 중심 잡기', description: '흙의 중심을 안정적으로 잡아 봐요.', done: false }] }],
  },
];

