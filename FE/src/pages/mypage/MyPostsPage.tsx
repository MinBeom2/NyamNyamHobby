import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { communityQueryKeys, getMyPosts, type PostType } from '../../features/community/api/communityApi';
import { AppHeader } from '../../shared/ui/AppHeader';

const typeLabels: Record<PostType, string> = {
  QUESTION: '질문',
  INFO: '정보',
  REVIEW: '후기',
  SHARE: '공유',
};

export function MyPostsPage() {
  const postsQuery = useQuery({
    queryKey: communityQueryKeys.myPosts(0),
    queryFn: () => getMyPosts({ page: 0, size: 20 }),
  });
  const posts = postsQuery.data?.content ?? [];

  return <div className="site-shell">
    <AppHeader />
    <main className="page-container community-page">
      <header className="community-page__heading"><div><h1>내가 쓴 글</h1></div></header>
      {postsQuery.isLoading && <p role="status">작성한 글을 불러오고 있어요.</p>}
      {postsQuery.isError && <p role="alert">작성한 글을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
      {!postsQuery.isLoading && !postsQuery.isError && posts.length === 0 && <div className="empty-state"><h2>아직 작성한 글이 없어요.</h2></div>}
      {posts.length > 0 && <div className="board-table">
        <div className="board-table__head"><span>분류</span><span>제목</span><span>작성자</span><span>작성일</span><span>댓글</span><span>조회</span></div>
        {posts.map((post) => <Link className="board-row" key={post.id} to={`/community/${post.id}`}>
          <span>{typeLabels[post.type]}</span>
          <strong>{post.title}</strong>
          <span>{post.author.nickname}</span>
          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
          <span>{post.commentCount}</span>
          <span>{post.viewCount}</span>
        </Link>)}
      </div>}
    </main>
  </div>;
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', { month: '2-digit', day: '2-digit' }).format(date);
}
