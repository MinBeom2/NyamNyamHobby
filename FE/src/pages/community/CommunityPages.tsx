import { Flag, Heart, Image, Link2, MessageSquareReply, Search, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { AppHeader } from '../../shared/ui/AppHeader';
import { ApiError } from '../../shared/api/client';
import { Button } from '../../shared/ui/Button';
import { Pagination } from '../../shared/ui/Pagination';
import { usePageBounds, usePageSearchParams } from '../../shared/hooks/usePageSearchParams';
import { getOnboardingOptions, userQueryKeys } from '../../features/user/api/userApi';
import { communityWriteSchema, type CommunityWriteFormValues } from '../../features/community/model/communityWriteSchema';
import { DeleteConfirmModal, type DeleteTarget } from '../../features/community/ui/DeleteConfirmModal';
import {
  communityQueryKeys,
  createComment,
  createPost,
  createReport,
  deleteComment,
  deletePost,
  getComments,
  getPost,
  getPosts,
  likeComment,
  likePost,
  unlikeComment,
  unlikePost,
  uploadPostImage,
  updatePost,
  type CommentDto,
  type PageDto,
  type PostType,
  type ReportReason,
  type ReportTargetType,
} from '../../features/community/api/communityApi';

const categories = ['전체글', '후기', '질문', '정보 공유', '모임'] as const;
const categoryType: Record<(typeof categories)[number], PostType | undefined> = { '전체글': undefined, '후기': 'REVIEW', '질문': 'QUESTION', '정보 공유': 'INFO', '모임': 'SHARE' };
const typeLabel: Record<PostType, (typeof categories)[number]> = { REVIEW: '후기', QUESTION: '질문', INFO: '정보 공유', SHARE: '모임' };

export function CommunityListPage() {
  const { page, searchParams, changePage, changeFilters } = usePageSearchParams();
  const category = categories.find((item) => categoryType[item] === searchParams.get('type')) ?? '전체글';
  const keyword = searchParams.get('keyword') ?? '';
  const rawHobbyId = Number(searchParams.get('hobbyId'));
  const hobbyId = Number.isSafeInteger(rawHobbyId) && rawHobbyId > 0 ? rawHobbyId : null;
  const optionsQuery = useQuery({ queryKey: userQueryKeys.onboardingOptions(), queryFn: getOnboardingOptions });
  const postsQuery = useQuery({
    queryKey: communityQueryKeys.posts(hobbyId, categoryType[category], keyword, page),
    queryFn: () => getPosts(hobbyId, { type: categoryType[category], keyword, page }),
  });
  usePageBounds(page, postsQuery.data?.page.totalPages, changePage);
  const posts = postsQuery.data?.content ?? [];
  const hobby = optionsQuery.data?.hobbies.find((item) => item.hobbyId === hobbyId);

  return (
    <div className="site-shell">
      <AppHeader notificationCount={3} />
      <main className="page-container community-page">
        <div className="community-layout">
          <aside className="board-sidebar">
            <h2>취미 게시판</h2>
            {categories.map((item) => (
              <button className={category === item ? 'active' : ''} type="button" onClick={() => changeFilters({ type: categoryType[item] ?? null })} key={item}>
                {item}
                <small>{item === category ? postsQuery.data?.page.totalElements ?? 0 : ''}</small>
              </button>
            ))}
          </aside>

          <section className="board-main">
            <div className="page-title-row">
              <div><h1>{hobby ? `${hobby.name} 게시판` : '전체 게시판'}</h1><p>취미 경험과 질문, 유용한 정보를 나눠 보세요.</p></div>
              <Button to="/community/new">글쓰기</Button>
            </div>
            <div className="board-tools">
              <div>
                <button aria-pressed={hobbyId === null} className={hobbyId === null ? 'active' : ''} onClick={() => changeFilters({ hobbyId: null })} type="button">전체</button>
                {(optionsQuery.data?.hobbies ?? []).map((item) => <button aria-pressed={hobbyId === item.hobbyId} className={hobbyId === item.hobbyId ? 'active' : ''} onClick={() => changeFilters({ hobbyId: String(item.hobbyId) })} type="button" key={item.hobbyId}>{item.name}</button>)}
              </div>
              <label><Search /><input value={keyword} onChange={(event) => changeFilters({ keyword: event.target.value }, true)} placeholder="제목 검색" /></label>
            </div>
            <div className="board-table">
              <div className={`board-table__head${hobbyId === null ? ' board-table__head--all' : ''}`}><span>분류</span><span>제목</span><span>작성자</span><span>작성일</span><span>댓글</span><span>조회</span></div>
              {posts.length ? posts.map((post) => (
                <Link className={`board-row${hobbyId === null ? ' board-row--all' : ''}`} to={`/community/${post.id}`} key={post.id}>
                  <span><b className="badge">{typeLabel[post.type]}</b>{hobbyId === null && <b className="badge badge--soft">{post.hobbyName}</b>}</span>
                  <strong>{post.title} <em>[{post.commentCount}]</em></strong>
                  <span>{post.author.nickname}</span><span>{formatDate(post.createdAt)}</span><span>{post.commentCount}</span><span>{post.viewCount}</span>
                </Link>
              )) : postsQuery.isPending ? <section className="mini-empty" role="status">게시글을 불러오고 있어요.</section>
                : !postsQuery.isError && <section className="mini-empty"><Search /><b>검색 결과가 없어요</b></section>}
            </div>
            {postsQuery.isError && <p role="alert">게시글을 불러오지 못했어요. <Button variant="text" onClick={() => postsQuery.refetch()}>다시 시도</Button></p>}
            <Pagination page={page} totalPages={postsQuery.data?.page.totalPages ?? 0} disabled={postsQuery.isFetching} onPageChange={(nextPage) => changePage(nextPage)} />
          </section>
        </div>
      </main>
    </div>
  );
}

export function CommunityDetailPage() {
  const { postId } = useParams();
  const postNumber = Number(postId);
  const { page: commentPage, changePage: changeCommentPage } = usePageSearchParams('commentPage');
  const queryClient = useQueryClient();
  const postQuery = useQuery({ queryKey: communityQueryKeys.post(postNumber), queryFn: () => getPost(postNumber), enabled: Number.isFinite(postNumber) });
  const commentsQuery = useQuery({ queryKey: communityQueryKeys.comments(postNumber, commentPage), queryFn: () => getComments(postNumber, commentPage), enabled: Number.isFinite(postNumber) });
  usePageBounds(commentPage, commentsQuery.data?.page.totalPages, changeCommentPage);
  const post = postQuery.data;
  const comments = commentsQuery.data?.content ?? [];
  const [comment, setComment] = useState('');
  const [replyTarget, setReplyTarget] = useState<number | null>(null);
  const [reply, setReply] = useState('');
  const [linkCopied, setLinkCopied] = useState(false);
  const [reportTarget, setReportTarget] = useState<ReportModalTarget | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null);

  useEffect(() => { setReplyTarget(null); setReply(''); }, [postNumber, commentPage]);

  const likeMutation = useMutation({
    mutationFn: () => post?.isLiked ? unlikePost(post.id) : likePost(postNumber),
    onSuccess: (result) => queryClient.setQueryData(communityQueryKeys.post(postNumber), (current: typeof post) => current ? { ...current, ...result } : current),
  });
  const deleteMutation = useMutation({ mutationFn: (postId: number) => deletePost(postId), onSuccess: () => navigateToCommunity() });
  const createCommentMutation = useMutation({
    mutationFn: ({ content, parentId }: { content: string; parentId: number | null }) => createComment(postNumber, { content, parentId }),
    onSuccess: async (_created, variables) => {
      await refreshComments();
      if (variables.parentId === null) {
        const updated = queryClient.getQueryData<PageDto<CommentDto>>(communityQueryKeys.comments(postNumber, commentPage));
        if (updated) changeCommentPage(Math.max(0, updated.page.totalPages - 1));
      }
    },
  });
  const commentLikeMutation = useMutation({
    mutationFn: (target: CommentDto) => target.isLiked ? unlikeComment(target.id) : likeComment(target.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: communityQueryKeys.commentPages(postNumber) }),
  });
  const deleteCommentMutation = useMutation({
    mutationFn: (commentId: number) => deleteComment(commentId),
    onSuccess: () => refreshComments(),
  });
  const navigate = useNavigate();
  const navigateToCommunity = () => navigate('/community', { replace: true });

  async function refreshComments() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: communityQueryKeys.commentPages(postNumber) }),
      queryClient.invalidateQueries({ queryKey: communityQueryKeys.post(postNumber), exact: true }),
    ]);
  }

  if (postQuery.isLoading) return <div className="site-shell"><AppHeader /><main className="page-container empty-state"><h1>게시글을 불러오는 중이에요</h1></main></div>;
  if (!post) {
    return <div className="site-shell"><AppHeader /><main className="page-container empty-state"><h1>게시글을 찾을 수 없어요</h1><Button to="/community">목록으로</Button></main></div>;
  }

  function submitComment(event: FormEvent) {
    event.preventDefault();
    if (!comment.trim()) return;
    createCommentMutation.mutate({ content: comment.trim(), parentId: null });
    setComment('');
  }

  function submitReply(event: FormEvent, commentId: number) {
    event.preventDefault();
    if (!reply.trim()) return;
    createCommentMutation.mutate({ content: reply.trim(), parentId: commentId });
    setReply('');
    setReplyTarget(null);
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
      <AppHeader notificationCount={3} />
      <main className="page-container post-page">
        <Link className="back-link" to="/community">커뮤니티 · {post.hobby.name}</Link>
        <article className="post-detail">
          <header>
            <div className="post-detail__heading"><span className="badge">{typeLabel[post.type]}</span><span className="badge badge--soft">{post.hobby.name}</span><h1>{post.title}</h1><p>{post.author.nickname} · {formatDate(post.createdAt)} · 조회 {post.viewCount}</p></div>
            {post.isMine && <div className="post-detail__controls"><button type="button" onClick={() => navigate(`/community/${post.id}/edit`)}>수정</button><button type="button" disabled={deleteMutation.isPending} onClick={() => setDeleteTarget({ type: 'post', id: post.id, content: post.title })}>삭제</button></div>}
          </header>
          {post.imageUrls.length > 0 && (
            <div className="post-images">
              {post.imageUrls.map((url, index) => (
                <div className="post-image" key={`${index}-${url}`}>
                  <img src={url} alt={`게시글 첨부 이미지 ${index + 1}/${post.imageUrls.length}`} loading={index === 0 ? 'eager' : 'lazy'} />
                </div>
              ))}
            </div>
          )}
          <p className="post-content">{post.content}</p>
          <footer className="post-actions">
            <div>
              <button className={post.isLiked ? 'active' : ''} type="button" disabled={likeMutation.isPending} onClick={() => likeMutation.mutate()}><Heart aria-hidden="true" fill={post.isLiked ? 'currentColor' : 'none'} /> 좋아요 {post.likeCount}</button>
              <button type="button" onClick={copyPostLink}><Link2 aria-hidden="true" /> {linkCopied ? '복사했어요' : '링크 복사'}</button>
            </div>
            {!post.isMine && <button type="button" onClick={() => setReportTarget({ type: 'POST', id: post.id, label: '게시글' })}><Flag aria-hidden="true" /> 신고</button>}
          </footer>
        </article>

        <section className="comments" aria-labelledby="comments-title">
          <h2 id="comments-title">댓글 {post.commentCount}</h2>
          <label className="comments__label" htmlFor="comment-input">댓글</label>
          <form onSubmit={submitComment}>
            <input id="comment-input" value={comment} onChange={(event) => setComment(event.target.value)} placeholder="댓글을 입력하세요 (500자)" maxLength={500} />
            <button type="submit" disabled={createCommentMutation.isPending}>등록</button>
          </form>

          {commentsQuery.isPending && <p role="status">댓글을 불러오고 있어요.</p>}
          {commentsQuery.isError && <p role="alert">댓글을 불러오지 못했어요. <Button variant="text" onClick={() => commentsQuery.refetch()}>다시 시도</Button></p>}
          <div className="comment-list">
            {comments.map((item) => (
              <article className={item.isDeleted ? 'comment-item comment-item--deleted' : 'comment-item'} key={item.id}>
                {item.isDeleted ? <p>삭제된 댓글입니다.</p> : (
                  <>
                    <span className="comment-avatar" aria-hidden="true">{item.author?.nickname.slice(0, 1) ?? ''}</span>
                    <div className="comment-item__body">
                      <div className="comment-item__author"><b>{item.author?.nickname}</b><small>{formatDate(item.createdAt)}</small></div>
                      <p>{item.content}</p>
                      <div className="comment-item__actions">
                        <button type="button" onClick={() => { setReplyTarget((current) => current === item.id ? null : item.id); setReply(''); }}><MessageSquareReply aria-hidden="true" /> 답글</button>
                        <button className={item.isLiked ? 'active' : ''} type="button" disabled={commentLikeMutation.isPending} onClick={() => commentLikeMutation.mutate(item)}><Heart aria-hidden="true" fill={item.isLiked ? 'currentColor' : 'none'} /> 좋아요 {item.likeCount}</button>
                        {!item.isMine && <button type="button" onClick={() => setReportTarget({ type: 'COMMENT', id: item.id, label: `${item.author?.nickname ?? '사용자'}님의 댓글` })}><Flag aria-hidden="true" /> 신고</button>}
                        {item.isMine && <button type="button" disabled={deleteCommentMutation.isPending} onClick={() => setDeleteTarget({ type: 'comment', id: item.id, content: item.content ?? '' })}><Trash2 aria-hidden="true" /> 삭제</button>}
                      </div>
                      {replyTarget === item.id && (
                        <form className="reply-form" onSubmit={(event) => submitReply(event, item.id)}>
                          <input value={reply} onChange={(event) => setReply(event.target.value)} placeholder={`${item.author?.nickname ?? '사용자'}님에게 답글 쓰기`} maxLength={500} autoFocus />
                          <button type="submit" disabled={createCommentMutation.isPending}>등록</button>
                        </form>
                      )}
                      {item.replies.map((replyItem) => (
                        <div className="comment-reply" key={replyItem.id}>
                          <span className="comment-avatar" aria-hidden="true">{replyItem.author?.nickname.slice(0, 1) ?? ''}</span>
                          <div>
                            <div className="comment-item__author"><b>{replyItem.author?.nickname}</b><small>{formatDate(replyItem.createdAt)}</small></div>
                            <p>{replyItem.isDeleted ? '삭제된 댓글입니다.' : replyItem.content}</p>
                            {!replyItem.isDeleted && <div className="comment-item__actions">
                              <button className={replyItem.isLiked ? 'active' : ''} type="button" disabled={commentLikeMutation.isPending} onClick={() => commentLikeMutation.mutate(replyItem)}><Heart aria-hidden="true" fill={replyItem.isLiked ? 'currentColor' : 'none'} /> 좋아요 {replyItem.likeCount}</button>
                              {!replyItem.isMine && <button type="button" onClick={() => setReportTarget({ type: 'COMMENT', id: replyItem.id, label: `${replyItem.author?.nickname ?? '사용자'}님의 답글` })}><Flag aria-hidden="true" /> 신고</button>}
                              {replyItem.isMine && <button type="button" disabled={deleteCommentMutation.isPending} onClick={() => setDeleteTarget({ type: 'reply', id: replyItem.id, content: replyItem.content ?? '' })}><Trash2 aria-hidden="true" /> 삭제</button>}
                            </div>}
                          </div>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
          <Pagination page={commentPage} totalPages={commentsQuery.data?.page.totalPages ?? 0} disabled={commentsQuery.isFetching}
            label="댓글 페이지 이동" onPageChange={(nextPage) => { setReplyTarget(null); setReply(''); changeCommentPage(nextPage); }} />
        </section>
        {reportTarget && <ReportModal target={reportTarget} onClose={() => setReportTarget(null)} />}
        {deleteTarget && <DeleteConfirmModal
          target={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onConfirm={() => deleteTarget.type === 'post'
            ? deleteMutation.mutateAsync(deleteTarget.id)
            : deleteCommentMutation.mutateAsync(deleteTarget.id)}
        />}
      </main>
    </div>
  );
}

type ReportModalTarget = { type: ReportTargetType; id: number; label: string };

const REPORT_REASONS: { value: ReportReason; label: string }[] = [
  { value: 'SPAM', label: '스팸·홍보' },
  { value: 'ABUSE', label: '욕설·비방' },
  { value: 'PRIVACY', label: '개인정보 노출' },
  { value: 'ADULT', label: '부적절한 내용' },
  { value: 'ETC', label: '기타' },
];

function reportErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.message.trim()) return error.message;
  return '신고하지 못했어요. 다시 시도해 주세요.';
}

function ReportModal({ target, onClose }: { target: ReportModalTarget; onClose: () => void }) {
  const [reason, setReason] = useState<ReportReason>('SPAM');
  const [detail, setDetail] = useState('');
  const reportMutation = useMutation({
    mutationFn: () => createReport({ targetType: target.type, targetId: target.id, reason, detail: detail.trim() || undefined }),
  });

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="report-modal" role="dialog" aria-modal="true" aria-labelledby="report-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="report-title">{target.label} 신고하기</h2>
        {reportMutation.isSuccess ? (
          <><p>신고가 접수됐어요. 확인 후 필요한 조치를 진행할게요.</p><Button onClick={onClose}>확인</Button></>
        ) : (
          <>
            <p>신고 사유를 선택해 주세요.</p>
            {REPORT_REASONS.map((item) => <label key={item.value}><input checked={reason === item.value} onChange={() => setReason(item.value)} name="report" type="radio" />{item.label}</label>)}
            <textarea value={detail} onChange={(event) => setDetail(event.target.value)} placeholder="상세 사유를 입력해 주세요 (300자)" maxLength={300} />
            {reportMutation.isError && <p className="status-error" role="alert">{reportErrorMessage(reportMutation.error)}</p>}
            <div>
              <Button variant="secondary" onClick={onClose}>취소</Button>
              <Button disabled={reportMutation.isPending} onClick={() => reportMutation.mutate()}>{reportMutation.isPending ? '신고하는 중' : '신고하기'}</Button>
            </div>
          </>
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
  const { register, handleSubmit, watch, setValue, reset, formState: { errors, isSubmitted } } = useForm<CommunityWriteFormValues>({
    resolver: zodResolver(communityWriteSchema),
    defaultValues: { hobbyId: 0, title: '', content: '' },
  });
  const hobbyId = watch('hobbyId');
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
    if (!hobbyId && optionsQuery.data?.hobbies[0]) setValue('hobbyId', optionsQuery.data.hobbies[0].hobbyId, { shouldValidate: isSubmitted });
  }, [hobbyId, isEdit, isSubmitted, optionsQuery.data, setValue]);
  useEffect(() => {
    if (!postQuery.data) return;
    reset({ hobbyId: postQuery.data.hobby.id, title: postQuery.data.title, content: postQuery.data.content });
    setType(postQuery.data.type);
    setImageUrls(postQuery.data.imageUrls);
  }, [postQuery.data, reset]);
  const createMutation = useMutation({
    mutationFn: (values: CommunityWriteFormValues) => createPost({ ...values, type, imageUrls }),
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.post(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.summary() });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.posts() });
      navigate(`/community/${post.id}`);
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const updateMutation = useMutation({
    mutationFn: ({ title, content }: CommunityWriteFormValues) => updatePost(editPostId as number, { type, title, content, imageUrls }),
    onSuccess: (post) => {
      queryClient.setQueryData(communityQueryKeys.post(post.id), post);
      void queryClient.invalidateQueries({ queryKey: communityQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.summary() });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.posts() });
      navigate(`/community/${post.id}`);
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const uploadMutation = useMutation({
    mutationFn: uploadPostImage,
    onSuccess: (result) => {
      setImageUrls((current) => [...current, result.url].slice(0, 5));
      setMessage('');
    },
    onError: (error: Error) => setMessage(error.message),
  });
  const submit = handleSubmit((values) => {
    if (createMutation.isPending || updateMutation.isPending) return;
    setMessage('');
    if (isEdit) updateMutation.mutate(values);
    else createMutation.mutate(values);
  });

  return (
    <div className="site-shell">
      <AppHeader notificationCount={3} />
      <main className="page-container write-page">
        <h1>{isEdit ? '글 수정' : '글쓰기'}</h1>
        <form onSubmit={submit} noValidate>
          <div className="write-grid">
            <label>게시판
              <select disabled={isEdit} aria-invalid={Boolean(errors.hobbyId)} aria-describedby={errors.hobbyId ? 'write-hobby-error' : undefined} {...register('hobbyId', { valueAsNumber: true })}>
                <option value={0}>게시판을 선택해 주세요</option>
                {isEdit && postQuery.data && <option value={postQuery.data.hobby.id}>{postQuery.data.hobby.name}</option>}
                {!isEdit && (optionsQuery.data?.hobbies ?? []).map((item) => <option value={item.hobbyId} key={item.hobbyId}>{item.name}</option>)}
              </select>
              {errors.hobbyId && <span id="write-hobby-error" className="form-field__error" role="alert">{errors.hobbyId.message}</span>}
            </label>
            <label>글 유형<select value={type} onChange={(event) => setType(event.target.value as PostType)}><option value="REVIEW">후기</option><option value="QUESTION">질문</option><option value="INFO">정보 공유</option><option value="SHARE">모임</option></select></label>
          </div>
          <label>제목
            <input placeholder="제목을 입력하세요 (50자)" maxLength={50} aria-invalid={Boolean(errors.title)} aria-describedby={errors.title ? 'write-title-error' : undefined} {...register('title')} />
            {errors.title && <span id="write-title-error" className="form-field__error" role="alert">{errors.title.message}</span>}
          </label>
          <label>내용
            <textarea placeholder="내용을 입력하세요 (10자 이상, 5,000자 이하)" maxLength={5000} aria-invalid={Boolean(errors.content)} aria-describedby={errors.content ? 'write-content-error' : undefined} {...register('content')} />
            {errors.content && <span id="write-content-error" className="form-field__error" role="alert">{errors.content.message}</span>}
          </label>
          <div className="image-upload">{imageUrls.map((url, index) => <span key={url}><Image /> 이미지 {index + 1}</span>)}{imageUrls.length < 5 && <label className="button button--secondary">{uploadMutation.isPending ? '업로드 중…' : '＋ 추가'}<input disabled={uploadMutation.isPending} hidden type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => { const file = event.target.files?.[0]; if (file) uploadMutation.mutate(file); event.target.value = ''; }} /></label>}<small>JPG · PNG · WEBP, 이미지당 5MB 이하</small></div>
          {message && <p role="alert" className="profile-edit-error">{message}</p>}
          <div className="write-actions"><Button variant="secondary" onClick={() => navigate(isEdit ? `/community/${editPostId}` : '/community')}>취소</Button><Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>{createMutation.isPending || updateMutation.isPending ? '저장 중…' : isEdit ? '저장' : '등록'}</Button></div>
        </form>
      </main>
    </div>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('ko-KR', { month: '2-digit', day: '2-digit' }).format(date);
}
