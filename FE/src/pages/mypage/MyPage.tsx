import { Link, useNavigate } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { logout } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useSupportMockStore } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';
import { ProfileEditModal } from './ProfileEditPages';
import { getUserHobbies, getUserProfile, userQueryKeys } from '../../features/user/api/userApi';

export function MyPage({ profileEditorOpen = false }: { profileEditorOpen?: boolean }) {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const inquiries = useSupportMockStore((state) => state.inquiries);
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession();
      navigate('/', { replace: true });
    },
  });
  const profileQuery = useQuery({ queryKey: userQueryKeys.profile(), queryFn: getUserProfile });
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const profile = profileQuery.data;
  const hobbies = hobbiesQuery.data ?? [];
  const inProgress = hobbies.filter((item) => item.status === 'IN_PROGRESS' || item.status === 'PAUSED');
  const conditions = profile?.conditions;
  return <div className="site-shell"><AppHeader authenticated notificationCount={unreadCount} onLogout={() => logoutMutation.mutate()} logoutPending={logoutMutation.isPending} /><main className="page-container mypage-preview">
    <aside className="mypage-profile"><div className="mypage-avatar">{(profile?.nickname ?? user?.nickname ?? '취').slice(0, 1)}<Link to="/mypage/profile" aria-label="프로필 수정"><Pencil size={14} /></Link></div><h1>{profile?.nickname ?? user?.nickname ?? '취미찾는사람'}</h1><p>{profile?.email ?? user?.email ?? 'user@example.com'}</p><Link className="button button--secondary" to="/mypage/edit">개인정보 수정</Link><hr /><h2>이용 상황</h2><dl><div><dt>활동 시간</dt><dd>{conditions?.weeklyHours == null ? '미설정' : `주 ${conditions.weeklyHours}시간`}</dd></div><div><dt>예산</dt><dd>{conditions?.monthlyBudget == null ? '미설정' : `월 ${Math.round(conditions.monthlyBudget / 10000)}만 원`}</dd></div><div><dt>실내 · 실외</dt><dd>{conditions?.placePref ? placeLabel[conditions.placePref] : '미설정'}</dd></div><div><dt>혼자 · 함께</dt><dd>{conditions?.socialPref ? socialLabel[conditions.socialPref] : '미설정'}</dd></div><div><dt>선호 지역</dt><dd>{conditions?.region ? `${conditions.region.sido} ${conditions.region.sigungu}` : '미설정'}</dd></div></dl><Link className="mypage-inquiry-link" to="/support/inquiries">내 문의 모아보기 <b>{inquiries.length}</b></Link></aside>
    <section className="mypage-content"><div className="mypage-stats"><article><strong>{hobbies.length}</strong><span>관심 취미</span></article><article><strong>{inProgress.length}</strong><span>진행 중</span></article><Link to="/mypage/saved-shorts" aria-label="보관한 숏폼 보기"><strong>12</strong><span>보관한 숏폼</span></Link><article><strong>3</strong><span>내 글</span></article></div><nav className="mypage-tabs"><b>진행 중</b><span>관심 취미</span><Link to="/mypage/saved-shorts">보관한 숏폼</Link><span>내가 쓴 글</span><span>이용 상황</span></nav>{inProgress.map((hobby) => <div className="mypage-hobby-row" key={hobby.userHobbyId}><strong>{hobby.name}</strong><span>{statusLabel[hobby.status]}</span><i><em style={{ width: `${Math.max(0, Math.min(100, hobby.progressRate))}%` }} /></i><Link className="button button--primary" to={`/hobbies/${hobby.hobbyId}`}>{hobby.status === 'PAUSED' ? '다시 시작' : '이어하기'}</Link></div>)}{!hobbiesQuery.isLoading && inProgress.length === 0 && <p className="mypage-empty">진행 중인 취미가 없어요.</p>}</section>
  </main>{profileEditorOpen && <ProfileEditModal />}</div>;
}

const placeLabel = { INDOOR: '실내', OUTDOOR: '실외', ANY: '상관없음' } as const;
const socialLabel = { ALONE: '혼자', TOGETHER: '함께', ANY: '상관없음' } as const;
const statusLabel = { SAVED: '관심', IN_PROGRESS: '진행 중', PAUSED: '일시중지', COMPLETED: '완료', STOPPED: '중단' } as const;
