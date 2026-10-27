import { Link, NavLink } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { brandAssets } from '../assets/assets';

type AppHeaderProps = {
  authenticated?: boolean;
  notificationCount?: number;
  onLogout?: () => void;
  logoutPending?: boolean;
};

const navigation = [
  { label: '숏폼 탐색', to: '/explore' },
  { label: '취미 찾기', to: '/hobbies' },
  { label: '장소·정보', to: '/places' },
  { label: '커뮤니티', to: '/community' },
  { label: '공지', to: '/notices' },
];

export function AppHeader({ authenticated = false, notificationCount = 0, onLogout, logoutPending = false }: AppHeaderProps) {
  return (
    <header className="app-header">
      <div className="app-header__inner">
        <Link className="brand" to={authenticated ? '/home' : '/'} aria-label="냠냠취미 홈">
          <img src={brandAssets.logo} alt="냠냠취미" />
        </Link>

        <nav className="main-nav" aria-label="주요 메뉴">
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="account-nav">
          {authenticated ? (
            <>
              <Link className="notification-link" to="/notifications" aria-label={`읽지 않은 알림 ${notificationCount}개`}>
                <Bell aria-hidden="true" size={20} strokeWidth={1.8} />
                {notificationCount > 0 && <b>{notificationCount}</b>}
              </Link>
              <Link to="/mypage">마이페이지</Link>
              <button className="account-nav__button" type="button" onClick={onLogout} disabled={logoutPending}>
                {logoutPending ? '로그아웃 중…' : '로그아웃'}
              </button>
            </>
          ) : (
            <>
              <Link className="login-link" to="/login">로그인</Link>
              <Link className="signup-link" to="/signup">회원가입</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
