import { CalendarDays, ExternalLink, Image, MapPin, Search } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, Navigate } from 'react-router-dom';
import { getPlaceHobbies, getPlaces, getRegions, getSidos, placeQueryKeys } from '../../features/place/api/placeApi';
import { KakaoPlaceMap } from '../../features/place/ui/KakaoPlaceMap';
import type { FeedScope, FeedType } from '../../features/feed/api/feedApi';
import { feedTypeLabels, OTHER_HOBBY_LABEL } from '../../features/feed/model/feedAdapter';
import { dateRangeLabel, regionLabel, type DateRange } from '../../features/feed/model/feedFilters';
import { useFeedList, useFeedRegions, useScopeHobbies } from '../../features/feed/model/useFeedList';
import { DateFilterModal, RegionFilterModal } from '../../features/feed/ui/FeedFilterModals';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';

export function PlaceSearchPage() {
  const [sido, setSido] = useState('');
  const [regionId, setRegionId] = useState<number>();
  const [hobbyId, setHobbyId] = useState<number>();
  const [selectedPlaceId, setSelectedPlaceId] = useState<number>();
  const sidosQuery = useQuery({ queryKey: placeQueryKeys.regions(), queryFn: getSidos });
  const availableSido = sidosQuery.data?.find((item) => item.enabled)?.sido ?? '';
  const regionsQuery = useQuery({
    queryKey: placeQueryKeys.regions(sido),
    queryFn: () => getRegions(sido),
    enabled: Boolean(sido),
  });
  const hobbiesQuery = useQuery({ queryKey: placeQueryKeys.hobbies, queryFn: getPlaceHobbies });
  const placesQuery = useQuery({
    queryKey: placeQueryKeys.list(regionId, hobbyId),
    queryFn: () => getPlaces(regionId, hobbyId),
    enabled: regionId !== undefined && hobbyId !== undefined,
  });
  const hobbies = hobbiesQuery.data?.content ?? [];
  const places = placesQuery.data ?? [];
  const selectedPlace = places.find((place) => place.id === selectedPlaceId);
  const selectedRegion = regionsQuery.data?.find((region) => region.id === regionId);

  useEffect(() => {
    if (!sido && availableSido) setSido(availableSido);
  }, [availableSido, sido]);

  useEffect(() => {
    if (!regionsQuery.data?.length) return;
    if (regionId !== undefined && regionsQuery.data.some((region) => region.id === regionId)) return;
    const defaultRegion = regionsQuery.data.find((region) => region.sigungu === '마포구') ?? regionsQuery.data[0];
    setRegionId(defaultRegion.id);
  }, [regionId, regionsQuery.data]);

  useEffect(() => {
    if (!hobbies.length) return;
    if (hobbyId !== undefined && hobbies.some((hobby) => hobby.id === hobbyId)) return;
    const defaultHobby = hobbies.find((hobby) => hobby.name === '클라이밍') ?? hobbies[0];
    setHobbyId(defaultHobby.id);
  }, [hobbyId, hobbies]);

  useEffect(() => {
    if (!places.length) {
      setSelectedPlaceId(undefined);
      return;
    }
    if (selectedPlaceId !== undefined && places.some((place) => place.id === selectedPlaceId)) return;
    setSelectedPlaceId(places[0].id);
  }, [places, selectedPlaceId]);

  const selectedHobby = hobbies.find((hobby) => hobby.id === hobbyId);

  return <div className="site-shell"><AppHeader notificationCount={3} /><main className="page-container places-page"><div className="page-title-row"><div><h1>활동 장소 찾기</h1></div><Button variant="secondary" to="/info-feed">취미 정보 피드</Button></div>
    <div className="place-controls">
      <label>시·도<select value={sido} onChange={(event) => { setSido(event.target.value); setRegionId(undefined); }} disabled={!sidosQuery.data?.length}>
        {sidosQuery.data?.map((item) => <option disabled={!item.enabled} value={item.sido} key={item.sido}>{item.sido}{item.enabled ? '' : ' (준비 중)'}</option>)}
      </select></label>
      <label>시·군·구<select value={regionId ?? ''} onChange={(event) => setRegionId(Number(event.target.value))} disabled={!regionsQuery.data?.length}>
        {regionsQuery.data?.map((region) => <option value={region.id} key={region.id}>{region.sigungu}</option>)}
      </select></label>
      <div className="place-hobbies">{hobbies.map((hobby) => <button className={hobbyId === hobby.id ? 'active' : ''} type="button" onClick={() => setHobbyId(hobby.id)} key={hobby.id}>{hobby.name}</button>)}</div>
    </div>
    {sidosQuery.isError || regionsQuery.isError || hobbiesQuery.isError ? <p className="place-query-message" role="alert">장소 검색 조건을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p> : null}
    <div className="place-layout"><section className="place-results"><h2>장소 {placesQuery.isLoading ? '불러오는 중' : `${places.length}곳`} · {selectedHobby?.name ?? ''}</h2>
      {placesQuery.isError ? <div className="mini-empty"><Search /><b>장소를 불러오지 못했어요</b></div> : null}
      {placesQuery.isLoading ? <div className="mini-empty" role="status">장소를 불러오고 있어요.</div> : null}
      {!placesQuery.isLoading && !placesQuery.isError && places.length === 0 ? <div className="mini-empty"><Search /><b>조건에 맞는 장소가 없어요</b></div> : null}
      {places.map((place) => <article id={`place-${place.id}`} className={selectedPlaceId === place.id ? 'selected' : ''} key={place.id} onClick={() => setSelectedPlaceId(place.id)}>
        <div className="place-thumb"><Image /><span>{place.source}</span></div><div><h3>{place.name}<span>{place.category}</span></h3><p>{place.address}</p><small>{place.openingHours || '운영 시간 정보 없음'}</small><b>{place.feeText || '비용 정보 없음'}</b>{place.needsInfoCheck && <small className="place-info-check">정보 확인 필요</small>}</div>
        {place.externalUrl && <a href={place.externalUrl} target="_blank" rel="noreferrer" onClick={(event) => event.stopPropagation()}>지도에서 보기 <ExternalLink /></a>}
      </article>)}
    </section>
    <KakaoPlaceMap places={places} selectedPlace={selectedPlace} regionName={selectedRegion?.sigungu ?? sido} onSelect={setSelectedPlaceId} />
    </div>
  </main></div>;
}

