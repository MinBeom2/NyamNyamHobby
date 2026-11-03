import { Check, ChevronDown, ChevronUp, Clock3, Coins, Gauge, Heart, Image, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getHobbies, getHobbyDetail, getHobbyPlanPreview, getHobbyShorts, hobbyQueryKeys, startHobby, type HobbyDifficulty } from '../../features/hobby/api/hobbyApi';
import { toHobbyCardModel, toHobbyDetailModel, type HobbyCardModel } from '../../features/hobby/model/hobbyAdapter';
import { useAuthStore } from '../../features/auth/model/authStore';
import { deleteUserHobby, getUserHobbies, saveUserHobby, userQueryKeys } from '../../features/user/api/userApi';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';
import { scrollToElement, scrollToTop } from '../../shared/scroll/smoothScroll';

function HobbyCard({ hobby }: { hobby: HobbyCardModel }) {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies(), enabled: Boolean(user) });
  const saved = hobbiesQuery.data?.some((item) => item.hobbyId === hobby.id && item.status === 'SAVED') ?? false;
  const saveMutation = useMutation({ mutationFn: () => saved ? deleteUserHobby(hobby.id) : saveUserHobby(hobby.id), onSuccess: () => queryClient.invalidateQueries({ queryKey: userQueryKeys.hobbies() }) });
  return <article className="catalog-card">
    <div className="catalog-card__visual">{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <><Image aria-hidden="true" /><span>이미지</span></>}<b>{hobby.relation}</b></div>
    <div className="catalog-card__body"><span className="badge">{hobby.category}</span><h2>{hobby.name}</h2><p>{hobby.description}</p><div className="catalog-card__meta"><span><Clock3 />{hobby.duration}</span><span><Coins />{hobby.cost}</span><span><Gauge />{hobby.difficulty}</span></div><div className="catalog-card__actions"><button className={saved ? 'active' : ''} disabled={saveMutation.isPending} onClick={() => user ? saveMutation.mutate() : navigate('/login')} type="button"><Heart fill={saved ? 'currentColor' : 'none'} /> {saved ? '관심 저장됨' : '관심 저장'}</button><Link to={`/hobbies/${hobby.id}`}>상세 보기</Link></div></div>
  </article>;
}

export function HobbyInterestSelectPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<number[]>([]);
  const hobbiesQuery = useQuery({ queryKey: hobbyQueryKeys.list({ size: 100, sort: 'NAME' }), queryFn: () => getHobbies({ size: 100, sort: 'NAME' }) });
  const normalizedKeyword = keyword.trim();
  const allHobbies = useMemo(() => (hobbiesQuery.data?.content ?? []).map(toHobbyCardModel), [hobbiesQuery.data]);
  const groups = useMemo(() => {
    const grouped = new Map<string, HobbyCardModel[]>();
    allHobbies.filter((hobby) => !normalizedKeyword || `${hobby.name} ${hobby.tags.join(' ')}`.includes(normalizedKeyword)).forEach((hobby) => grouped.set(hobby.category, [...(grouped.get(hobby.category) ?? []), hobby]));
    return [...grouped.entries()].map(([title, items]) => ({ title, items }));
  }, [allHobbies, normalizedKeyword]);
  const toggle = (id: number) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : current.length >= 10 ? current : [...current, id]);
  const submit = () => { const query = new URLSearchParams(); if (selected.length) query.set('interests', selected.join(',')); navigate(`/hobbies/recommendations${query.size ? `?${query}` : ''}`); };
  return <div className="site-shell"><AppHeader /><main className="page-container direct-interest-page">
    <header className="direct-interest-heading"><h1>관심사를 직접 골라 주세요</h1><p>영상을 보지 않아도 추천을 받을 수 있어요. 최대 10개까지 고를 수 있어요.</p></header>
    <label className="direct-interest-search"><span>취미 검색</span><div><Search aria-hidden="true" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="취미명이나 태그로 찾기" /></div></label>
    {hobbiesQuery.isLoading && <section className="direct-interest-empty"><p>취미를 불러오고 있어요.</p></section>}
    {hobbiesQuery.isError && <section className="direct-interest-empty"><p>취미 목록을 불러오지 못했어요.</p></section>}
    {groups.length > 0 && <section className="direct-interest-groups" aria-label="관심사 목록">{groups.map((group) => <article className="direct-interest-group" key={group.title}><h2>{group.title}</h2><div>{group.items.map((hobby) => { const active = selected.includes(hobby.id); return <button className={active ? 'selected' : ''} type="button" key={hobby.id} aria-pressed={active} disabled={!active && selected.length >= 10} onClick={() => toggle(hobby.id)}>{active && <Check aria-hidden="true" />}{hobby.name}</button>; })}</div></article>)}</section>}
    {!hobbiesQuery.isLoading && !hobbiesQuery.isError && groups.length === 0 && <section className="direct-interest-empty"><Search aria-hidden="true" /><p>검색한 취미가 없어요.</p></section>}
    <section className="direct-interest-summary" aria-label="선택한 관심사"><div className="direct-interest-summary__selection"><strong>선택한 관심사 {selected.length}개</strong><div>{selected.map((id) => { const hobby = allHobbies.find((item) => item.id === id); return hobby ? <button type="button" key={id} onClick={() => toggle(id)}><Check aria-hidden="true" />{hobby.name}</button> : null; })}</div></div><div className="direct-interest-summary__actions"><button type="button" onClick={() => setSelected([])}>선택 초기화</button><Button onClick={submit}>추천 결과 보기</Button></div></section>
  </main></div>;
}

