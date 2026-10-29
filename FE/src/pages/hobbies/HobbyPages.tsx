import { Check, ChevronDown, Clock3, Coins, Gauge, Heart, Image, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { hobbyCategories, hobbyFixtures, type HobbySummary } from '../../features/hobby/model/hobbyFixtures';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';

function HobbyCard({ hobby }: { hobby: HobbySummary }) {
  return (
    <article className="catalog-card">
      <div className="catalog-card__visual"><Image aria-hidden="true" /><span>이미지</span><b>{hobby.relation}</b></div>
      <div className="catalog-card__body">
        <span className="badge">{hobby.category}</span>
        <h2>{hobby.name}</h2>
        <p>{hobby.description}</p>
        <div className="catalog-card__meta"><span><Clock3 />{hobby.duration}</span><span><Coins />{hobby.cost}</span><span><Gauge />{hobby.difficulty}</span></div>
        <div className="catalog-card__actions"><button type="button"><Heart /> 관심 저장</button><Link to={`/hobbies/${hobby.id}`}>상세 보기</Link></div>
      </div>
    </article>
  );
}

const directInterestGroups = [
  { title: '운동·야외', items: ['클라이밍', '러닝', '등산', '요가', '수영', '배드민턴'] },
  { title: '공예·만들기', items: ['도예', '뜨개질', '가죽공예', '목공'] },
  { title: '음악·예술', items: ['드로잉', '우쿨렐레', '사진', '캘리그라피'] },
  { title: '요리·생활', items: ['홈베이킹', '커피', '플랜테리어'] },
  { title: '배움·교양', items: ['독서 모임', '외국어', '보드게임'] },
  { title: '일상·감성', items: ['산책', '필름 카메라', '다이어리 꾸미기'] },
] as const;

export function HobbyInterestSelectPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<string[]>(['클라이밍', '요가', '도예']);
  const normalizedKeyword = keyword.trim();

  const visibleGroups = directInterestGroups.map((group) => ({
    ...group,
    items: group.items.filter((item) => !normalizedKeyword || item.includes(normalizedKeyword)),
  })).filter((group) => group.items.length > 0);

  const toggleInterest = (interest: string) => {
    setSelected((current) => {
      if (current.includes(interest)) return current.filter((item) => item !== interest);
      if (current.length >= 10) return current;
      return [...current, interest];
    });
  };

  const showRecommendations = () => {
    const query = new URLSearchParams();
    if (selected.length > 0) query.set('interests', selected.join(','));
    navigate(`/hobbies/recommendations${query.size > 0 ? `?${query.toString()}` : ''}`);
  };

  return (
    <div className="site-shell"><AppHeader authenticated notificationCount={3} />
      <main className="page-container direct-interest-page">
        <header className="direct-interest-heading">
          <h1>관심사를 직접 골라 주세요</h1>
          <p>영상을 보지 않아도 추천을 받을 수 있어요. 최대 10개까지 고를 수 있어요.</p>
        </header>

        <label className="direct-interest-search">
          <span>취미 검색</span>
          <div><Search aria-hidden="true" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="취미명이나 태그로 찾기" /></div>
        </label>

        {visibleGroups.length > 0 ? (
          <section className="direct-interest-groups" aria-label="관심사 목록">
            {visibleGroups.map((group) => (
              <article className="direct-interest-group" key={group.title}>
                <h2>{group.title}</h2>
                <div>{group.items.map((interest) => {
                  const isSelected = selected.includes(interest);
                  return <button className={isSelected ? 'selected' : ''} type="button" key={interest} aria-pressed={isSelected} disabled={!isSelected && selected.length >= 10} onClick={() => toggleInterest(interest)}>{isSelected && <Check aria-hidden="true" />}{interest}</button>;
                })}</div>
              </article>
            ))}
          </section>
        ) : <section className="direct-interest-empty"><Search aria-hidden="true" /><p>검색한 취미가 없어요.</p></section>}

        <section className="direct-interest-summary" aria-label="선택한 관심사">
          <div className="direct-interest-summary__selection"><strong>선택한 관심사 {selected.length}개</strong><div>{selected.map((interest) => <button type="button" key={interest} onClick={() => toggleInterest(interest)}><Check aria-hidden="true" />{interest}</button>)}</div></div>
          <div className="direct-interest-summary__actions"><button type="button" onClick={() => setSelected([])}>선택 초기화</button><Button onClick={showRecommendations}>추천 결과 보기</Button></div>
        </section>
      </main>
    </div>
  );
}