const feedTypeTabs: Array<{ label: string; value?: FeedType }> = [
  { label: '전체' },
  { label: feedTypeLabels.EVENT, value: 'EVENT' },
  { label: feedTypeLabels.CLASS, value: 'CLASS' },
  { label: feedTypeLabels.SHOW, value: 'SHOW' },
  { label: feedTypeLabels.GUIDE, value: 'GUIDE' },
];
const feedScopes: Array<{ label: string; value: FeedScope }> = [
  { label: '내 취미', value: 'MY' },
  { label: '관심 취미', value: 'SAVED' },
  { label: '전체', value: 'ALL' },
];

export function InfoFeedPage() {
  const [type, setType] = useState<FeedType | undefined>();
  const [scope, setScope] = useState<FeedScope>('MY');
  // 숫자 = 그 취미, 'OTHER' = 30종 어디에도 맞지 않는 '그 외', undefined = 전체 취미
  const [hobbyId, setHobbyId] = useState<number | 'OTHER' | undefined>();
  const [hideEnded, setHideEnded] = useState(true);
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [dateRange, setDateRange] = useState<DateRange>({});
  const [regionIds, setRegionIds] = useState<number[]>([]);
  const [openFilter, setOpenFilter] = useState<'date' | 'region' | null>(null);
  const scopeHobbies = useScopeHobbies(scope);
  const regionsQuery = useFeedRegions(openFilter === 'region' || regionIds.length > 0);
  const regionOptions = (regionsQuery.data ?? []).map((region) => ({ id: region.id, name: region.sigungu }));
  // 내 취미가 없으면 맞춤 피드 대신 전체 피드를 보여 주고 안내한다
  const fallbackToAll = scope === 'MY' && scopeHobbies.hasNoMyHobbies;
  // '그 외'는 내 취미·관심 취미에 속할 수 없어 전체 범위에서만 칩으로 보여 준다
  const showOther = scope === 'ALL' || fallbackToAll;
  const feeds = useFeedList({
    scope: fallbackToAll ? 'ALL' : scope, type, excludeEnded: hideEnded,
    hobbyId: typeof hobbyId === 'number' ? hobbyId : undefined, uncategorized: hobbyId === 'OTHER',
    keyword: keyword || undefined, from: dateRange.from, to: dateRange.to, regionIds,
  });
  const changeScope = (next: FeedScope) => { setScope(next); setHobbyId(undefined); };
  const submitSearch = () => { const value = keywordInput.trim(); if (!value || value.length >= 2) setKeyword(value); };
  const dateLabel = dateRangeLabel(dateRange);
  const regionNames = regionIds.flatMap((id) => regionOptions.filter((option) => option.id === id).map((option) => option.name));
  const regionText = regionLabel(regionNames) ?? (regionIds.length ? `${regionIds.length}곳` : null);
  const filtered = Boolean(keyword || dateLabel || regionIds.length);
  const clearFilters = () => { setKeywordInput(''); setKeyword(''); setDateRange({}); setRegionIds([]); };

  return <div className="site-shell"><AppHeader notificationCount={3} /><main className="page-container feed-page"><div className="page-title-row"><div><h1>취미 정보 피드</h1><p>내 취미의 축제, 강좌, 공연과 입문 자료를 모았어요.</p></div><Button variant="secondary" to="/places">장소 찾기</Button></div>
    <div className="feed-tabs">
      {feedTypeTabs.map((item) => <button className={type === item.value ? 'active' : ''} type="button" onClick={() => setType(item.value)} key={item.label}>{item.label}</button>)}
      <form className="search-bar feed-search" role="search" onSubmit={(event) => { event.preventDefault(); submitSearch(); }}><Search aria-hidden="true" /><input value={keywordInput} onChange={(event) => setKeywordInput(event.target.value)} placeholder="제목이나 장소로 검색 (2자 이상)" aria-label="정보 피드 검색" /><button type="submit">검색</button></form>
      <label><input checked={hideEnded} onChange={(event) => setHideEnded(event.target.checked)} type="checkbox" /> 마감·종료된 행사 숨기기</label>
    </div>
    {keywordInput.trim().length === 1 && <p className="form-message form-message--error">검색어는 2자 이상 입력해 주세요.</p>}
    <div className="feed-toolbar">
      <div className="feed-scope" role="group" aria-label="피드 기준">{feedScopes.map((item) => <button aria-pressed={scope === item.value} className={scope === item.value ? 'active' : ''} type="button" onClick={() => changeScope(item.value)} key={item.value}>{item.label}</button>)}</div>
      <div className="feed-filter-buttons">
        <button className={dateLabel ? 'feed-filter-button active' : 'feed-filter-button'} type="button" aria-haspopup="dialog" onClick={() => setOpenFilter('date')}><CalendarDays aria-hidden="true" />{dateLabel ?? '날짜 설정'}</button>
        <button className={regionText ? 'feed-filter-button active' : 'feed-filter-button'} type="button" aria-haspopup="dialog" onClick={() => setOpenFilter('region')}><MapPin aria-hidden="true" />{regionText ?? '지역 설정'}</button>
      </div>
    </div>
    {fallbackToAll && <p className="feed-notice">내 취미를 선택하면 맞춤 소식을 볼 수 있어요. 지금은 전체 취미 소식을 보여 드려요. <Link to="/mypage/interests">내 취미 선택</Link></p>}
    {(scopeHobbies.hobbies.length > 0 || showOther) && <div className="place-hobbies feed-hobbies">
      <button className={hobbyId === undefined ? 'active' : ''} type="button" onClick={() => setHobbyId(undefined)}>전체 취미</button>
      {scopeHobbies.hobbies.map((item) => <button className={hobbyId === item.hobbyId ? 'active' : ''} type="button" onClick={() => setHobbyId(item.hobbyId)} key={item.hobbyId}>{item.name}</button>)}
      {showOther && <button className={hobbyId === 'OTHER' ? 'active' : ''} type="button" onClick={() => setHobbyId('OTHER')}>{OTHER_HOBBY_LABEL}</button>}
    </div>}
    {feeds.isPending && <section className="empty-state feed-empty"><p>정보 피드를 불러오고 있어요.</p></section>}
    {feeds.isError && <section className="empty-state feed-empty"><h2>정보 피드를 불러오지 못했어요</h2><Button onClick={() => feeds.refetch()}>다시 시도</Button></section>}
    {feeds.isSuccess && <section className="feed-list">{feeds.items.length ? feeds.items.map((item) => <article className={item.ended ? 'feed-card--ended' : item.closed ? 'feed-card--closed' : undefined} aria-disabled={item.ended || undefined} key={item.id}>
      <FeedThumbnail url={item.thumbnailUrl} />
      <div className="feed-content"><div>{item.ended && <span className="badge badge--ended">종료된 행사</span>}{item.closed && <span className="badge badge--ended">접수 마감</span>}<span className="badge">{item.typeLabel}</span><span className="badge badge--soft">{item.hobbyName}</span></div><h2>{item.title}</h2>{item.summary && <p>{item.summary}</p>}<small>{item.meta}</small></div>
      {item.ended
        ? <span className="feed-link feed-link--disabled">종료됨</span>
        : <a className="feed-link" href={item.sourceUrl} target="_blank" rel="noopener noreferrer" aria-label={`${item.title} 자세히 보기 (새 탭)`}>자세히 보기 <ExternalLink /></a>}
    </article>) : <section className="empty-state feed-empty"><img src={characterAssets.searching} alt="정보를 찾는 냠냠취미 캐릭터" /><h2>선택한 조건의 정보가 없어요</h2><p>{filtered ? '검색어나 날짜·지역 조건을 바꿔 보세요.' : '다른 취미나 정보 유형을 선택해 보세요.'}</p>{filtered && <Button variant="secondary" onClick={clearFilters}>검색·날짜·지역 조건 지우기</Button>}</section>}</section>}
    {feeds.hasNextPage && <div className="feed-more"><Button variant="secondary" disabled={feeds.isFetchingNextPage} onClick={() => feeds.fetchNextPage()}>{feeds.isFetchingNextPage ? '불러오는 중' : '더 보기'}</Button></div>}
    {openFilter === 'date' && <DateFilterModal value={dateRange} onClose={() => setOpenFilter(null)} onApply={(range) => { setDateRange(range); setOpenFilter(null); }} />}
    {openFilter === 'region' && <RegionFilterModal options={regionOptions} value={regionIds} loading={regionsQuery.isLoading} error={regionsQuery.isError} onClose={() => setOpenFilter(null)} onApply={(ids) => { setRegionIds(ids); setOpenFilter(null); }} />}
  </main></div>;
}

/** 썸네일이 없거나 출처 이미지가 깨지면(404 등) 냠냠취미 캐릭터 이미지로 대신한다 */
function FeedThumbnail({ url }: { url: string | null }) {
  const [failed, setFailed] = useState(false);
  const fallback = !url || failed;
  return <div className={fallback ? 'feed-thumb feed-thumb--fallback' : 'feed-thumb'}>
    <img src={fallback ? characterAssets.searching : url} alt="" loading="lazy" onError={fallback ? undefined : () => setFailed(true)} />
  </div>;
}

/** 피드 상세는 출처 사이트(새 탭)로 보낸다. 예전 내부 상세 주소로 들어오면 목록으로 돌려보낸다. */
export function InfoFeedDetailPage() {
  return <Navigate replace to="/info-feed" />;
}
