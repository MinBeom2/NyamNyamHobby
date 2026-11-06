import type { CommentDto, PageDto, PostDetailDto, PostSummaryDto } from '../api/communityApi';

// BE는 게시글 제목·본문과 댓글을 HtmlEscaper로 이스케이프해 저장·응답한다(& < > " ' 5종).
// 화면은 React 텍스트 노드로 그리므로 원문으로 되돌려야 하고, 수정 폼에도 원문이 들어가야 재저장 시 이중 이스케이프되지 않는다.
const ENTITY_TO_CHAR: Record<string, string> = {
  '&amp;': '&',
  '&lt;': '<',
  '&gt;': '>',
  '&quot;': '"',
  '&#39;': "'",
};
const ENTITY_PATTERN = /&(?:amp|lt|gt|quot|#39);/g;

/** 한 번만 훑어서 되돌린다. 사용자가 직접 쓴 "&lt;"(저장값 "&amp;lt;")가 "<"로 바뀌지 않게 하기 위함 */
export function decodeHtmlEntities(value: string): string {
  return value.replace(ENTITY_PATTERN, (entity) => ENTITY_TO_CHAR[entity] ?? entity);
}

export function decodePostSummary(post: PostSummaryDto): PostSummaryDto {
  return { ...post, title: decodeHtmlEntities(post.title), contentSummary: decodeHtmlEntities(post.contentSummary) };
}

export function decodePostDetail(post: PostDetailDto): PostDetailDto {
  return { ...post, title: decodeHtmlEntities(post.title), content: decodeHtmlEntities(post.content) };
}

export function decodeComment(comment: CommentDto): CommentDto {
  return {
    ...comment,
    content: comment.content === null ? null : decodeHtmlEntities(comment.content),
    replies: comment.replies.map(decodeComment),
  };
}

export function decodePage<T>(page: PageDto<T>, decodeItem: (item: T) => T): PageDto<T> {
  return { ...page, content: page.content.map(decodeItem) };
}
