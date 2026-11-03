import { Link } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useSupportMockStore } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';
import { ProfileEditModal } from './ProfileEditPages';
import {
  getMyPosts,
  getMySummary,
  getUserHobbies,
  getUserProfile,
  userQueryKeys,
} from '../../features/user/api/userApi';

type MyPageTab = 'IN_PROGRESS' | 'SAVED' | 'POSTS';

export function MyPage({ profileEditorOpen = false }: { profileEditorOpen?: boolean }) {
  const [activeTab, setActiveTab] = useState<MyPageTab>('IN_PROGRESS');
  const user = useAuthStore((state) => state.user);
  const inquiries = useSupportMockStore((state) => state.inquiries);
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);

  const profileQuery = useQuery({ queryKey: userQueryKeys.profile(), queryFn: getUserProfile });
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const summaryQuery = useQuery({ queryKey: userQueryKeys.summary(), queryFn: getMySummary });
  const myPostsQuery = useQuery({
    queryKey: userQueryKeys.posts(),
    queryFn: () => getMyPosts(0, 20),
    enabled: activeTab === 'POSTS',
  });

  const profile = profileQuery.data;
  const summary = summaryQuery.data;
  const hobbies = hobbiesQuery.data ?? [];
  const inProgress = hobbies.filter((item) => item.status === 'IN_PROGRESS' || item.status === 'PAUSED');
  const savedHobbies = hobbies.filter((item) => item.status === 'SAVED');
  const myPosts = myPostsQuery.data?.content ?? [];
  const conditions = profile?.conditions;

  const savedHobbyCount = summary?.savedHobbyCount ?? savedHobbies.length;
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
            <article
              style={{ cursor: 'pointer' }}
              onClick={() => setActiveTab('SAVED')}
              role="button"
              tabIndex={0}
            >
              <strong>{savedHobbyCount}</strong>
              <span>관심 취미</span>
            </article>
            <article
              style={{ cursor: 'pointer' }}
              onClick={() => setActiveTab('IN_PROGRESS')}
              role="button"
              tabIndex={0}
            >
              <strong>{inProgressCount}</strong>
              <span>진행 중</span>
            </article>
            <Link to="/mypage/saved-shorts" aria-label="보관한 숏폼 보기">
              <strong>{savedShortCount}</strong>
              <span>보관한 숏폼</span>
            </Link>
            <article
              style={{ cursor: 'pointer' }}
              onClick={() => setActiveTab('POSTS')}
              role="button"
              tabIndex={0}
            >
              <strong>{myPostCount}</strong>
              <span>내 글</span>
            </article>
          </div>

          <nav className="mypage-tabs">
            {activeTab === 'IN_PROGRESS' ? (
              <b>진행 중</b>
            ) : (
              <span style={{ cursor: 'pointer' }} onClick={() => setActiveTab('IN_PROGRESS')}>
                진행 중
              </span>
            )}
            {activeTab === 'SAVED' ? (
              <b>관심 취미</b>
            ) : (
              <span style={{ cursor: 'pointer' }} onClick={() => setActiveTab('SAVED')}>
                관심 취미
              </span>
            )}
            <Link to="/mypage/saved-shorts">보관한 숏폼</Link>
            {activeTab === 'POSTS' ? (
              <b>내가 쓴 글</b>
            ) : (
              <span style={{ cursor: 'pointer' }} onClick={() => setActiveTab('POSTS')}>
                내가 쓴 글
              </span>
            )}
          </nav>

          {activeTab === 'IN_PROGRESS' && (
            <>
              {inProgress.map((hobby) => (
                <div className="mypage-hobby-row" key={hobby.userHobbyId}>
                  <strong>{hobby.name}</strong>
                  <span>{statusLabel[hobby.status]}</span>
                  <i>
                    <em style={{ width: `${Math.max(0, Math.min(100, hobby.progressRate))}%` }} />
                  </i>
                  <Link
                    className="button button--primary"
                    to={
                      hobby.planId
                        ? `/my-hobbies/${hobby.hobbyId}/plan?planId=${hobby.planId}`
                        : `/hobbies/${hobby.hobbyId}`
                    }
                  >
                    {hobby.status === 'PAUSED' ? '다시 시작' : '이어하기'}
                  </Link>
                </div>
              ))}
              {!hobbiesQuery.isLoading && inProgress.length === 0 && (
                <p className="mypage-empty">진행 중인 취미가 없어요.</p>
              )}
            </>
          )}

          {activeTab === 'SAVED' && (
            <>
              {savedHobbies.map((hobby) => (
                <div className="mypage-hobby-row" key={hobby.userHobbyId}>
                  <strong>{hobby.name}</strong>
                  <span>{statusLabel[hobby.status]}</span>
                  <i />
                  <Link className="button button--primary" to={`/hobbies/${hobby.hobbyId}`}>
                    상세보기
                  </Link>
                </div>
              ))}
              {!hobbiesQuery.isLoading && savedHobbies.length === 0 && (
                <p className="mypage-empty">관심 취미로 저장된 항목이 없어요.</p>
              )}
            </>
          )}

          {activeTab === 'POSTS' && (
            <>
              <div className="board-table">
                <div className="board-table__head">
                  <span>분류</span>
                  <span>제목</span>
                  <span>작성자</span>
                  <span>작성일</span>
                  <span>댓글</span>
                  <span>조회</span>
                </div>
                {myPosts.length > 0 ? (
                  myPosts.map((post) => (
                    <Link className="board-row" to={`/community/${post.id}`} key={post.id}>
                      <span>
                        <b className="badge">{typeLabel[post.type] ?? post.type}</b>
                      </span>
                      <strong>
                        {post.title} <em>[{post.commentCount}]</em>
                      </strong>
                      <span>{post.author.nickname}</span>
                      <span>{formatDate(post.createdAt)}</span>
                      <span>{post.commentCount}</span>
                      <span>{post.viewCount}</span>
                    </Link>
                  ))
                ) : (
                  <p className="mypage-empty" style={{ padding: '32px 0' }}>
                    작성한 글이 없어요.
                  </p>
                )}
              </div>
            </>
          )}
        </section>
      </main>
      {profileEditorOpen && <ProfileEditModal />}
    </div>
  );
}

const typeLabel: Record<string, string> = {
  REVIEW: '후기',
  QUESTION: '질문',
  INFO: '정보 공유',
  SHARE: '모임',
};

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('ko-KR', { month: '2-digit', day: '2-digit' }).format(date);
}

const placeLabel = { INDOOR: '실내', OUTDOOR: '실외', ANY: '상관없음' } as const;
const socialLabel = { ALONE: '혼자', TOGETHER: '함께', ANY: '상관없음' } as const;
const statusLabel = {
  SAVED: '관심',
  IN_PROGRESS: '진행 중',
  PAUSED: '일시중지',
  COMPLETED: '완료',
  STOPPED: '중단',
} as const;
