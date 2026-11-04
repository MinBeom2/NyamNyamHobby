import { ExternalLink, Image, Search } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, Navigate, useParams } from 'react-router-dom';
import { getPlaceHobbies, getPlaces, getRegions, getSidos, placeQueryKeys } from '../../features/place/api/placeApi';
import { KakaoPlaceMap } from '../../features/place/ui/KakaoPlaceMap';
import { infoFeedFixtures } from '../../features/place/model/placeFixtures';
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

export function InfoFeedPage() {
  const [type, setType] = useState('전체');
  const [hobby, setHobby] = useState('전체 취미');
  const [hideEnded, setHideEnded] = useState(true);
  const items = infoFeedFixtures.filter((item) => (type === '전체' || item.type === type) && (hobby === '전체 취미' || item.hobby === hobby) && (!hideEnded || !item.ended));

  return <div className="site-shell"><AppHeader notificationCount={3} /><main className="page-container feed-page"><div className="page-title-row"><div><h1>취미 정보 피드</h1><p>관심·진행 중인 취미의 축제, 강좌, 공연과 입문 자료를 모았어요.</p></div><Button variant="secondary" to="/places">장소 찾기</Button></div>
    <div className="feed-tabs">{['전체', '축제·행사', '강좌·클래스', '공연·전시', '입문 자료'].map((item) => <button className={type === item ? 'active' : ''} type="button" onClick={() => setType(item)} key={item}>{item}</button>)}<label><input checked={hideEnded} onChange={(event) => setHideEnded(event.target.checked)} type="checkbox" /> 종료된 행사 숨기기</label></div>
    <div className="place-hobbies feed-hobbies">{['전체 취미', '클라이밍', '요가', '도예'].map((item) => <button className={hobby === item ? 'active' : ''} type="button" onClick={() => setHobby(item)} key={item}>{item}</button>)}</div>
    <section className="feed-list">{items.length ? items.map((item) => <article key={item.id}><div className="feed-thumb"><Image /><span>{item.type}</span></div><div className="feed-content"><div><span className="badge">{item.type}</span><span className="badge badge--soft">{item.hobby}</span>{item.ended && <span className="badge badge--paused">종료</span>}</div><h2>{item.title}</h2><p>{item.summary}</p><small>{item.source} · {item.location} · {item.period}</small></div><Link to={`/info-feed/${item.id}`}>자세히 보기 <ExternalLink /></Link></article>) : <section className="empty-state feed-empty"><img src={characterAssets.searching} alt="정보를 찾는 냠냠취미 캐릭터" /><h2>선택한 조건의 정보가 없어요</h2><p>다른 취미나 정보 유형을 선택해 보세요.</p></section>}</section>
  </main></div>;
}

export function InfoFeedDetailPage() {
  const { feedId } = useParams();
  const item = infoFeedFixtures.find((feed) => feed.id === Number(feedId));

  if (!item) return <Navigate replace to="/info-feed" />;

  return <div className="site-shell"><AppHeader notificationCount={3} /><main className="page-container feed-detail-page">
    <article className="feed-detail-card">
      <div className="feed-detail-card__badges"><span className="badge">{item.type}</span><span className="badge badge--soft">{item.hobby}</span>{item.ended && <span className="badge badge--paused">종료</span>}</div>
      <h1>{item.title}</h1>
      <p className="feed-detail-card__meta">{item.source} · {item.location} · {item.period}</p>
      <div className="feed-detail-card__image"><Image aria-hidden="true" /><span>{item.type} 이미지</span></div>
      <p className="feed-detail-card__summary">{item.summary}</p>
      <Link className="button button--secondary" to="/info-feed">목록으로</Link>
    </article>
  </main></div>;
}
