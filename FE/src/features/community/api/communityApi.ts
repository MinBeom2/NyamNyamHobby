import { apiRequest } from '../../../shared/api/client';
import { decodeComment, decodePage, decodePostDetail, decodePostSummary } from '../model/decodePostText';

export type PostType = 'QUESTION' | 'INFO' | 'REVIEW' | 'SHARE';
export type PostAuthorDto = { id: number; nickname: string; profileImageUrl: string | null };
export type PostSummaryDto = {
  id: number; hobbyId: number; hobbyName: string; type: PostType; title: string; contentSummary: string;
  author: PostAuthorDto; thumbnailUrl: string | null; viewCount: number;
  likeCount: number; commentCount: number; createdAt: string;
};
export type PostDetailDto = {
  id: number;
  hobby: { id: number; name: string };
  type: PostType;
  title: string;
  content: string;
  imageUrls: string[];
  author: PostAuthorDto;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
};
export type PageDto<T> = { content: T[]; page: { number: number; size: number; totalElements: number; totalPages: number; hasNext: boolean } };
export type PostWriteRequest = { hobbyId: number; type: PostType; title: string; content: string; imageUrls: string[] };
export type PostUpdateRequest = Partial<Omit<PostWriteRequest, 'hobbyId'>>;
export type CommentDto = {
  id: number;
  parentId: number | null;
  author: PostAuthorDto | null;
  content: string | null;
  isDeleted: boolean;
  likeCount: number;
  isLiked: boolean;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
  replies: CommentDto[];
};
export type CommentWriteRequest = { content: string; parentId: number | null };
export type CommentLikeResponse = { likeCount: number; isLiked: boolean };
export type ImageUploadResponse = { url: string; width: number | null; height: number | null };

export const communityQueryKeys = {
  all: ['community'] as const,
  posts: (hobbyId: number | null, type?: PostType, keyword?: string, page = 0) => ['community', 'posts', hobbyId ?? 'ALL_HOBBIES', type ?? 'ALL', keyword ?? '', page] as const,
  post: (postId: number) => ['community', 'post', postId] as const,
  commentPages: (postId: number) => ['community', 'post', postId, 'comments'] as const,
  comments: (postId: number, page = 0) => ['community', 'post', postId, 'comments', page] as const,
  myPosts: (page = 0) => ['community', 'my-posts', page] as const,
};

export function getPosts(hobbyId: number | null, options: { type?: PostType; keyword?: string; page?: number; size?: number } = {}) {
  const params = new URLSearchParams({ sort: 'LATEST', page: String(options.page ?? 0), size: String(options.size ?? 20) });
  if (options.type) params.set('type', options.type);
  if (options.keyword?.trim()) params.set('keyword', options.keyword.trim());
  const path = hobbyId === null ? '/posts' : `/hobbies/${hobbyId}/posts`;
  return apiRequest<PageDto<PostSummaryDto>>(`${path}?${params}`).then((page) => decodePage(page, decodePostSummary));
}
export function getMyPosts(options: { page?: number; size?: number } = {}) {
  const params = new URLSearchParams({ page: String(options.page ?? 0), size: String(options.size ?? 20) });
  return apiRequest<PageDto<PostSummaryDto>>(`/users/me/posts?${params}`).then((page) => decodePage(page, decodePostSummary));
}
export function getPost(postId: number) { return apiRequest<PostDetailDto>(`/posts/${postId}`).then(decodePostDetail); }
export function createPost(request: PostWriteRequest) { return apiRequest<PostDetailDto>('/posts', { method: 'POST', body: JSON.stringify(request) }).then(decodePostDetail); }
export function updatePost(postId: number, request: PostUpdateRequest) { return apiRequest<PostDetailDto>(`/posts/${postId}`, { method: 'PATCH', body: JSON.stringify(request) }).then(decodePostDetail); }
export function deletePost(postId: number) { return apiRequest<void>(`/posts/${postId}`, { method: 'DELETE' }); }
export function likePost(postId: number) { return apiRequest<{ likeCount: number; isLiked: boolean }>(`/posts/${postId}/like`, { method: 'PUT' }); }
export function unlikePost(postId: number) { return apiRequest<{ likeCount: number; isLiked: boolean }>(`/posts/${postId}/like`, { method: 'DELETE' }); }
export function getComments(postId: number, page = 0) {
  return apiRequest<PageDto<CommentDto>>(`/posts/${postId}/comments?page=${page}`).then((response) => decodePage(response, decodeComment));
}
export function createComment(postId: number, request: CommentWriteRequest) {
  return apiRequest<CommentDto>(`/posts/${postId}/comments`, { method: 'POST', body: JSON.stringify(request) }).then(decodeComment);
}
export function updateComment(commentId: number, content: string) {
  return apiRequest<CommentDto>(`/comments/${commentId}`, { method: 'PATCH', body: JSON.stringify({ content }) }).then(decodeComment);
}
export function deleteComment(commentId: number) {
  return apiRequest<void>(`/comments/${commentId}`, { method: 'DELETE' });
}
export function likeComment(commentId: number) {
  return apiRequest<CommentLikeResponse>(`/comments/${commentId}/like`, { method: 'PUT' });
}
export function unlikeComment(commentId: number) {
  return apiRequest<CommentLikeResponse>(`/comments/${commentId}/like`, { method: 'DELETE' });
}
export function uploadPostImage(file: File) {
  const body = new FormData();
  body.append('file', file);
  return apiRequest<ImageUploadResponse>('/uploads/images?purpose=POST', { method: 'POST', body });
}

export type ReportTargetType = 'POST' | 'COMMENT';
// 명세(3.8)의 reason은 SPAM_OR_AD·INAPPROPRIATE·OFFENSIVE·ETC지만, 팀 결정(11/09)으로 백엔드 구현값을 따른다.
// 백엔드 V1 reports.reason과 신고 화면 선택지 5개가 1:1이다(스팸·홍보, 욕설·비방, 개인정보 노출, 부적절한 내용, 기타). 명세 갱신 필요.
export type ReportReason = 'SPAM' | 'ABUSE' | 'PRIVACY' | 'ADULT' | 'ETC';
export type ReportCreateRequest = { targetType: ReportTargetType; targetId: number; reason: ReportReason; detail?: string };

export function createReport(request: ReportCreateRequest) {
  return apiRequest<{ reportId: number }>('/reports', { method: 'POST', body: JSON.stringify(request) });
}
