import { Heart, Image, Search, ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { HobbyListView } from '../model/managementAdapter';

function Thumbnail({ src }: { src: string | null }) {
  return <span className="hm-thumbnail">{src ? <img src={src} alt="" /> : <Image size={22} aria-hidden="true" />}</span>;
}

export function HobbySidebar({ list, selectedId, count, max, unsave, removingId, removeError, onSelect }: {
  list: HobbyListView; selectedId?: number; count: number; max: number;
  unsave: (id: number) => void; removingId?: number; removeError?: string; onSelect?: () => void;
}) {
  return <nav className="hm-sidebar" aria-label="내 취미 목록">
    <div className="hm-sidebar__title"><span className="hm-eyebrow">MY HOBBIES</span><h1>내 취미</h1><p>좋아하는 일, 나의 속도로.</p></div>
    <section>
      <h2 className="hm-section-title">내 계획 <span>{list.plans.length}</span></h2>
      <p className="hm-sidebar__counts">진행 중 {count}/{max} · 일시중지 {list.pausedCount}</p>
      <div className="hm-hobby-list">{list.plans.map((hobby) => <Link key={hobby.id} to={hobby.link} onClick={onSelect} aria-current={selectedId === hobby.id ? 'page' : undefined} className={`hm-hobby ${selectedId === hobby.id ? 'hm-hobby--selected' : ''}`}>
        <div className="hm-hobby__top"><Thumbnail src={hobby.thumbnail} /><div><b>{hobby.name}</b>{hobby.paused && <span className="hm-badge hm-badge--paused">일시중지</span>}</div><ChevronRight size={16} aria-hidden="true" /></div>
        <div className="hm-hobby__meta"><span>{hobby.weekLabel}</span><b>{hobby.progress.percent}%</b></div>
        <div className="hm-mini-track" aria-label={`${hobby.name} 완료율 ${hobby.progress.percent}%`}><span style={{ width: `${hobby.progress.percent}%` }} /></div>
      </Link>)}</div>
      {!list.plans.length && <p className="hm-muted">아직 시작한 계획이 없어요.</p>}
    </section>
    <section>
      <h2 className="hm-section-title">관심 목록 <span>{list.saved.length}</span></h2>
      <div className="hm-saved-list">{list.saved.map((hobby) => <article className="hm-saved" key={hobby.id}>
        <Link to={hobby.link} onClick={onSelect}><Thumbnail src={hobby.thumbnail} /><span><b>{hobby.name}</b><small>시작 전</small></span></Link>
        <button type="button" className="hm-icon-button hm-heart" disabled={removingId === hobby.id} onClick={() => unsave(hobby.id)} aria-label={`${hobby.name} 관심 해제`}><Heart size={18} fill="currentColor" /></button>
      </article>)}</div>
      {!list.saved.length && <p className="hm-muted">관심 있는 취미를 저장해 보세요.</p>}
      {removeError && <p className="hm-error" role="alert">{removeError} 하트를 눌러 다시 시도해 주세요.</p>}
    </section>
    <Link className="hm-find-link" to="/hobbies" onClick={onSelect}><Search size={17} /> 취미 찾기 <ChevronRight size={16} /></Link>
  </nav>;
}
