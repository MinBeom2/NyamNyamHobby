import { ChartNoAxesColumnIncreasing, Check, ChevronLeft, ChevronRight, CircleDollarSign, Clock3, MessageCircleQuestion, Play, Search } from 'lucide-react';
import { useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/model/authStore';
import { activityQueryKeys, getTodoSummary } from '../../features/activity/api/activityApi';
import { getHobbies, hobbyQueryKeys } from '../../features/hobby/api/hobbyApi';
import { toHobbyCardModel, type HobbyCardModel } from '../../features/hobby/model/hobbyAdapter';
import { recommendationKindLabels } from '../../features/recommendation/model/recommendationAdapter';
import { useLatestRecommendedHobbies } from '../../features/recommendation/model/useRecommendationResult';
import type { FeedHighlightModel } from '../../features/feed/model/feedAdapter';
import { useFeedHighlights } from '../../features/feed/model/useFeedList';
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

function HobbyCard({ hobby, badge }: { hobby: HobbyCardModel; badge?: { label: string; related: boolean } }) {
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
        {badge && <span className={badge.related ? 'hobby-card__kind hobby-card__kind--related' : 'hobby-card__kind'}>{badge.label}</span>}
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
        <div className="guest-stage">
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
  const sliderRef = useRef<HTMLDivElement>(null);
  const recommendedQuery = useLatestRecommendedHobbies(Boolean(user));
  const todoSummaryQuery = useQuery({ queryKey: activityQueryKeys.summary(), queryFn: getTodoSummary });
  const highlightsQuery = useFeedHighlights(5);
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
          <div className="section-heading">
            <div className="section-heading__title"><h2>맞춤 추천 취미</h2><p>쇼츠 탐색을 통해 추천받은 취미들이에요</p></div>
            {recommendedQuery.data && <SliderControls targetRef={sliderRef} />}
          </div>
          {recommendedQuery.isPending && <p>추천 취미를 불러오고 있어요.</p>}
          {recommendedQuery.isError && <p>추천 취미를 불러오지 못했어요.</p>}
          {recommendedQuery.data === null && (
            <div className="recommend-empty">
              <p>숏폼을 탐색하면 맞춤 취미를 추천해 드려요.</p>
              <Button to="/explore">숏폼 탐색하기</Button>
            </div>
          )}
          {recommendedQuery.data && (
            <div className="hobby-slider" ref={sliderRef} aria-label="맞춤 추천 취미 목록">
              {recommendedQuery.data.map((hobby) => (
                <HobbyCard
                  key={hobby.id}
                  hobby={hobby}
                  badge={{ label: recommendationKindLabels[hobby.kind], related: hobby.kind === 'RELATED' }}
                />
              ))}
            </div>
          )}
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
            <div className="panel-heading"><h2>관심 취미 새 소식</h2><Link to="/info-feed">전체 피드</Link></div>
            {highlightsQuery.isPending && <p>새 소식을 불러오고 있어요.</p>}
            {highlightsQuery.isError && <p>새 소식을 불러오지 못했어요.</p>}
            {highlightsQuery.data?.length === 0 && <p>아직 올라온 소식이 없어요.</p>}
            {highlightsQuery.data?.map((item) => <NewsRow key={item.id} item={item} />)}
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

/** 카드 한 장 너비만큼 좌우로 넘긴다. 터치·트랙패드로 직접 밀어도 된다. */
function SliderControls({ targetRef }: { targetRef: React.RefObject<HTMLDivElement | null> }) {
  const slide = (direction: 1 | -1) => {
    const slider = targetRef.current;
    if (!slider) return;
    const card = slider.firstElementChild as HTMLElement | null;
    const gap = Number.parseFloat(getComputedStyle(slider).columnGap) || 0;
    slider.scrollBy({ left: direction * ((card?.offsetWidth ?? slider.clientWidth) + gap), behavior: 'smooth' });
  };
  return (
    <div className="slider-controls">
      <button type="button" aria-label="이전 추천 취미" onClick={() => slide(-1)}><ChevronLeft aria-hidden="true" /></button>
      <button type="button" aria-label="다음 추천 취미" onClick={() => slide(1)}><ChevronRight aria-hidden="true" /></button>
    </div>
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

function NewsRow({ item }: { item: FeedHighlightModel }) {
  return (
    <a aria-label={`${item.isNew ? '새 소식, ' : ''}${item.title} 자세히 보기 (새 탭)`} className="news-row" href={item.sourceUrl} target="_blank" rel="noopener noreferrer">
      <i className={item.isNew ? 'news-row__dot news-row__dot--new' : 'news-row__dot'} aria-hidden="true" />
      <span className={item.accent ? 'badge badge--orange' : 'badge'}>{item.badge}</span>
      <strong title={item.title}>{item.title}</strong>
      <small>{item.meta}</small>
    </a>
  );
}
