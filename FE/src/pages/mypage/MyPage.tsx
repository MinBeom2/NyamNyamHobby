import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useSupportMockStore } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';
import { ProfileEditModal } from './ProfileEditPages';
import { getMySummary, getUserHobbies, getUserProfile, userQueryKeys } from '../../features/user/api/userApi';
import { activityQueryKeys, updateUserHobbyStatus } from '../../features/activity/api/activityApi';

export function MyPage({ profileEditorOpen = false }: { profileEditorOpen?: boolean }) {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<HobbyTab>('IN_PROGRESS');
  const user = useAuthStore((state) => state.user);
  const inquiries = useSupportMockStore((state) => state.inquiries);
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);

  const profileQuery = useQuery({ queryKey: userQueryKeys.profile(), queryFn: getUserProfile });
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const summaryQuery = useQuery({ queryKey: userQueryKeys.summary(), queryFn: getMySummary });
  const resumeMutation = useMutation({
    mutationFn: (hobbyId: number) => updateUserHobbyStatus(hobbyId, 'IN_PROGRESS'),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: userQueryKeys.all }),
        queryClient.invalidateQueries({ queryKey: activityQueryKeys.all }),
      ]);
    },
  });

  const profile = profileQuery.data;
  const summary = summaryQuery.data;
  const hobbies = hobbiesQuery.data ?? [];
  const inProgress = hobbies.filter((item) => item.status === 'IN_PROGRESS' || item.status === 'PAUSED');
  const visibleHobbies = hobbies.filter((item) => item.status === tab);
  const conditions = profile?.conditions;
  const savedHobbyCount = summary?.savedHobbyCount ?? hobbies.filter((item) => item.status === 'SAVED').length;
  const inProgressCount = summary?.inProgressHobbyCount ?? inProgress.length;
  const savedShortCount = summary?.savedShortCount ?? (summaryQuery.isError ? '확인 불가' : '…');
  const myPostCount = summary?.myPostCount ?? 0;

  return (
    <div className="site-shell">
      <AppHeader notificationCount={unreadCount} />
      <main className="page-container mypage-preview">
        <aside className="mypage-profile">
          <div className="mypage-avatar">
            {(profile?.nickname ?? user?.nickname ?? '취').slice(0, 1)}
            <Link to="/mypage/profile" aria-label="프로필 수정">
              <Pencil size={14} />
            </Link>
          </div>
          <h1>{profile?.nickname ?? user?.nickname ?? '취미찾는사람'}</h1>
          <p>{profile?.email ?? user?.email ?? 'user@example.com'}</p>
          <Link className="button button--secondary" to="/mypage/edit">
            개인정보 수정
          </Link>
          <hr />
          <h2>이용 상황</h2>
          <dl>
            <div>
              <dt>활동 시간</dt>
              <dd>{conditions?.weeklyHours == null ? '미설정' : `주 ${conditions.weeklyHours}시간`}</dd>
            </div>
            <div>
              <dt>예산</dt>
              <dd>
                {conditions?.monthlyBudget == null
                  ? '미설정'
                  : `월 ${Math.round(conditions.monthlyBudget / 10000)}만 원`}
              </dd>
            </div>
            <div>
              <dt>실내 · 실외</dt>
              <dd>{conditions?.placePref ? placeLabel[conditions.placePref] : '미설정'}</dd>
            </div>
            <div>
              <dt>혼자 · 함께</dt>
              <dd>{conditions?.socialPref ? socialLabel[conditions.socialPref] : '미설정'}</dd>
            </div>
            <div>
              <dt>선호 지역</dt>
              <dd>
                {conditions?.region ? `${conditions.region.sido} ${conditions.region.sigungu}` : '미설정'}
              </dd>
            </div>
          </dl>
          <Link className="mypage-inquiry-link" to="/support/inquiries">
            내 문의 모아보기 <b>{inquiries.length}</b>
          </Link>
        </aside>
        <section className="mypage-content">
          <div className="mypage-stats">
            <Link to="/my-hobbies" aria-label="관심 취미 보기">
              <strong>{savedHobbyCount}</strong>
              <span>관심 취미</span>
            </Link>
            <article>
              <strong>{inProgressCount}</strong>
              <span>진행 중</span>
            </article>
            <Link to="/mypage/saved-shorts" aria-label="보관한 숏폼 보기">
              <strong>{savedShortCount}</strong>
              <span>보관한 숏폼</span>
            </Link>
            <Link to="/mypage/posts">
              <strong>{myPostCount}</strong>
              <span>내 글</span>
            </Link>
          </div>
          <nav className="mypage-tabs" aria-label="내 취미 상태">
            {hobbyTabs.map((item) => (
              <button
                aria-pressed={tab === item.status}
                className={tab === item.status ? 'active' : ''}
                key={item.status}
                onClick={() => setTab(item.status)}
                type="button"
              >
                {item.label}
              </button>
            ))}
          </nav>
          {visibleHobbies.map((hobby) => (
            <div className="mypage-hobby-row" key={hobby.userHobbyId}>
              <strong>{hobby.name}</strong>
              <span>{statusLabel[hobby.status]}</span>
              <i>
                <em style={{ width: `${Math.max(0, Math.min(100, hobby.progressRate))}%` }} />
              </i>
              {hobby.status === 'PAUSED' ? (
                <button
                  className="button button--primary"
                  disabled={resumeMutation.isPending}
                  onClick={() => resumeMutation.mutate(hobby.hobbyId)}
                  type="button"
                >
                  다시 시작
                </button>
              ) : (
                <Link
                  className="button button--primary"
                  to={
                    hobby.planId
                      ? `/my-hobbies/${hobby.hobbyId}/plan?planId=${hobby.planId}`
                      : `/hobbies/${hobby.hobbyId}`
                  }
                >
                  이어하기
                </Link>
              )}
            </div>
          ))}
          {!hobbiesQuery.isLoading && visibleHobbies.length === 0 && (
            <p className="mypage-empty">{tab === 'PAUSED' ? '일시중지한 취미가 없어요.' : '진행 중인 취미가 없어요.'}</p>
          )}
        </section>
      </main>
      {profileEditorOpen && <ProfileEditModal />}
    </div>
  );
}

type HobbyTab = 'IN_PROGRESS' | 'PAUSED';
const hobbyTabs: Array<{ status: HobbyTab; label: string }> = [
  { status: 'IN_PROGRESS', label: '진행 중' },
  { status: 'PAUSED', label: '일시중지' },
];
const placeLabel = { INDOOR: '실내', OUTDOOR: '실외', ANY: '상관없음' } as const;
const socialLabel = { ALONE: '혼자', TOGETHER: '함께', ANY: '상관없음' } as const;
const statusLabel = {
  SAVED: '관심',
  IN_PROGRESS: '진행 중',
  PAUSED: '일시중지',
  COMPLETED: '완료',
  STOPPED: '중단',
} as const;