const difficultyValues: Record<string, HobbyDifficulty | undefined> = { 전체: undefined, 쉬움: 'EASY', 보통: 'NORMAL', 어려움: 'HARD' };
const placeValues: Record<string, 'INDOOR' | 'OUTDOOR' | 'ANY' | undefined> = { 전체: undefined, 실내: 'INDOOR', 실외: 'OUTDOOR', 상관없음: 'ANY' };

export function HobbySearchPage({ recommended = false }: { recommended?: boolean }) {
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [category, setCategory] = useState('전체');
  const [difficulty, setDifficulty] = useState('전체');
  const [place, setPlace] = useState('전체');
  const params = { keyword: submittedKeyword.length >= 2 ? submittedKeyword : undefined, difficulty: difficultyValues[difficulty], place: placeValues[place], sort: recommended ? 'RECOMMEND' as const : undefined, size: 100 };
  const hobbiesQuery = useQuery({ queryKey: hobbyQueryKeys.list(params), queryFn: () => getHobbies(params) });
  const allHobbies = useMemo(() => (hobbiesQuery.data?.content ?? []).map(toHobbyCardModel), [hobbiesQuery.data]);
  const categories = useMemo(() => ['전체', ...new Set(allHobbies.map((hobby) => hobby.category))], [allHobbies]);
  const hobbies = category === '전체' ? allHobbies : allHobbies.filter((hobby) => hobby.category === category);
  const reset = () => { setKeyword(''); setSubmittedKeyword(''); setCategory('전체'); setDifficulty('전체'); setPlace('전체'); };
  const search = () => { const value = keyword.trim(); if (!value || value.length >= 2) setSubmittedKeyword(value); };
  return <div className="site-shell"><AppHeader /><main className="page-container discovery-page">
    <div className="discovery-title"><div><h1>{recommended ? '맞춤 취미 추천' : '취미 찾기'}</h1><p>{recommended ? '관심사와 이용 상황을 바탕으로 잘 맞는 취미를 모았어요.' : '취미명이나 태그로 찾고, 시간·비용·난이도로 좁혀 보세요.'}</p></div>{!recommended && <Button to="/hobbies/interests">맞춤 추천 보기</Button>}</div>
    <form className="search-bar" onSubmit={(event) => { event.preventDefault(); search(); }}><Search aria-hidden="true" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="취미명이나 태그로 검색 (2자 이상)" /><button type="submit">검색</button></form>
    <div className="filter-row" aria-label="취미 필터"><select value={category} onChange={(event) => setCategory(event.target.value)}>{categories.map((item) => <option key={item}>{item}</option>)}</select><select value={difficulty} onChange={(event) => setDifficulty(event.target.value)}>{Object.keys(difficultyValues).map((item) => <option key={item}>{item}</option>)}</select><select value={place} onChange={(event) => setPlace(event.target.value)}>{Object.keys(placeValues).map((item) => <option key={item}>{item}</option>)}</select><button className="filter-reset" type="button" onClick={reset}>초기화</button></div>
    {keyword.trim().length === 1 && <p className="form-message form-message--error">검색어는 2자 이상 입력해 주세요.</p>}
    {hobbiesQuery.isLoading && <section className="empty-state"><p>취미를 불러오고 있어요.</p></section>}
    {hobbiesQuery.isError && <section className="empty-state"><h2>취미를 불러오지 못했어요</h2><Button onClick={() => hobbiesQuery.refetch()}>다시 시도</Button></section>}
    {!hobbiesQuery.isLoading && !hobbiesQuery.isError && (hobbies.length ? <section className="catalog-grid" aria-label="취미 검색 결과">{hobbies.map((hobby) => <HobbyCard key={hobby.id} hobby={hobby} />)}</section> : <section className="empty-state"><Search aria-hidden="true" /><h2>조건에 맞는 취미가 없어요</h2><Button onClick={reset}>모든 조건 해제</Button></section>)}
  </main></div>;
}

