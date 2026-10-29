import { Navigate, Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/model/authStore';
import { noticeCategoryLabels, noticeFixtures } from '../../features/notice/model/noticeFixtures';
import { useSupportMockStore } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';

type NoticeLocationState = {
  authenticatedContext?: boolean;
};

function useNoticeAuthentication() {
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const navigationState = location.state as NoticeLocationState | null;

  return Boolean(user) || navigationState?.authenticatedContext === true;
}

function NoticeHeader({ authenticated }: { authenticated: boolean }) {
  const navigate = useNavigate();
  const clearSession = useAuthStore((state) => state.clearSession);
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);
  const logout = () => {
    clearSession();
    navigate('/');
  };
  return <AppHeader authenticated={authenticated} notificationCount={unreadCount} onLogout={logout} />;
}

export function NoticeListPage() {
  const authenticated = useNoticeAuthentication();
  const notices = [...noticeFixtures].sort((a, b) => Number(b.pinned) - Number(a.pinned));
  return <div className="site-shell"><NoticeHeader authenticated={authenticated} /><main className="page-container notice-page">
    <header className="notice-heading"><h1>공지사항</h1><span>전체 {notices.length}</span></header>
    <div className="notice-list">{notices.map((notice) => <Link key={notice.id} className={notice.pinned ? 'notice-item notice-item--pinned' : 'notice-item'} to={`/notices/${notice.id}`} state={{ authenticatedContext: authenticated }}>
      <div><span className={`notice-badge notice-badge--${notice.category.toLowerCase()}`}>{noticeCategoryLabels[notice.category]}</span>{notice.pinned && <b>중요</b>}<strong>{notice.title}</strong></div>
      <div><time>{notice.publishedAt}</time><i>›</i></div>
    </Link>)}</div>
  </main></div>;
}

export function NoticeDetailPage() {
  const authenticated = useNoticeAuthentication();
  const { noticeId } = useParams();
  const notice = noticeFixtures.find((item) => item.id === Number(noticeId));
  if (!notice) return <Navigate replace to="/notices" state={{ authenticatedContext: authenticated }} />;
  return <div className="site-shell"><NoticeHeader authenticated={authenticated} /><main className="page-container notice-page">
    <header className="notice-heading"><h1>공지사항</h1></header>
    <article className="notice-detail"><div className="notice-detail__badges"><span className={`notice-badge notice-badge--${notice.category.toLowerCase()}`}>{noticeCategoryLabels[notice.category]}</span>{notice.pinned && <b>중요</b>}</div>
      <h2>{notice.title}</h2><time>{notice.publishedAt} · 관리자</time><hr /><p>{notice.content}</p><Link className="button button--secondary" to="/notices" state={{ authenticatedContext: authenticated }}>목록으로</Link>
    </article>
  </main></div>;
}
