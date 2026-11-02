import { Navigate, Link, useParams } from 'react-router-dom';
import { noticeCategoryLabels, noticeFixtures } from '../../features/notice/model/noticeFixtures';
import { useSupportMockStore } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';

function NoticeHeader() {
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);
  return <AppHeader notificationCount={unreadCount} />;
}

export function NoticeListPage() {
  const notices = [...noticeFixtures].sort((a, b) => Number(b.pinned) - Number(a.pinned));
  return <div className="site-shell"><NoticeHeader /><main className="page-container notice-page">
    <header className="notice-heading"><h1>공지사항</h1><span>전체 {notices.length}</span></header>
    <div className="notice-list">{notices.map((notice) => <Link key={notice.id} className={notice.pinned ? 'notice-item notice-item--pinned' : 'notice-item'} to={`/notices/${notice.id}`}>
      <div><span className={`notice-badge notice-badge--${notice.category.toLowerCase()}`}>{noticeCategoryLabels[notice.category]}</span>{notice.pinned && <b>중요</b>}<strong>{notice.title}</strong></div>
      <div><time>{notice.publishedAt}</time><i>›</i></div>
    </Link>)}</div>
  </main></div>;
}

export function NoticeDetailPage() {
  const { noticeId } = useParams();
  const notice = noticeFixtures.find((item) => item.id === Number(noticeId));
  if (!notice) return <Navigate replace to="/notices" />;
  return <div className="site-shell"><NoticeHeader /><main className="page-container notice-page">
    <header className="notice-heading"><h1>공지사항</h1></header>
    <article className="notice-detail"><div className="notice-detail__badges"><span className={`notice-badge notice-badge--${notice.category.toLowerCase()}`}>{noticeCategoryLabels[notice.category]}</span>{notice.pinned && <b>중요</b>}</div>
      <h2>{notice.title}</h2><time>{notice.publishedAt} · 관리자</time><hr /><p>{notice.content}</p><Link className="button button--secondary" to="/notices">목록으로</Link>
    </article>
  </main></div>;
}