export function HobbySearchPage({ recommended = false }: { recommended?: boolean }) {
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState<(typeof hobbyCategories)[number]>('전체');
  const [difficulty, setDifficulty] = useState('전체');
  const [place, setPlace] = useState('전체');

  const hobbies = useMemo(() => hobbyFixtures.filter((hobby) => {
    const queryMatched = !keyword.trim() || `${hobby.name} ${hobby.tags.join(' ')}`.includes(keyword.trim());
    return queryMatched && (category === '전체' || hobby.category === category) && (difficulty === '전체' || hobby.difficulty === difficulty) && (place === '전체' || hobby.place === place);
  }), [category, difficulty, keyword, place]);

  return (
    <div className="site-shell"><AppHeader authenticated notificationCount={3} />
      <main className="page-container discovery-page">
        <div className="discovery-title"><div><h1>{recommended ? '맞춤 취미 추천' : '취미 찾기'}</h1><p>{recommended ? '관심사와 이용 상황을 바탕으로 잘 맞는 취미를 모았어요.' : '취미명이나 태그로 찾고, 시간·비용·난이도로 좁혀 보세요.'}</p></div>{!recommended && <Button to="/hobbies/recommendations">맞춤 추천 보기</Button>}</div>
        <div className="search-bar"><Search aria-hidden="true" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="취미명이나 태그로 검색 (예: 실내, 혼자)" /><button type="button">검색</button></div>
        <div className="filter-row" aria-label="취미 필터">
          <select value={category} onChange={(event) => setCategory(event.target.value as (typeof hobbyCategories)[number])}>{hobbyCategories.map((item) => <option key={item}>{item}</option>)}</select>
          <select value={difficulty} onChange={(event) => setDifficulty(event.target.value)}><option>전체</option><option>쉬움</option><option>보통</option><option>어려움</option></select>
          <select value={place} onChange={(event) => setPlace(event.target.value)}><option>전체</option><option>실내</option><option>실외</option><option>상관없음</option></select>
          <button className="filter-reset" type="button" onClick={() => { setKeyword(''); setCategory('전체'); setDifficulty('전체'); setPlace('전체'); }}>초기화</button>
        </div>
        {hobbies.length > 0 ? <section className="catalog-grid" aria-label="취미 검색 결과">{hobbies.map((hobby) => <HobbyCard key={hobby.id} hobby={hobby} />)}</section> : <section className="empty-state"><Search aria-hidden="true" /><h2>조건에 맞는 취미가 없어요</h2><p>검색어나 조건을 하나씩 줄여 볼까요?</p><Button onClick={() => { setKeyword(''); setCategory('전체'); setDifficulty('전체'); setPlace('전체'); }}>모든 조건 해제</Button></section>}
      </main>
    </div>
  );
}