export function HobbyDetailPage() {
  const { hobbyId } = useParams();
  const id = Number(hobbyId);
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const detailQuery = useQuery({ queryKey: hobbyQueryKeys.detail(id), queryFn: () => getHobbyDetail(id), enabled: Number.isFinite(id) });
  const planQuery = useQuery({ queryKey: hobbyQueryKeys.planPreview(id), queryFn: () => getHobbyPlanPreview(id), enabled: Number.isFinite(id) });
  const shortsQuery = useQuery({ queryKey: hobbyQueryKeys.shorts(id, 3), queryFn: () => getHobbyShorts(id, 3), enabled: Number.isFinite(id), retry: false });
  const userHobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies(), enabled: Boolean(user) && Number.isFinite(id) });
  const hobby = detailQuery.data ? toHobbyDetailModel(detailQuery.data) : null;
  const userHobby = userHobbiesQuery.data?.find((item) => item.hobbyId === id);
  const interested = detailQuery.data?.myStatus === 'SAVED' || userHobby?.status === 'SAVED';
  const existingPlanId = userHobby?.planId;
  const hasExistingPlan = Boolean(existingPlanId && (userHobby?.status === 'IN_PROGRESS' || userHobby?.status === 'PAUSED'));
  const startLabel = hasExistingPlan ? (userHobby?.status === 'PAUSED' ? '계획 확인' : '계획 이어가기') : '이 취미 시작하기';
  const interestMutation = useMutation({ mutationFn: () => interested ? deleteUserHobby(id) : saveUserHobby(id), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: hobbyQueryKeys.detail(id) }); await queryClient.invalidateQueries({ queryKey: userQueryKeys.hobbies() }); } });
  const startMutation = useMutation({ mutationFn: () => startHobby(id), onSuccess: async (result) => { await queryClient.invalidateQueries({ queryKey: userQueryKeys.hobbies() }); navigate(`/my-hobbies/${id}/plan?planId=${result.planId}`); } });
  const [activeGuide, setActiveGuide] = useState('hobby-overview');
  const [showGuideBar, setShowGuideBar] = useState(false);
  const toggleInterest = () => user ? interestMutation.mutate() : navigate('/login');
  const beginHobby = () => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (hasExistingPlan && existingPlanId) {
      navigate(`/my-hobbies/${id}/plan?planId=${existingPlanId}`);
      return;
    }
    startMutation.mutate();
  };
  useEffect(() => { const update = () => { const guide = document.getElementById('hobby-guide'); const heading = document.getElementById('hobby-guide-heading'); if (guide && heading) { const guideRect = guide.getBoundingClientRect(); const headingRect = heading.getBoundingClientRect(); setShowGuideBar(headingRect.bottom <= 68 && guideRect.bottom > 68); } }; update(); window.addEventListener('scroll', update, { passive: true }); window.addEventListener('resize', update); return () => { window.removeEventListener('scroll', update); window.removeEventListener('resize', update); }; }, [hobbyId]);
  if (detailQuery.isLoading) return <div className="site-shell"><AppHeader /><main className="page-container"><section className="empty-state"><p>취미 정보를 불러오고 있어요.</p></section></main></div>;
  if (!hobby || detailQuery.isError) return <div className="site-shell"><AppHeader /><main className="page-container"><section className="empty-state"><h1>취미를 찾을 수 없어요</h1><Button to="/hobbies">목록으로</Button></section></main></div>;
  const plan = planQuery.data;
  return <div className="site-shell hobby-detail-shell"><AppHeader /><main className="page-container hobby-detail">
    <section className="hobby-detail__above-fold"><p className="hobby-detail__breadcrumb">취미 찾기 › {hobby.category} › {hobby.name}</p><section className="hobby-detail__hero"><div className="hobby-detail__media">{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt={`${hobby.name} 대표`} /> : <><Image aria-hidden="true" /><span>이미지 영역</span></>}<small>{hobby.name} 대표 이미지 · 콘텐츠 사진</small></div><div className="hobby-detail__summary"><h1>{hobby.name}</h1><div className="hobby-detail__tags"><span className="badge">#{hobby.place}</span>{hobby.tags.map((tag) => <span className="badge" key={tag}>#{tag}</span>)}</div><p>{hobby.longDescription}</p><dl><div><dt>소요 시간</dt><dd>{hobby.duration}</dd></div><div><dt>초기 비용</dt><dd>{hobby.initialCost}</dd></div><div><dt>난이도</dt><dd>{hobby.difficulty}</dd></div></dl><small className="hobby-detail__notice">※ 지역과 장비 대여 여부에 따라 비용이 달라질 수 있어요.</small><div className="hobby-detail__actions"><Button variant="secondary" disabled={interestMutation.isPending} onClick={toggleInterest}><Heart fill={interested ? 'currentColor' : 'none'} />{interested ? '관심 저장됨' : '관심 저장'}</Button><Button disabled={startMutation.isPending} onClick={beginHobby}>{startLabel}</Button></div></div></section><a className="hobby-detail__scroll-cue" href="#hobby-guide" onClick={(event) => { event.preventDefault(); scrollToElement(document.getElementById('hobby-guide')); }}><span>스크롤로 자세한 정보 보기</span><ChevronDown /></a></section>
    <section className="hobby-guide" id="hobby-guide"><div className={`hobby-guide__summary${showGuideBar ? ' is-visible' : ' is-hidden'}`} aria-hidden={!showGuideBar}><div className="hobby-guide__summary-media"><Image /><span>사진</span></div><div><strong>{hobby.name}</strong><p>{hobby.duration} · 초기 {hobby.initialCost} · 난이도 {hobby.difficulty}</p></div><div className="hobby-guide__summary-actions"><Button disabled={startMutation.isPending} onClick={beginHobby}>{startLabel}</Button><button type="button" onClick={toggleInterest}><Heart fill={interested ? 'currentColor' : 'none'} /></button></div></div>
      <button className="hobby-guide__back" type="button" onClick={() => scrollToTop()}><ChevronUp aria-hidden="true" /><span>핵심 정보로 돌아가기</span></button>
      <header className="hobby-guide__heading" id="hobby-guide-heading"><h2>{hobby.name} 상세 가이드</h2><p>필요한 정보만 선택해서 확인하세요.</p></header><div className="hobby-guide__layout"><aside className="hobby-guide__nav" aria-label="상세 가이드 목차"><b>목차</b>{[['hobby-overview','취미 알아보기'],['hobby-preparation','시작 전 준비'],['hobby-plan','활동 계획'],['hobby-content','콘텐츠·장소']].map(([key,label]) => <button className={activeGuide === key ? 'active' : ''} type="button" key={key} onClick={() => setActiveGuide(key)}>{label}</button>)}</aside><div className="hobby-guide__content">
        {activeGuide === 'hobby-overview' && <article className="hobby-guide-section"><header><h3>취미 알아보기</h3><p>어떤 활동인지, 나에게 잘 맞는지 먼저 확인합니다.</p></header><div className="hobby-guide__intro"><h4>소개</h4><p>{hobby.longDescription}</p></div><div className="hobby-guide__columns"><section><h4>활동 환경</h4><dl className="hobby-guide__facts"><div><dt>장소</dt><dd>{hobby.environment || hobby.place}</dd></div><div><dt>활동 방식</dt><dd>{hobby.social}</dd></div><div><dt>이용 시간</dt><dd>{hobby.duration}</dd></div></dl></section><section><h4>취미 태그</h4><ul className="hobby-guide__check-list">{hobby.tags.map((tag) => <li key={tag}><Check />{tag}</li>)}</ul></section></div></article>}
        {activeGuide === 'hobby-preparation' && <article className="hobby-guide-section"><header><h3>시작 전 준비</h3><p>첫 활동 전에 필요한 준비물과 비용, 안전 정보를 확인합니다.</p></header><section className="hobby-guide__preparation"><h4>준비물</h4><p>{hobby.supplies}</p></section><div className="hobby-guide__columns"><section><h4>예상 비용</h4><dl className="hobby-guide__facts"><div><dt>첫 방문</dt><dd>{hobby.initialCost}</dd></div><div><dt>월 예상</dt><dd>{hobby.monthlyCost}</dd></div></dl></section><section><h4>첫 활동과 유의 사항</h4><p>{hobby.firstActivity}</p><small>{hobby.cautions}</small></section></div></article>}
        {activeGuide === 'hobby-plan' && <article className="hobby-guide-section"><header><h3>활동 계획</h3><p>입문 과정을 미리 확인한 뒤 내 계획으로 시작합니다.</p></header>{planQuery.isLoading && <p>계획을 불러오고 있어요.</p>}{plan && plan.ready ? <><div className="hobby-guide__plan-head"><div><h4>{plan.title}</h4><p>총 {plan.totalWeeks}주 · 미리보기 {plan.previewWeeks}주</p></div><div className="hobby-guide__progress"><span /></div><Button disabled={startMutation.isPending} onClick={beginHobby}>{startLabel}</Button></div><div className="hobby-guide__week-list">{plan.weeks.map((week) => <div key={week.weekNo}><span>{week.weekNo}주차</span><strong>{week.title}</strong><small>활동 {week.items.length}개</small><Link to={`/hobbies/${id}/plan-preview#week-${week.weekNo}`}>보기 ›</Link></div>)}</div><Button variant="secondary" to={`/hobbies/${id}/plan-preview`}>전체 계획 미리보기</Button></> : !planQuery.isLoading && <p>아직 준비된 입문 계획이 없어요.</p>}</article>}
        {activeGuide === 'hobby-content' && <article className="hobby-guide-section"><header><h3>콘텐츠·장소</h3><p>숏폼과 주변 정보를 탐색합니다.</p></header><h4>관련 숏폼</h4><div className="hobby-guide__shorts">{(shortsQuery.data ?? []).map((short) => <article key={short.shortId}><div>{short.thumbnailUrl ? <img src={short.thumbnailUrl} alt="" /> : <><Image /><span>이미지 영역</span></>}</div><b>{short.title}</b></article>)}<Button variant="secondary" to="/explore">전체 숏폼</Button></div><div className="hobby-guide__columns hobby-guide__columns--footer"><section><h4>주변에서 시작하기</h4><div><Button to="/places">주변 장소</Button></div></section></div></article>}
      </div></div></section>
  </main></div>;
}

