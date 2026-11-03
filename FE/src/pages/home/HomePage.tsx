import { ChartNoAxesColumnIncreasing, Check, CircleDollarSign, Clock3, MessageCircleQuestion, Play, Search } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/model/authStore';
import { activityQueryKeys, getTodoSummary } from '../../features/activity/api/activityApi';
import { getHobbies, hobbyQueryKeys } from '../../features/hobby/api/hobbyApi';
import { toHobbyCardModel, type HobbyCardModel } from '../../features/hobby/model/hobbyAdapter';
import { deleteUserHobby, getUserHobbies, saveUserHobby, userQueryKeys, type UserHobbyDto } from '../../features/user/api/userApi';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';
import { MediaPlaceholder } from '../../shared/ui/MediaPlaceholder';
import { characterAssets, heroAssets } from '../../shared/assets/assets';

function PageShell({
  variant,
  children,
}: {
  variant?: 'guest';
  children: React.ReactNode;
}) {
  return (
    <div className={variant === 'guest' ? 'site-shell site-shell--guest' : 'site-shell'}>
      <AppHeader notificationCount={3} />
      <main>{children}</main>
    </div>
  );
}

function HobbyCard({ hobby }: { hobby: HobbyCardModel }) {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const savedHobbiesQuery = useQuery({
    queryKey: userQueryKeys.hobbies(),
    queryFn: () => getUserHobbies(),
    enabled: Boolean(user),
  });
  const saved = savedHobbiesQuery.data?.some((item) => item.hobbyId === hobby.id && item.status === 'SAVED') ?? false;
  const saveMutation = useMutation({
    mutationFn: () => saved ? deleteUserHobby(hobby.id) : saveUserHobby(hobby.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: userQueryKeys.hobbies() }),
  });

  return (
    <article className="hobby-card">
      <div className="hobby-card__visual">
        {hobby.relation && <span className="hobby-card__relation">{hobby.relation}</span>}
        {hobby.thumbnailUrl
          ? <img src={hobby.thumbnailUrl} alt="" />
          : <MediaPlaceholder />}
      </div>
      <div className="hobby-card__body">
        <h3>{hobby.name}</h3>
        <p>{hobby.description}</p>
        <div className="hobby-card__meta">
          <span><Clock3 aria-hidden="true" />{hobby.duration}</span>
          <span><CircleDollarSign aria-hidden="true" />{hobby.cost}</span>
          <span><ChartNoAxesColumnIncreasing aria-hidden="true" />{hobby.difficulty}</span>
        </div>
        <div className="hobby-card__actions">
          <Button
            variant="secondary"
            disabled={saveMutation.isPending}
            onClick={() => user ? saveMutation.mutate() : navigate('/login')}
          >
            {saved ? '저장 해제' : '관심 저장'}
          </Button>
          <Button to={`/hobbies/${hobby.id}`}>상세 보기</Button>
        </div>
      </div>
    </article>
  );
}

export function GuestHomePage() {
  const popularQuery = useQuery({
    queryKey: hobbyQueryKeys.list({ sort: 'POPULAR', size: 4 }),
    queryFn: () => getHobbies({ sort: 'POPULAR', size: 4 }),
  });
  const popularHobbies = popularQuery.data?.content.map(toHobbyCardModel) ?? [];

  return (
    <PageShell variant="guest">
      <div className="page-container guest-home">
        <div className="guest-above-fold">
          <section className="guest-hero">
            <div className="guest-hero__copy">
              <span className="eyebrow">취미를 더 빨리 찾고 즐기자</span>
              <h1>나에게 맞는 취미,<br />숏폼으로 찾아보세요</h1>
              <p>영상 몇 개만 보고 좋아요·별로예요를 누르면 시간·비용까지 맞춘 취미를 추천해 드려요.</p>
              <div className="guest-hero__actions">
                <Button to="/signup">무료로 시작하기</Button>
                <Button to="/onboarding/interests?mode=guest" variant="secondary">비회원 체험하기</Button>
              </div>
              <small>로그인하지 않으면 탐색 기록은 7일 뒤 사라져요</small>
            </div>

            <div className="guest-hero__visual">
              {/* 움직임 줄이기 설정을 켠 사용자에게는 멈춘 장면을 보여 준다 */}
              <picture className="guest-hero__animation">
                <source media="(prefers-reduced-motion: reduce)" srcSet={heroAssets.shortsStill} />
                <img src={heroAssets.shortsAnimation} width={400} height={378}
                  alt="냠냠취미 캐릭터가 클라이밍·드로잉 숏폼을 넘겨 보며 좋아요와 보관을 누르는 모습" />
              </picture>
            </div>
          </section>

          <section className="value-grid" aria-label="서비스 특징">
            <article><i><Play aria-hidden="true" size={14} fill="currentColor" /></i><h2>숏폼 탐색</h2><p>영상에 반응만 하면 관심사가 쌓여요</p></article>
            <article><i><Search aria-hidden="true" size={15} /></i><h2>맞춤 추천</h2><p>시간·비용·난이도까지 맞춘 취미를 골라 드려요</p></article>
            <article><i><Check aria-hidden="true" size={16} /></i><h2>주차별 TODO</h2><p>첫 활동까지 작은 단계로 나눠 안내해요</p></article>
          </section>
        </div>

        <section className="section-block">
          <div className="section-heading"><h2>지금 인기 있는 취미</h2><Link to="/hobbies">전체 보기</Link></div>
          <div className="hobby-grid">
            {popularQuery.isPending && <p>인기 취미를 불러오고 있어요.</p>}
            {popularQuery.isError && <p>인기 취미를 불러오지 못했어요.</p>}
            {popularHobbies.map((hobby) => <HobbyCard key={hobby.id} hobby={hobby} />)}
          </div>
        </section>
      </div>
    </PageShell>
  );
}

