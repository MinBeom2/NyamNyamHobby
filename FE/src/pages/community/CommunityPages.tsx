import { Flag, Heart, Image, Link2, MessageSquareReply, Search, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  communityCommentFixtures,
  type CommunityComment,
} from '../../features/community/model/communityFixtures';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';
import { getOnboardingOptions, userQueryKeys } from '../../features/user/api/userApi';
import {
  communityQueryKeys,
  createPost,
  deletePost,
  getPost,
  getPosts,
  likePost,
  unlikePost,
  updatePost,
  uploadPostImage,
  type PostType,
} from '../../features/community/api/communityApi';

const categories = ['전체글', '후기', '질문', '정보 공유', '모임'] as const;
const categoryType: Record<(typeof categories)[number], PostType | undefined> = { '전체글': undefined, '후기': 'REVIEW', '질문': 'QUESTION', '정보 공유': 'INFO', '모임': 'SHARE' };
const typeLabel: Record<PostType, (typeof categories)[number]> = { REVIEW: '후기', QUESTION: '질문', INFO: '정보 공유', SHARE: '모임' };

export function CommunityListPage() {
  const [category, setCategory] = useState<(typeof categories)[number]>('전체글');
  const [keyword, setKeyword] = useState('');
  const [hobbyId, setHobbyId] = useState<number | null>(null);
  const optionsQuery = useQuery({ queryKey: userQueryKeys.onboardingOptions(), queryFn: getOnboardingOptions });
  useEffect(() => { if (hobbyId === null && optionsQuery.data?.hobbies[0]) setHobbyId(optionsQuery.data.hobbies[0].hobbyId); }, [hobbyId, optionsQuery.data]);
  const postsQuery = useQuery({
    queryKey: communityQueryKeys.posts(hobbyId ?? 0, categoryType[category], keyword),
    queryFn: () => getPosts(hobbyId as number, { type: categoryType[category], keyword }),
    enabled: hobbyId !== null,
  });
  const posts = postsQuery.data?.content ?? [];
  const hobby = optionsQuery.data?.hobbies.find((item) => item.hobbyId === hobbyId);

  return (
    <div className="site-shell">
      <AppHeader authenticated notificationCount={3} />
      <main className="page-container community-page">
        <div className="community-layout">
          <aside className="board-sidebar">
            <h2>취미 게시판</h2>
            {categories.map((item) => (
              <button className={category === item ? 'active' : ''} type="button" onClick={() => setCategory(item)} key={item}>
                {item}
                <small>{item === category ? postsQuery.data?.page.totalElements ?? 0 : ''}</small>
              </button>
            ))}
          </aside>

          <section className="board-main">
            <div className="page-title-row">
              <div><h1>{hobby ? `${hobby.name} 게시판` : '취미 게시판'}</h1><p>취미 경험과 질문, 유용한 정보를 나눠 보세요.</p></div>
              <Button to="/community/new">글쓰기</Button>
            </div>
            <div className="board-tools">
              <div>{(optionsQuery.data?.hobbies ?? []).slice(0, 6).map((item) => <button className={hobbyId === item.hobbyId ? 'active' : ''} onClick={() => setHobbyId(item.hobbyId)} type="button" key={item.hobbyId}>{item.name}</button>)}</div>
              <label><Search /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="제목 검색" /></label>
            </div>
            <div className="board-table">
              <div className="board-table__head"><span>분류</span><span>제목</span><span>작성자</span><span>작성일</span><span>댓글</span><span>조회</span></div>
              {posts.length ? posts.map((post) => (
                <Link className="board-row" to={`/community/${post.id}`} key={post.id}>
                  <span><b className="badge">{typeLabel[post.type]}</b></span>
                  <strong>{post.title} <em>[{post.commentCount}]</em></strong>
                  <span>{post.author.nickname}</span><span>{formatDate(post.createdAt)}</span><span>{post.commentCount}</span><span>{post.viewCount}</span>
                </Link>
              )) : <section className="mini-empty"><Search /><b>검색 결과가 없어요</b></section>}
            </div>
            <nav className="pagination" aria-label="페이지 이동"><button type="button">‹</button><button className="active" type="button">1</button><button type="button">2</button><button type="button">3</button><button type="button">›</button></nav>
          </section>
        </div>
      </main>
    </div>
  );
}

