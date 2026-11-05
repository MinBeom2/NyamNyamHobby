import { apiRequest } from '../../../shared/api/client';
import { decodeComment, decodePage, decodePostDetail, decodePostSummary } from '../model/decodePostText';

export type PostType = 'QUESTION' | 'INFO' | 'REVIEW' | 'SHARE';
export type PostAuthorDto = { id: number; nickname: string; profileImageUrl: string | null };
export type PostSummaryDto = {
  id: number; type: PostType; title: string; contentSummary: string;
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
  posts: (hobbyId: number, type?: PostType, keyword?: string, page = 0) => ['community', 'posts', hobbyId, type ?? 'ALL', keyword ?? '', page] as const,
  post: (postId: number) => ['community', 'post', postId] as const,
  comments: (postId: number, page = 0) => ['community', 'post', postId, 'comments', page] as const,
  myPosts: (page = 0) => ['community', 'my-posts', page] as const,
};

export function getPosts(hobbyId: number, options: { type?: PostType; keyword?: string; page?: number; size?: number } = {}) {
  const params = new URLSearchParams({ sort: 'LATEST', page: String(options.page ?? 0), size: String(options.size ?? 20) });
  if (options.type) params.set('type', options.type);
  if (options.keyword?.trim()) params.set('keyword', options.keyword.trim());
  return apiRequest<PageDto<PostSummaryDto>>(`/hobbies/${hobbyId}/posts?${params}`).then((page) => decodePage(page, decodePostSummary));
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
