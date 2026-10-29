import { ExternalLink, Image, MapPin, Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { infoFeedFixtures, placeFixtures } from '../../features/place/model/placeFixtures';
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

  return <div className="site-shell"><AppHeader authenticated notificationCount={3} /><main className="page-container places-page"><div className="page-title-row"><div><h1>활동 장소 찾기</h1></div><Button variant="secondary" to="/info-feed">취미 정보 피드</Button></div>
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

export function InfoFeedPage() {
  const [type, setType] = useState('전체');
  const [hobby, setHobby] = useState('전체 취미');
  const [hideEnded, setHideEnded] = useState(true);
  const items = infoFeedFixtures.filter((item) => (type === '전체' || item.type === type) && (hobby === '전체 취미' || item.hobby === hobby) && (!hideEnded || !item.ended));

  return <div className="site-shell"><AppHeader authenticated notificationCount={3} /><main className="page-container feed-page"><div className="page-title-row"><div><h1>취미 정보 피드</h1><p>관심·진행 중인 취미의 축제, 강좌, 공연과 입문 자료를 모았어요.</p></div><Button variant="secondary" to="/places">장소 찾기</Button></div>
    <div className="feed-tabs">{['전체', '축제·행사', '강좌·클래스', '공연·전시', '입문 자료'].map((item) => <button className={type === item ? 'active' : ''} type="button" onClick={() => setType(item)} key={item}>{item}</button>)}<label><input checked={hideEnded} onChange={(event) => setHideEnded(event.target.checked)} type="checkbox" /> 종료된 행사 숨기기</label></div>
    <div className="place-hobbies feed-hobbies">{['전체 취미', '클라이밍', '요가', '도예'].map((item) => <button className={hobby === item ? 'active' : ''} type="button" onClick={() => setHobby(item)} key={item}>{item}</button>)}</div>
    <section className="feed-list">{items.length ? items.map((item) => <article key={item.id}><div className="feed-thumb"><Image /><span>{item.type}</span></div><div className="feed-content"><div><span className="badge">{item.type}</span><span className="badge badge--soft">{item.hobby}</span>{item.ended && <span className="badge badge--paused">종료</span>}</div><h2>{item.title}</h2><p>{item.summary}</p><small>{item.source} · {item.location} · {item.period}</small></div><a href="#feed-detail">자세히 보기 <ExternalLink /></a></article>) : <section className="empty-state feed-empty"><img src={characterAssets.searching} alt="정보를 찾는 냠냠취미 캐릭터" /><h2>선택한 조건의 정보가 없어요</h2><p>다른 취미나 정보 유형을 선택해 보세요.</p></section>}</section>
  </main></div>;
}
