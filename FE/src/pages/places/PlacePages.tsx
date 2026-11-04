import { ExternalLink, Image, MapPin, Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { placeFixtures } from '../../features/place/model/placeFixtures';
import type { FeedScope, FeedType } from '../../features/feed/api/feedApi';
import { feedTypeLabels } from '../../features/feed/model/feedAdapter';
import { useFeedList, useScopeHobbies } from '../../features/feed/model/useFeedList';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';

const mapMarkerPositions = [
  { markerLeft: 25, markerTop: 36, popupLeft: 31, popupTop: 24 },
  { markerLeft: 58, markerTop: 67, popupLeft: 62, popupTop: 55 },
  { markerLeft: 78, markerTop: 32, popupLeft: 55, popupTop: 20 },
] as const;

export function PlaceSearchPage() {
  const [hobby, setHobby] = useState('클라이밍');
  const [district, setDistrict] = useState('마포구');
  const places = useMemo(() => placeFixtures.filter((item) => item.hobby === hobby), [hobby]);
  const [selected, setSelected] = useState(places[0]?.id ?? 0);
  const selectedIndex = Math.max(places.findIndex((place) => place.id === selected), 0);
  const selectedPlace = places[selectedIndex];
  const selectedPosition = mapMarkerPositions[selectedIndex % mapMarkerPositions.length];

  return <div className="site-shell"><AppHeader notificationCount={3} /><main className="page-container places-page"><div className="page-title-row"><div><h1>활동 장소 찾기</h1></div><Button variant="secondary" to="/info-feed">취미 정보 피드</Button></div>
    <div className="place-controls"><label>시·도<select><option>서울특별시</option></select></label><label>시·군·구<select value={district} onChange={(event) => setDistrict(event.target.value)}><option>마포구</option><option>성동구</option><option>강남구</option></select></label><div className="place-hobbies">{['클라이밍', '요가', '도예'].map((item) => <button className={hobby === item ? 'active' : ''} type="button" onClick={() => { setHobby(item); setSelected(placeFixtures.find((place) => place.hobby === item)?.id ?? 0); }} key={item}>{item}</button>)}</div></div>
    <div className="place-layout"><section className="place-results"><h2>장소 {places.length}곳 · {hobby}</h2>{places.length ? places.map((place) => <article id={`place-${place.id}`} className={selected === place.id ? 'selected' : ''} key={place.id} onClick={() => setSelected(place.id)}><div className="place-thumb"><Image /><span>사진</span></div><div><h3>{place.name}<span>{place.indoor ? '실내 활동' : '실외 활동'}</span></h3><p>{place.address}</p><small>{place.hours}</small><b>{place.price}</b></div><a href={`https://map.kakao.com/?q=${encodeURIComponent(place.name)}`} target="_blank" rel="noreferrer">홈페이지 <ExternalLink /></a></article>) : <div className="mini-empty"><Search /><b>조건에 맞는 장소가 없어요</b></div>}<Button variant="secondary">더 보기</Button></section>
      <section className="map-placeholder" aria-label={`${district} 지도`}>
        <span>Kakao 지도 SDK · 마커 {places.length}개</span>
        {places.map((place, index) => {
          const position = mapMarkerPositions[index % mapMarkerPositions.length];
          return <button aria-label={`${place.name} 지도 위치`} className={selected === place.id ? 'active' : ''} style={{ left: `${position.markerLeft}%`, top: `${position.markerTop}%` }} onClick={() => setSelected(place.id)} type="button" key={place.id}><MapPin /><i>{index + 1}</i></button>;
        })}
        {selectedPlace && <div className="map-popup" style={{ left: `${selectedPosition.popupLeft}%`, top: `${selectedPosition.popupTop}%` }}><b>{selectedPlace.name}</b><small>{selectedPlace.hours}</small><a href={`#place-${selectedPlace.id}`}>목록에서 보기</a></div>}
      </section></div>
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
  const [hobbyId, setHobbyId] = useState<number | undefined>();
  const [hideEnded, setHideEnded] = useState(true);
  const scopeHobbies = useScopeHobbies(scope);
  // 내 취미가 없으면 맞춤 피드 대신 전체 피드를 보여 주고 안내한다
  const fallbackToAll = scope === 'MY' && scopeHobbies.hasNoMyHobbies;
  const feeds = useFeedList({ scope: fallbackToAll ? 'ALL' : scope, hobbyId, type, excludeEnded: hideEnded });
  const changeScope = (next: FeedScope) => { setScope(next); setHobbyId(undefined); };

  return <div className="site-shell"><AppHeader notificationCount={3} /><main className="page-container feed-page"><div className="page-title-row"><div><h1>취미 정보 피드</h1><p>내 취미의 축제, 강좌, 공연과 입문 자료를 모았어요.</p></div><Button variant="secondary" to="/places">장소 찾기</Button></div>
    <div className="feed-tabs">{feedTypeTabs.map((item) => <button className={type === item.value ? 'active' : ''} type="button" onClick={() => setType(item.value)} key={item.label}>{item.label}</button>)}<label><input checked={hideEnded} onChange={(event) => setHideEnded(event.target.checked)} type="checkbox" /> 종료된 행사 숨기기</label></div>
    <div className="feed-scope" role="group" aria-label="피드 기준">{feedScopes.map((item) => <button aria-pressed={scope === item.value} className={scope === item.value ? 'active' : ''} type="button" onClick={() => changeScope(item.value)} key={item.value}>{item.label}</button>)}</div>
    {fallbackToAll && <p className="feed-notice">내 취미를 선택하면 맞춤 소식을 볼 수 있어요. 지금은 전체 취미 소식을 보여 드려요. <Link to="/mypage/interests">내 취미 선택</Link></p>}
    {scopeHobbies.hobbies.length > 0 && <div className="place-hobbies feed-hobbies">
      <button className={hobbyId === undefined ? 'active' : ''} type="button" onClick={() => setHobbyId(undefined)}>전체 취미</button>
      {scopeHobbies.hobbies.map((item) => <button className={hobbyId === item.hobbyId ? 'active' : ''} type="button" onClick={() => setHobbyId(item.hobbyId)} key={item.hobbyId}>{item.name}</button>)}
    </div>}
    {feeds.isPending && <section className="empty-state feed-empty"><p>정보 피드를 불러오고 있어요.</p></section>}
    {feeds.isError && <section className="empty-state feed-empty"><h2>정보 피드를 불러오지 못했어요</h2><Button onClick={() => feeds.refetch()}>다시 시도</Button></section>}
    {feeds.isSuccess && <section className="feed-list">{feeds.items.length ? feeds.items.map((item) => <article key={item.id}>
      <div className="feed-thumb">{item.thumbnailUrl ? <img src={item.thumbnailUrl} alt="" loading="lazy" /> : <><Image /><span>{item.typeLabel}</span></>}</div>
      <div className="feed-content"><div><span className="badge">{item.typeLabel}</span><span className="badge badge--soft">{item.hobbyName}</span>{item.ended && <span className="badge badge--paused">종료</span>}</div><h2>{item.title}</h2>{item.summary && <p>{item.summary}</p>}<small>{item.meta}</small></div>
      <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer" aria-label={`${item.title} 자세히 보기 (새 탭)`}>자세히 보기 <ExternalLink /></a>
    </article>) : <section className="empty-state feed-empty"><img src={characterAssets.searching} alt="정보를 찾는 냠냠취미 캐릭터" /><h2>선택한 조건의 정보가 없어요</h2><p>다른 취미나 정보 유형을 선택해 보세요.</p></section>}</section>}
    {feeds.hasNextPage && <div className="feed-more"><Button variant="secondary" disabled={feeds.isFetchingNextPage} onClick={() => feeds.fetchNextPage()}>{feeds.isFetchingNextPage ? '불러오는 중' : '더 보기'}</Button></div>}
  </main></div>;
}

/** 피드 상세는 출처 사이트(새 탭)로 보낸다. 예전 내부 상세 주소로 들어오면 목록으로 돌려보낸다. */
export function InfoFeedDetailPage() {
  return <Navigate replace to="/info-feed" />;
}