export function MemberHomePage() {
  const user = useAuthStore((state) => state.user);
  const recommendedQuery = useQuery({
    queryKey: hobbyQueryKeys.list({ sort: 'RECOMMEND', size: 4 }),
    queryFn: () => getHobbies({ sort: 'RECOMMEND', size: 4 }),
  });
  const todoSummaryQuery = useQuery({ queryKey: activityQueryKeys.summary(), queryFn: getTodoSummary });
  const recommendedHobbies = recommendedQuery.data?.content.map(toHobbyCardModel) ?? [];
  const activeHobbies = todoSummaryQuery.data?.plans.filter((item) => item.status === 'IN_PROGRESS' || item.status === 'PAUSED').slice(0, 2) ?? [];
  const remainingTodoCount = todoSummaryQuery.data?.remainingThisWeekCount;

  return (
    <PageShell
    >
      <div className="page-container member-home">
        <section className="member-hero">
          <div className="member-hero__copy">
            <h1>안녕하세요, {user?.nickname ?? '취미찾는사람'}님!</h1>
            <p>숏폼으로 관심사를 찾고, 맞춤 취미를 추천받아 보세요. {remainingTodoCount === undefined ? '이번 주 TODO를 확인해 보세요.' : `이번 주 TODO는 ${remainingTodoCount}개 남았어요.`}</p>
            <div><Button to="/explore">숏폼 탐색 이어하기</Button><Button to="/hobbies/interests" variant="secondary">관심사 직접 선택</Button></div>
          </div>
          <div className="member-hero__media">
            <img src={characterAssets.greeting} alt="인사하는 냠냠취미 캐릭터" />
            <MediaPlaceholder dark label="▶" />
            <MediaPlaceholder dark label="▶" />
          </div>
        </section>

        <section className="section-block recommendations">
          <div className="section-heading"><h2>맞춤 추천 취미</h2><Link to="/hobbies/recommendations">추천 전체 보기</Link></div>
          <div className="hobby-grid">
            {recommendedQuery.isPending && <p>추천 취미를 불러오고 있어요.</p>}
            {recommendedQuery.isError && <p>추천 취미를 불러오지 못했어요.</p>}
            {recommendedHobbies.map((hobby) => <HobbyCard key={hobby.id} hobby={hobby} />)}
          </div>
        </section>

        <div className="dashboard-grid">
          <section className="dashboard-panel todo-panel">
            <div className="panel-heading"><h2>진행 중인 TODO</h2><Link to="/my-hobbies">내 취미</Link></div>
            {todoSummaryQuery.isPending && <p>진행 중인 계획을 불러오고 있어요.</p>}
            {todoSummaryQuery.isError && <p>진행 중인 계획을 불러오지 못했어요.</p>}
            {!todoSummaryQuery.isPending && !todoSummaryQuery.isError && activeHobbies.length === 0 && <p>진행 중인 취미가 없어요.</p>}
            {activeHobbies.map((hobby) => <TodoRow key={hobby.userHobbyId} hobby={hobby} />)}
          </section>

          <section className="dashboard-panel news-panel">
            <div className="panel-heading"><h2>관심 취미 새 소식</h2><Link to="/info-feed">정보 피드</Link></div>
            <NewsRow feedId={1} badge="클래스" title="실내 클라이밍 입문 원데이 클래스" meta="마포구 · 10.20~10.26" />
            <NewsRow feedId={2} badge="행사" title="2026 생활체육 클라이밍 페스티벌" meta="서울 · 10.25(토)" orange />
            <NewsRow feedId={4} badge="입문 자료" title="처음 암장에 가기 전 알아두면 좋은 볼더링 용어 20" meta="게시 10.08" />
          </section>
        </div>

        <section className="support-cta" aria-labelledby="support-cta-title">
          <div className="support-cta__icon"><MessageCircleQuestion aria-hidden="true" /></div>
          <div className="support-cta__copy">
            <h2 id="support-cta-title">도움이 필요하신가요?</h2>
            <p>서비스 이용 중 궁금하거나 불편한 점을 남겨 주시면 확인 후 답변해 드려요.</p>
          </div>
          <Button to="/support/inquiries">문의하기</Button>
        </section>
      </div>
    </PageShell>
  );
}

function TodoRow({ hobby }: { hobby: UserHobbyDto }) {
  const progress = hobby.weekProgress?.rate ?? hobby.planProgress?.rate ?? hobby.progressRate;
  const step = [hobby.currentWeek ? `${hobby.currentWeek}주차` : null, hobby.weekTitle].filter(Boolean).join(' · ');
  const target = hobby.planId ? `/my-hobbies/${hobby.hobbyId}/plan?planId=${hobby.planId}` : `/hobbies/${hobby.hobbyId}`;
  return (
    <Link aria-label={`${hobby.name} 진행 중인 계획 보기`} className="todo-row" to={target}>
      <div className="todo-row__title"><strong>{hobby.name}</strong><span>{step || '계획 준비 중'}</span><small>남은 활동 {hobby.remainingThisWeekCount}개</small></div>
      <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
    </Link>
  );
}

function NewsRow({ feedId, badge, title, meta, orange = false }: { feedId: number; badge: string; title: string; meta: string; orange?: boolean }) {
  return (
    <Link aria-label={`${title} 정보 피드에서 보기`} className="news-row" to={`/info-feed/${feedId}`}>
      <span className={orange ? 'badge badge--orange' : 'badge'}>{badge}</span>
      <strong>{title}</strong>
      <small>{meta}</small>
    </Link>
  );
}
