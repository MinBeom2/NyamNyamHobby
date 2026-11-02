import { useCallback, useEffect } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { Bell, X } from 'lucide-react';
import { brandAssets, characterAssets } from '../assets/assets';

type AppHeaderProps = {
  authenticated?: boolean;
  notificationCount?: number;
  onLogout?: () => void;
  onBrandClick?: () => void;
  logoutPending?: boolean;
};

const navigation = [
  { label: '숏폼 탐색', to: '/explore' },
  { label: '취미 찾기', to: '/hobbies', requiresAuth: true },
  { label: '장소·정보', to: '/places', requiresAuth: true },
  { label: '커뮤니티', to: '/community', requiresAuth: true },
  { label: '공지', to: '/notices' },
];

type SignupRequiredState = {
  signupRequired: string;
  returnTo: string;
};

function getSignupRequiredState(value: unknown): SignupRequiredState | null {
  if (!value || typeof value !== 'object') return null;
  const state = value as Record<string, unknown>;
  if (typeof state.signupRequired !== 'string' || typeof state.returnTo !== 'string') return null;
  return { signupRequired: state.signupRequired, returnTo: state.returnTo };
}

export function AppHeader({ authenticated = false, notificationCount = 0, onLogout, onBrandClick, logoutPending = false }: AppHeaderProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const restriction = authenticated ? null : getSignupRequiredState(location.state);
  const restrictedMenu = restriction?.signupRequired ?? null;
  const headerAuthenticated = authenticated;
  const closeRestriction = useCallback(() => {
    if (!restriction) return;
    navigate(restriction.returnTo, { replace: true });
  }, [navigate, restriction]);

  useEffect(() => {
    if (!restrictedMenu) return;

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') closeRestriction();
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [closeRestriction, restrictedMenu]);

  return (
    <>
      <header className="app-header">
        <div className="app-header__inner">
          <Link className="brand" to={headerAuthenticated ? '/home' : '/'} aria-label="냠냠취미 홈" onClick={onBrandClick}>
            <img src={brandAssets.logo} alt="냠냠취미" />
          </Link>

          <nav className="main-nav" aria-label="주요 메뉴">
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={(event) => {
                  if (!headerAuthenticated && item.requiresAuth) {
                    event.preventDefault();
                    navigate(item.to, {
                      state: {
                        signupRequired: item.label,
                        returnTo: `${location.pathname}${location.search}`,
                      } satisfies SignupRequiredState,
                    });
                  }
                }}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="account-nav">
            {headerAuthenticated ? (
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

      {restrictedMenu && (
        <div className="signup-required-backdrop" role="presentation" onMouseDown={closeRestriction}>
          <section
            className="signup-required-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="signup-required-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button className="signup-required-modal__close" type="button" aria-label="닫기" onClick={closeRestriction}>
              <X aria-hidden="true" size={20} />
            </button>
            <img src={characterAssets.signupRequired} alt="부탁하는 냠냠취미 캐릭터" />
            <span>{restrictedMenu}</span>
            <h2 id="signup-required-title">회원가입 후 이용할 수 있어요</h2>
            <p>회원이 되면 취향에 맞는 취미와 장소를 찾고<br />다른 사용자들과 이야기를 나눌 수 있어요.</p>
            <div className="signup-required-modal__actions">
              <button className="button button--secondary" type="button" onClick={closeRestriction}>닫기</button>
              <Link className="button button--primary" to="/signup">회원가입</Link>
            </div>
            <Link className="signup-required-modal__login" to="/login">이미 회원이신가요? 로그인</Link>
          </section>
        </div>
      )}
    </>
  );
}