export function CommunityDetailPage() {
  const { postId } = useParams();
  const postNumber = Number(postId);
  const queryClient = useQueryClient();
  const postQuery = useQuery({ queryKey: communityQueryKeys.post(postNumber), queryFn: () => getPost(postNumber), enabled: Number.isFinite(postNumber) });
  const post = postQuery.data;
  const [comments, setComments] = useState<CommunityComment[]>(communityCommentFixtures);
  const [comment, setComment] = useState('');
  const [replyTarget, setReplyTarget] = useState<number | null>(null);
  const [reply, setReply] = useState('');
  const [likedComments, setLikedComments] = useState<Set<number>>(() => new Set());
  const [linkCopied, setLinkCopied] = useState(false);
  const [reportTarget, setReportTarget] = useState<string | null>(null);

  const likeMutation = useMutation({
    mutationFn: () => post?.isLiked ? unlikePost(post.id) : likePost(postNumber),
    onSuccess: (result) => queryClient.setQueryData(communityQueryKeys.post(postNumber), (current: typeof post) => current ? { ...current, ...result } : current),
  });
  const deleteMutation = useMutation({ mutationFn: () => deletePost(postNumber), onSuccess: () => navigateToCommunity() });
  const navigate = useNavigate();
  const navigateToCommunity = () => navigate('/community', { replace: true });

  if (postQuery.isLoading) return <div className="site-shell"><AppHeader authenticated /><main className="page-container empty-state"><h1>게시글을 불러오는 중이에요</h1></main></div>;
  if (!post) {
    return <div className="site-shell"><AppHeader authenticated /><main className="page-container empty-state"><h1>게시글을 찾을 수 없어요</h1><Button to="/community">목록으로</Button></main></div>;
  }

  const visibleCommentCount = comments.filter((item) => !item.deleted).length;

  function submitComment(event: FormEvent) {
    event.preventDefault();
    if (!comment.trim()) return;
    setComments((items) => [...items, {
      id: Date.now(), author: '취미찾는사람', avatar: '취', content: comment.trim(), date: '방금 전', likes: 0, mine: true, replies: [],
    }]);
    setComment('');
  }

  function submitReply(event: FormEvent, commentId: number) {
    event.preventDefault();
    if (!reply.trim()) return;
    setComments((items) => items.map((item) => item.id === commentId ? {
      ...item,
      replies: [...item.replies, { id: Date.now(), author: '취미찾는사람', avatar: '취', content: reply.trim(), date: '방금 전', likes: 0, mine: true }],
    } : item));
    setReply('');
    setReplyTarget(null);
  }

  function toggleCommentLike(commentId: number) {
    setLikedComments((current) => {
      const next = new Set(current);
      if (next.has(commentId)) next.delete(commentId);
      else next.add(commentId);
      return next;
    });
  }

  function deleteComment(commentId: number) {
    setComments((items) => items.map((item) => item.id === commentId
      ? { ...item, author: '', avatar: '', content: '', deleted: true, replies: [] }
      : item));
  }

  async function copyPostLink() {
    try {
      await navigator.clipboard.writeText(window.location.href);
    } finally {
      setLinkCopied(true);
      window.setTimeout(() => setLinkCopied(false), 1600);
    }
  }

  return (
    <div className="site-shell">
      <AppHeader authenticated notificationCount={3} />
      <main className="page-container post-page">
        <Link className="back-link" to="/community">커뮤니티 · {post.hobby.name}</Link>
        <article className="post-detail">
          <header>
            <div><span className="badge">{typeLabel[post.type]}</span><span className="badge badge--soft">{post.hobby.name}</span><h1>{post.title}</h1><p>{post.author.nickname} · {formatDate(post.createdAt)} · 조회 {post.viewCount}</p></div>
            {post.isMine && <div><button type="button" onClick={() => navigate(`/community/${post.id}/edit`)}>수정</button><button type="button" disabled={deleteMutation.isPending} onClick={() => deleteMutation.mutate()}>삭제</button></div>}
          </header>
          {post.imageUrls.length > 0 && <div className="post-image"><img src={post.imageUrls[0]} alt="게시글 첨부 이미지" /><span>첨부 이미지 1/{post.imageUrls.length}</span></div>}
          <p className="post-content">{post.content}</p>
          <footer className="post-actions">
            <div>
              <button className={post.isLiked ? 'active' : ''} type="button" disabled={likeMutation.isPending} onClick={() => likeMutation.mutate()}><Heart aria-hidden="true" fill={post.isLiked ? 'currentColor' : 'none'} /> 좋아요 {post.likeCount}</button>
              <button type="button" onClick={copyPostLink}><Link2 aria-hidden="true" /> {linkCopied ? '복사했어요' : '링크 복사'}</button>
            </div>
            <button type="button" onClick={() => setReportTarget('게시글')}><Flag aria-hidden="true" /> 신고</button>
          </footer>
        </article>

        <section className="comments" aria-labelledby="comments-title">
          <h2 id="comments-title">댓글 {visibleCommentCount}</h2>
          <label className="comments__label" htmlFor="comment-input">댓글</label>
          <form onSubmit={submitComment}>
            <input id="comment-input" value={comment} onChange={(event) => setComment(event.target.value)} placeholder="댓글을 입력하세요 (500자)" maxLength={500} />
            <button type="submit">등록</button>
          </form>

          <div className="comment-list">
            {comments.map((item) => (
              <article className={item.deleted ? 'comment-item comment-item--deleted' : 'comment-item'} key={item.id}>
                {item.deleted ? <p>삭제된 댓글입니다.</p> : (
                  <>
                    <span className="comment-avatar" aria-hidden="true">{item.avatar}</span>
                    <div className="comment-item__body">
                      <div className="comment-item__author"><b>{item.author}</b><small>{item.date}</small></div>
                      <p>{item.content}</p>
                      <div className="comment-item__actions">
                        <button type="button" onClick={() => { setReplyTarget((current) => current === item.id ? null : item.id); setReply(''); }}><MessageSquareReply aria-hidden="true" /> 답글</button>
                        <button className={likedComments.has(item.id) ? 'active' : ''} type="button" onClick={() => toggleCommentLike(item.id)}><Heart aria-hidden="true" fill={likedComments.has(item.id) ? 'currentColor' : 'none'} /> 좋아요 {item.likes + (likedComments.has(item.id) ? 1 : 0)}</button>
                        <button type="button" onClick={() => setReportTarget(`${item.author}님의 댓글`)}><Flag aria-hidden="true" /> 신고</button>
                        {item.mine && <button type="button" onClick={() => deleteComment(item.id)}><Trash2 aria-hidden="true" /> 삭제</button>}
                      </div>
                      {replyTarget === item.id && (
                        <form className="reply-form" onSubmit={(event) => submitReply(event, item.id)}>
                          <input value={reply} onChange={(event) => setReply(event.target.value)} placeholder={`${item.author}님에게 답글 쓰기`} maxLength={500} autoFocus />
                          <button type="submit">등록</button>
                        </form>
                      )}
                      {item.replies.map((replyItem) => (
                        <div className="comment-reply" key={replyItem.id}>
                          <span className="comment-avatar" aria-hidden="true">{replyItem.avatar}</span>
                          <div><div className="comment-item__author"><b>{replyItem.author}</b><small>{replyItem.date}</small></div><p>{replyItem.content}</p></div>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
        </section>
        {reportTarget && <ReportModal target={reportTarget} onClose={() => setReportTarget(null)} />}
      </main>
    </div>
  );
}

function ReportModal({ target, onClose }: { target: string; onClose: () => void }) {
  const [reason, setReason] = useState('스팸·홍보');
  const [submitted, setSubmitted] = useState(false);

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="report-modal" role="dialog" aria-modal="true" aria-labelledby="report-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="report-title">{target} 신고하기</h2>
        {submitted ? (
          <><p>신고가 접수됐어요. 확인 후 필요한 조치를 진행할게요.</p><Button onClick={onClose}>확인</Button></>
        ) : (
          <><p>신고 사유를 선택해 주세요.</p>{['스팸·홍보', '욕설·비방', '개인정보 노출', '부적절한 내용', '기타'].map((item) => <label key={item}><input checked={reason === item} onChange={() => setReason(item)} name="report" type="radio" />{item}</label>)}<textarea placeholder="상세 사유를 입력해 주세요 (300자)" maxLength={300} /><div><Button variant="secondary" onClick={onClose}>취소</Button><Button onClick={() => setSubmitted(true)}>신고하기</Button></div></>
        )}
      </section>
    </div>
  );
}

export function CommunityWritePage() {
  const navigate = useNavigate();
  const { postId } = useParams();
  const editPostId = postId ? Number(postId) : null;
  const isEdit = editPostId !== null && Number.isFinite(editPostId);
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [hobbyId, setHobbyId] = useState<number | null>(null);
  const [type, setType] = useState<PostType>('REVIEW');
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [message, setMessage] = useState('');
  const optionsQuery = useQuery({ queryKey: userQueryKeys.onboardingOptions(), queryFn: getOnboardingOptions });
  const postQuery = useQuery({
    queryKey: communityQueryKeys.post(editPostId ?? 0),
    queryFn: () => getPost(editPostId as number),
    enabled: isEdit,
  });
  useEffect(() => {
    if (isEdit) return;
    if (hobbyId === null && optionsQuery.data?.hobbies[0]) setHobbyId(optionsQuery.data.hobbies[0].hobbyId);
  }, [hobbyId, isEdit, optionsQuery.data]);
  useEffect(() => {
    if (!postQuery.data) return;
    setHobbyId(postQuery.data.hobby.id);
    setType(postQuery.data.type);
    setTitle(postQuery.data.title);
    setContent(postQuery.data.content);
    setImageUrls(postQuery.data.imageUrls);
  }, [postQuery.data]);
  const createMutation = useMutation({ mutationFn: () => createPost({ hobbyId: hobbyId as number, type, title: title.trim(), content: content.trim(), imageUrls }), onSuccess: (post) => navigate(`/community/${post.id}`), onError: (error: Error) => setMessage(error.message) });
  const updateMutation = useMutation({
    mutationFn: () => updatePost(editPostId as number, { type, title: title.trim(), content: content.trim(), imageUrls }),
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.post(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
      navigate(`/community/${post.id}`);
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const uploadMutation = useMutation({ mutationFn: uploadPostImage, onSuccess: (image) => setImageUrls((items) => [...items, image.url]), onError: (error: Error) => setMessage(error.message) });

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!hobbyId || title.trim().length < 2 || content.trim().length < 10) return;
    if (isEdit) updateMutation.mutate();
    else createMutation.mutate();
  }

  return (
    <div className="site-shell">
      <AppHeader authenticated notificationCount={3} />
      <main className="page-container write-page">
        <h1>{isEdit ? '글 수정' : '글쓰기'}</h1>
        <form onSubmit={submit}>
          <div className="write-grid"><label>게시판<select disabled={isEdit} value={hobbyId ?? ''} onChange={(event) => setHobbyId(Number(event.target.value))}>{isEdit && postQuery.data && <option value={postQuery.data.hobby.id}>{postQuery.data.hobby.name}</option>}{!isEdit && (optionsQuery.data?.hobbies ?? []).map((item) => <option value={item.hobbyId} key={item.hobbyId}>{item.name}</option>)}</select></label><label>글 유형<select value={type} onChange={(event) => setType(event.target.value as PostType)}><option value="REVIEW">후기</option><option value="QUESTION">질문</option><option value="INFO">정보 공유</option><option value="SHARE">모임</option></select></label></div>
          <label>제목<input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="제목을 입력하세요 (50자)" maxLength={50} /></label>
          <label>내용<textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder="내용을 입력하세요 (5,000자)" maxLength={5000} /></label>
          <div className="image-upload">{imageUrls.map((url, index) => <span key={url}><Image /> 이미지 {index + 1}</span>)}{imageUrls.length < 5 && <label className="button button--secondary">＋ 추가<input hidden type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => { const file = event.target.files?.[0]; if (file) uploadMutation.mutate(file); }} /></label>}<small>JPG · PNG · WEBP · 용도별 BE 용량 제한</small></div>
          {message && <p role="alert" className="profile-edit-error">{message}</p>}
          <div className="write-actions"><Button variant="secondary" onClick={() => navigate(isEdit ? `/community/${editPostId}` : '/community')}>취소</Button><Button type="submit" disabled={!hobbyId || title.trim().length < 2 || content.trim().length < 10 || createMutation.isPending || updateMutation.isPending || uploadMutation.isPending}>{createMutation.isPending || updateMutation.isPending ? '저장 중…' : isEdit ? '저장' : '등록'}</Button></div>
        </form>
      </main>
    </div>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('ko-KR', { month: '2-digit', day: '2-digit' }).format(date);
}