export function HobbyDetailPage() {
  const { hobbyId } = useParams();
  const hobby = hobbyFixtures.find((item) => item.id === Number(hobbyId));
  const [interested, setInterested] = useState(false);
  const [activeGuide, setActiveGuide] = useState('hobby-overview');
  const [showGuideBar, setShowGuideBar] = useState(false);

  useEffect(() => {
    const updateGuideBar = () => {
      const guide = document.getElementById('hobby-guide');
      if (!guide) return;
      const guideRect = guide.getBoundingClientRect();
      setShowGuideBar(guideRect.top <= 68 && guideRect.bottom > 68);
    };

    updateGuideBar();
    window.addEventListener('scroll', updateGuideBar, { passive: true });
    window.addEventListener('resize', updateGuideBar);
    return () => {
      window.removeEventListener('scroll', updateGuideBar);
      window.removeEventListener('resize', updateGuideBar);
    };
  }, [hobbyId]);

  if (!hobby) return <div className="site-shell"><AppHeader authenticated /><main className="page-container"><section className="empty-state"><h1>취미를 찾을 수 없어요</h1><Button to="/hobbies">목록으로</Button></section></main></div>;

  return (
    <div className="site-shell hobby-detail-shell"><AppHeader authenticated notificationCount={3} />
      <main className="page-container hobby-detail">
        <section className="hobby-detail__above-fold">
          <p className="hobby-detail__breadcrumb">취미 찾기 › {hobby.category} › {hobby.name}</p>
          <section className="hobby-detail__hero">
            <div className="hobby-detail__media"><Image aria-hidden="true" /><span>이미지 영역</span><small>{hobby.name} 대표 이미지 · 콘텐츠 사진</small></div>
            <div className="hobby-detail__summary"><h1>{hobby.name}</h1><div className="hobby-detail__tags"><span className="badge">#{hobby.place}</span>{hobby.tags.map((tag) => <span className="badge" key={tag}>#{tag}</span>)}</div><p>{hobby.description}이에요.<br />장비를 빌려 처음 방문한 날부터 바로 시작할 수 있어요.</p>
              <dl><div><dt>소요 시간</dt><dd>{hobby.duration}</dd></div><div><dt>초기 비용</dt><dd>5~10만 원</dd></div><div><dt>난이도</dt><dd>{hobby.difficulty}</dd></div></dl>
              <small className="hobby-detail__notice">※ 지역과 장비 대여 여부에 따라 비용이 달라질 수 있어요.</small>
              <div className="hobby-detail__actions"><Button variant="secondary" onClick={() => setInterested((value) => !value)}><Heart aria-hidden="true" />{interested ? '관심 저장됨' : '관심 저장'}</Button><Button to="/my-hobbies">이 취미 시작하기</Button></div>
              <p className="hobby-detail__action-note">시작하면 내 상황에 맞는 입문 계획을 만들 수 있어요.</p>
            </div>
          </section>
          <a className="hobby-detail__scroll-cue" href="#hobby-guide"><span>스크롤로 자세한 정보 보기</span><ChevronDown aria-hidden="true" /></a>
        </section>

        <section className="hobby-guide" id="hobby-guide">
          <div className={`hobby-guide__summary${showGuideBar ? ' is-visible' : ' is-hidden'}`} aria-hidden={!showGuideBar}>
            <div className="hobby-guide__summary-media"><Image aria-hidden="true" /><span>사진</span></div>
            <div><strong>{hobby.name}</strong><p>{hobby.duration} · 초기 5~10만 원 · 난이도 {hobby.difficulty}</p></div>
            <div className="hobby-guide__summary-actions"><Button to="/my-hobbies">이 취미 시작하기</Button><button type="button" aria-label="관심 저장" onClick={() => setInterested((value) => !value)}><Heart fill={interested ? 'currentColor' : 'none'} aria-hidden="true" /></button></div>
          </div>

          <header className="hobby-guide__heading"><h2>{hobby.name} 상세 가이드</h2><p>필요한 정보만 선택해서 확인하세요.</p></header>
          <div className="hobby-guide__layout">
            <aside className="hobby-guide__nav" aria-label="상세 가이드 목차">
              <b>목차</b>
              <button className={activeGuide === 'hobby-overview' ? 'active' : ''} type="button" onClick={() => setActiveGuide('hobby-overview')}>취미 알아보기</button>
              <button className={activeGuide === 'hobby-preparation' ? 'active' : ''} type="button" onClick={() => setActiveGuide('hobby-preparation')}>시작 전 준비</button>
              <button className={activeGuide === 'hobby-plan' ? 'active' : ''} type="button" onClick={() => setActiveGuide('hobby-plan')}>활동 계획</button>
              <button className={activeGuide === 'hobby-content' ? 'active' : ''} type="button" onClick={() => setActiveGuide('hobby-content')}>콘텐츠·장소</button>
              <button type="button" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>↑ 핵심 정보로 돌아가기</button>
            </aside>

            <div className="hobby-guide__content">
              {activeGuide === 'hobby-overview' && <article className="hobby-guide-section" id="hobby-overview">
                <header><h3>취미 알아보기</h3><p>어떤 활동인지, 나에게 잘 맞는지 먼저 확인합니다.</p></header>
                <div className="hobby-guide__intro"><h4>소개</h4><p>홀드를 잡고 정해진 문제를 완등하는 실내 운동입니다.<br />근력과 균형 감각을 함께 사용하며 혼자서도 가볍게 시작할 수 있어요.</p></div>
                <div className="hobby-guide__columns">
                  <section><h4>활동 환경</h4><dl className="hobby-guide__facts"><div><dt>장소</dt><dd>실내 클라이밍 암장</dd></div><div><dt>활동 방식</dt><dd>혼자 또는 함께</dd></div><div><dt>이용 시간</dt><dd>암장 운영 시간 내 자유롭게</dd></div></dl></section>
                  <section><h4>이런 사람에게 추천</h4><ul className="hobby-guide__check-list"><li><Check />짧은 시간에도 성취감을 느끼고 싶은 사람</li><li><Check />혼자 시작할 수 있는 운동을 찾는 사람</li><li><Check />근력과 집중력을 함께 기르고 싶은 사람</li></ul></section>
                </div>
              </article>}

              {activeGuide === 'hobby-preparation' && <article className="hobby-guide-section" id="hobby-preparation">
                <header><h3>시작 전 준비</h3><p>첫 방문 전에 필요한 준비물과 비용, 안전 정보를 확인합니다.</p></header>
                <section className="hobby-guide__preparation"><h4>준비물</h4><dl className="hobby-guide__facts"><div><dt>필수</dt><dd>암벽화(대여 가능) · 편한 운동복</dd></div><div><dt>선택</dt><dd>초크 · 물병 · 손가락 테이프</dd></div></dl></section>
                <div className="hobby-guide__columns">
                  <section><h4>예상 비용</h4><dl className="hobby-guide__facts"><div><dt>첫 방문</dt><dd>약 3~5만 원</dd></div><div><dt>월 예상</dt><dd>약 10~15만 원</dd></div></dl><Button variant="secondary">비용 계산하기</Button></section>
                  <section><h4>첫 활동과 유의 사항</h4><ol><li>장비 대여와 안전 수칙 확인</li><li>가장 쉬운 루트 3개 체험</li></ol><small>※ 시작 전 스트레칭과 착지 매트 확인</small><Button>첫 활동 계획에 추가</Button></section>
                </div>
              </article>}

              {activeGuide === 'hobby-plan' && <article className="hobby-guide-section" id="hobby-plan">
                <header><h3>활동 계획</h3><p>입문 과정을 미리 확인한 뒤 내 계획으로 시작합니다.</p></header>
                <div className="hobby-guide__plan-head"><div><h4>입문 8주 계획</h4><p>주 2회 · 총 16개 활동 · 예상 소요 8주</p></div><div className="hobby-guide__progress"><span /></div><Button>계획 시작</Button></div>
                <div className="hobby-guide__week-list"><div><span>1주차</span><strong>암장 이용과 장비 빌려 보기</strong><small>활동 2개</small><button type="button">보기 ›</button></div><div><span>2주차</span><strong>기본 동작과 안전한 착지</strong><small>활동 3개</small><button type="button">보기 ›</button></div><div><span>3주차</span><strong>쉬운 루트 완등하기</strong><small>활동 2개</small><button type="button">보기 ›</button></div><div><span>4~8주차</span><strong>근력과 기술을 단계적으로 확장</strong><small>활동 9개</small><button type="button">보기 ›</button></div></div>
                <Button variant="secondary">전체 계획 미리보기</Button>
              </article>}

              {activeGuide === 'hobby-content' && <article className="hobby-guide-section" id="hobby-content">
                <header><h3>콘텐츠·장소</h3><p>더 알아보고 싶을 때만 숏폼과 주변 정보를 탐색합니다.</p></header>
                <h4>관련 숏폼</h4>
                <div className="hobby-guide__shorts"><article><div><Image /><span>이미지 영역</span></div><b>첫 방문 준비</b></article><article><div><Image /><span>이미지 영역</span></div><b>초보자 동작</b></article><article><div><Image /><span>이미지 영역</span></div><b>암벽화 고르기</b></article><Button variant="secondary">전체 숏폼</Button></div>
                <div className="hobby-guide__columns hobby-guide__columns--footer"><section><h4>주변에서 시작하기</h4><p>설정한 지역을 기준으로 장소와 입문 클래스를 확인합니다.</p><div><Button to="/places">주변 장소</Button><Button variant="secondary">입문 클래스</Button></div></section><section><h4>관련 자료</h4><ul className="hobby-guide__materials"><li><span>행사</span>2026 생활체육 클라이밍 페스티벌</li><li><span>입문</span>볼더링 용어 20</li></ul></section></div>
              </article>}
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