export function HobbyPlanPreviewPage() {
  const { hobbyId } = useParams();
  const id = Number(hobbyId);
  const detailQuery = useQuery({ queryKey: hobbyQueryKeys.detail(id), queryFn: () => getHobbyDetail(id), enabled: Number.isFinite(id) });
  const planQuery = useQuery({ queryKey: hobbyQueryKeys.planPreview(id), queryFn: () => getHobbyPlanPreview(id), enabled: Number.isFinite(id) });
  const hobby = detailQuery.data ? toHobbyDetailModel(detailQuery.data) : null;
  useEffect(() => {
    if (!planQuery.data?.ready || !window.location.hash) return;
    const targetId = decodeURIComponent(window.location.hash.slice(1));
    const frame = window.requestAnimationFrame(() => scrollToElement(document.getElementById(targetId), { immediate: true }));
    return () => window.cancelAnimationFrame(frame);
  }, [planQuery.data]);
  return <div className="site-shell"><AppHeader /><main className="page-container hobby-plan-preview"><header><p>취미 찾기 › {hobby?.name ?? '취미'} › 전체 계획</p><h1>{planQuery.data?.title ?? '입문 계획 미리보기'}</h1><p>시작하기 전에 주차별 활동을 확인하세요.</p></header>{planQuery.isLoading && <section className="empty-state"><p>계획을 불러오고 있어요.</p></section>}{planQuery.isError && <section className="empty-state"><h2>계획을 불러오지 못했어요.</h2></section>}{planQuery.data && !planQuery.data.ready && <section className="empty-state"><h2>아직 준비된 계획이 없어요.</h2></section>}{planQuery.data?.ready && <section className="hobby-plan-preview__weeks">{planQuery.data.weeks.map((week) => <article id={`week-${week.weekNo}`} key={week.weekNo}><div><span>{week.weekNo}주차</span><h2>{week.title}</h2></div><ol>{week.items.map((item) => <li key={item.seq}><strong>{item.title}</strong><p>{item.description}</p><small>예상 {item.expectedMinutes}분 · 준비물 {item.supplies} · 완료 기준 {item.doneCriteria}</small></li>)}</ol></article>)}</section>}<Button variant="secondary" to={`/hobbies/${id}`}>취미 상세로</Button></main></div>;
}
