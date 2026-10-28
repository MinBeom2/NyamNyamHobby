import { Link } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useSupportMockStore } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';
import { ProfileEditModal } from './ProfileEditPages';

export function MyPage({ profileEditorOpen = false }: { profileEditorOpen?: boolean }) {
  const user = useAuthStore((state) => state.user);
  const inquiries = useSupportMockStore((state) => state.inquiries);
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);
  return <div className="site-shell"><AppHeader authenticated notificationCount={unreadCount} /><main className="page-container mypage-preview">
    <aside className="mypage-profile"><div className="mypage-avatar">취<Link to="/mypage/profile" aria-label="프로필 수정"><Pencil size={14} /></Link></div><h1>{user?.nickname ?? '취미찾는사람'}</h1><p>{user?.email ?? 'user@example.com'}</p><Link className="button button--secondary" to="/mypage/edit">개인정보 수정</Link><hr /><h2>이용 상황</h2><dl><div><dt>활동 시간</dt><dd>주 3시간</dd></div><div><dt>예산</dt><dd>월 15만 원</dd></div><div><dt>실내 · 실외</dt><dd>실내</dd></div><div><dt>혼자 · 함께</dt><dd>상관없음</dd></div><div><dt>선호 지역</dt><dd>서울 마포구</dd></div></dl><Link className="mypage-inquiry-link" to="/support/inquiries">내 문의 모아보기 <b>{inquiries.length}</b></Link></aside>
    <section className="mypage-content"><div className="mypage-stats"><article><strong>4</strong><span>관심 취미</span></article><article><strong>2</strong><span>진행 중</span></article><Link to="/mypage/saved-shorts" aria-label="보관한 숏폼 12개 보기"><strong>12</strong><span>보관한 숏폼</span></Link><article><strong>3</strong><span>내 글</span></article></div><nav className="mypage-tabs"><b>진행 중</b><span>관심 취미</span><Link to="/mypage/saved-shorts">보관한 숏폼</Link><span>내가 쓴 글</span><span>이용 상황</span></nav><div className="mypage-hobby-row"><strong>클라이밍</strong><span>진행 중</span><i><em style={{ width: '34%' }} /></i><button>이어하기</button></div><div className="mypage-hobby-row"><strong>요가</strong><span>진행 중</span><i><em style={{ width: '12%' }} /></i><button>이어하기</button></div><div className="mypage-hobby-row"><strong>도예</strong><span>일시중지</span><i><em style={{ width: '28%' }} /></i><button>다시 시작</button></div></section>
  </main>{profileEditorOpen && <ProfileEditModal />}</div>;
}
