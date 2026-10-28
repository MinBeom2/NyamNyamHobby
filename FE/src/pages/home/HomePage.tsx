import { ChartNoAxesColumnIncreasing, Check, CircleDollarSign, Clock3, MessageCircleQuestion, Play, Search } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';
import { MediaPlaceholder } from '../../shared/ui/MediaPlaceholder';
import { characterAssets } from '../../shared/assets/assets';
import { popularHobbies, recommendedHobbies, type HobbyCardModel } from './homeFixtures';

function PageShell({
  authenticated,
  variant,
  children,
  onLogout,
  logoutPending,
}: {
  authenticated?: boolean;
  variant?: 'guest';
  children: React.ReactNode;
  onLogout?: () => void;
  logoutPending?: boolean;
}) {
  return (
    <div className={variant === 'guest' ? 'site-shell site-shell--guest' : 'site-shell'}>
      <AppHeader
        authenticated={authenticated}
        notificationCount={3}
        onLogout={onLogout}
        logoutPending={logoutPending}
      />
      <main>{children}</main>
    </div>
  );
}

function HobbyCard({ hobby }: { hobby: HobbyCardModel }) {
  return (
    <article className="hobby-card">
      <div className="hobby-card__visual">
        {hobby.relation && <span className="hobby-card__relation">{hobby.relation}</span>}
        <MediaPlaceholder />
      </div>
      <div className="hobby-card__body">
        <h3>{hobby.title}</h3>
        <p>{hobby.description}</p>
        <div className="hobby-card__meta">
          <span><Clock3 aria-hidden="true" />{hobby.duration}</span>
          <span><CircleDollarSign aria-hidden="true" />{hobby.budget}</span>
          <span><ChartNoAxesColumnIncreasing aria-hidden="true" />{hobby.difficulty}</span>
        </div>
        <div className="hobby-card__actions">
          <Button variant="secondary">관심 저장</Button>
          <Button>상세 보기</Button>
        </div>
      </div>
    </article>
  );
}

export function GuestHomePage() {
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

            <div className="guest-hero__visual" aria-label="숏폼 미리보기">
              <img src={characterAssets.greeting} alt="카메라를 든 냠냠취미 캐릭터" />
              <MediaPlaceholder className="short-preview" dark label="▶" />
            </div>
          </section>

          <section className="value-grid" aria-label="서비스 특징">
            <article><i><Play aria-hidden="true" size={14} fill="currentColor" /></i><h2>숏폼 탐색</h2><p>영상에 반응만 하면 관심사가 쌓여요</p></article>
            <article><i><Search aria-hidden="true" size={15} /></i><h2>맞춤 추천</h2><p>시간·비용·난이도까지 맞춘 취미를 골라 드려요</p></article>
            <article><i><Check aria-hidden="true" size={16} /></i><h2>주차별 TODO</h2><p>첫 활동까지 작은 단계로 나눠 안내해요</p></article>
          </section>
        </div>

        <section className="section-block">
          <div className="section-heading"><h2>지금 인기 있는 취미</h2><a href="/hobbies">전체 보기</a></div>
          <div className="hobby-grid">
            {popularHobbies.map((hobby) => <HobbyCard key={hobby.id} hobby={hobby} />)}
          </div>
        </section>
      </div>
    </PageShell>
  );
}

export function MemberHomePage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession();
      navigate('/', { replace: true });
    },
  });

  return (
    <PageShell
      authenticated
      onLogout={() => logoutMutation.mutate()}
      logoutPending={logoutMutation.isPending}
    >
      <div className="page-container member-home">
        <section className="member-hero">
          <div className="member-hero__copy">
            <h1>안녕하세요, {user?.nickname ?? '취미찾는사람'}님!</h1>
            <p>숏폼으로 관심사를 찾고, 맞춤 취미를 추천받아 보세요. 이번 주 TODO는 3개 남았어요.</p>
            <div><Button to="/explore">숏폼 탐색 이어하기</Button><Button to="/hobbies" variant="secondary">관심사 직접 선택</Button></div>
          </div>
          <div className="member-hero__media">
            <img src={characterAssets.greeting} alt="인사하는 냠냠취미 캐릭터" />
            <MediaPlaceholder dark label="▶" />
            <MediaPlaceholder dark label="▶" />
          </div>
        </section>

        <section className="section-block recommendations">
          <div className="section-heading"><h2>맞춤 추천 취미</h2><a href="/hobbies/recommendations">추천 전체 보기</a></div>
          <div className="hobby-grid">
            {recommendedHobbies.map((hobby) => <HobbyCard key={hobby.id} hobby={hobby} />)}
          </div>
        </section>

        <div className="dashboard-grid">
          <section className="dashboard-panel todo-panel">
            <div className="panel-heading"><h2>진행 중인 TODO</h2><a href="/my-hobbies">내 취미</a></div>
            <TodoRow title="클라이밍" step="2주차 · 기본 동작 익히기" progress={70} remaining="남은 활동 2개" />
            <TodoRow title="요가" step="1주차 · 매트 위 10분" progress={28} remaining="남은 활동 1개" />
          </section>

          <section className="dashboard-panel news-panel">
            <div className="panel-heading"><h2>관심 취미 새 소식</h2><a href="/feed">정보 피드</a></div>
            <NewsRow badge="클래스" title="실내 클라이밍 입문 원데이 클래스" meta="마포구 · 10.20~10.26" />
            <NewsRow badge="행사" title="2026 생활체육 클라이밍 페스티벌" meta="서울 · 10.25(토)" orange />
            <NewsRow badge="입문 자료" title="처음 암장에 가기 전 알아두면 좋은 볼더링 용어 20" meta="게시 10.08" />
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

function TodoRow({ title, step, progress, remaining }: { title: string; step: string; progress: number; remaining: string }) {
  return (
    <div className="todo-row">
      <div className="todo-row__title"><strong>{title}</strong><span>{step}</span><small>{remaining}</small></div>
      <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
    </div>
  );
}

function NewsRow({ badge, title, meta, orange = false }: { badge: string; title: string; meta: string; orange?: boolean }) {
  return (
    <div className="news-row">
      <span className={orange ? 'badge badge--orange' : 'badge'}>{badge}</span>
      <strong>{title}</strong>
      <small>{meta}</small>
    </div>
  );
}
