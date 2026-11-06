import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { communityQueryKeys, type CommentDto, type PageDto, type PostDetailDto, type PostSummaryDto } from '../features/community/api/communityApi';
import { hobbyQueryKeys, type HobbyListItemDto } from '../features/hobby/api/hobbyApi';
import { userQueryKeys } from '../features/user/api/userApi';
import { CommunityDetailPage, CommunityListPage } from './community/CommunityPages';
import { MyPostsPage } from './mypage/MyPostsPage';
import { HobbyInterestSelectPage, HobbySearchPage } from './hobbies/HobbyPages';

let client: QueryClient;
beforeEach(() => { client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity } } }); });
afterEach(() => client.clear());

function render(element: ReactNode, url: string, path: string) {
  return renderToStaticMarkup(<QueryClientProvider client={client}><MemoryRouter initialEntries={[url]}>
    <Routes><Route path={path} element={element} /></Routes>
  </MemoryRouter></QueryClientProvider>);
}

const author = { id: 1, nickname: '작성자', profileImageUrl: null };
const post: PostSummaryDto = {
  id: 21, hobbyId: 7, hobbyName: '등산', type: 'REVIEW', title: '두 번째 페이지 게시글', contentSummary: '', author,
  thumbnailUrl: null, viewCount: 1, likeCount: 0, commentCount: 0, createdAt: '2026-11-06T00:00:00Z',
};
const posts: PageDto<PostSummaryDto> = {
  content: [post], page: { number: 1, size: 20, totalElements: 41, totalPages: 3, hasNext: true },
};

describe('paginated screens', () => {
  it('restores a filtered community page from its URL and renders that page instead of page zero', () => {
    client.setQueryData(userQueryKeys.onboardingOptions(), { hobbies: [{ hobbyId: 7, name: '등산' }], regions: [] });
    client.setQueryData(communityQueryKeys.posts(7, 'REVIEW', '등산', 1), posts);
    const html = render(<CommunityListPage />, '/community?hobbyId=7&type=REVIEW&keyword=등산&page=2', '/community');
    expect(html).toContain('등산 게시판');
    expect(html).toContain('두 번째 페이지 게시글');
    expect(html).toMatch(/aria-label="2페이지" aria-current="page"/);
    expect(html).not.toContain('검색 결과가 없어요');
  });

  it('uses the URL page for my posts', () => {
    client.setQueryData(communityQueryKeys.myPosts(1), posts);
    const html = render(<MyPostsPage />, '/mypage/posts?page=2', '/mypage/posts');
    expect(html).toContain('두 번째 페이지 게시글');
    expect(html).toMatch(/aria-label="2페이지" aria-current="page"/);
  });

  it('shows the second parent-comment page with replies and keeps the full comment count', () => {
    const detail: PostDetailDto = {
      id: 21, hobby: { id: 7, name: '등산' }, type: 'REVIEW', title: '댓글 테스트', content: '내용', imageUrls: [], author,
      viewCount: 1, likeCount: 0, commentCount: 82, isLiked: false, isMine: false,
      createdAt: '2026-11-06T00:00:00Z', updatedAt: '2026-11-06T00:00:00Z',
    };
    const reply: CommentDto = {
      id: 52, parentId: 51, author, content: '함께 조회된 답글', isDeleted: false, likeCount: 0, isLiked: false, isMine: false,
      createdAt: detail.createdAt, updatedAt: detail.updatedAt, replies: [],
    };
    client.setQueryData(communityQueryKeys.post(21), detail);
    client.setQueryData(communityQueryKeys.comments(21, 1), {
      content: [{ ...reply, id: 51, parentId: null, content: '51번째 부모 댓글', replies: [reply] }],
      page: { number: 1, size: 50, totalElements: 81, totalPages: 2, hasNext: false },
    });
    const html = render(<CommunityDetailPage />, '/community/21?commentPage=2', '/community/:postId');
    expect(html).toContain('51번째 부모 댓글');
    expect(html).toContain('함께 조회된 답글');
    expect(html).toContain('댓글 82');
    expect(html).toContain('aria-label="댓글 페이지 이동"');
    expect(html).toMatch(/aria-label="다음 페이지" disabled=""/);
  });

  it('does not show fake page numbers for an empty community', () => {
    client.setQueryData(communityQueryKeys.posts(null, undefined, ''), {
      content: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false },
    });
    const html = render(<CommunityListPage />, '/community', '/community');
    expect(html).toContain('검색 결과가 없어요');
    expect(html).not.toContain('aria-label="페이지 이동"');
  });
});

describe('hobby catalog screens', () => {
  const hobbies: HobbyListItemDto[] = Array.from({ length: 101 }, (_, index) => ({
    id: index + 1, name: `전체 취미 ${index + 1}`, category: { id: 1, name: '카테고리' }, summary: '소개', thumbnailUrl: null,
    difficulty: 'EASY', placeType: 'INDOOR', socialType: 'ALONE', sessionMinutes: { min: 10, max: 20 },
    monthlyCost: { min: 0, max: 0 }, saveCount: 0, tags: [],
  }));

  it('renders hobbies beyond the first 100 in search results', () => {
    client.setQueryData(hobbyQueryKeys.catalog({}), hobbies);
    const html = render(<HobbySearchPage />, '/hobbies', '/hobbies');
    expect(html).toContain('전체 취미 101');
    expect(html).toContain('href="/hobbies/101"');
  });

  it('makes hobbies beyond the first 100 selectable in direct interest selection', () => {
    client.setQueryData(hobbyQueryKeys.catalog({ sort: 'NAME' }), hobbies);
    const html = render(<HobbyInterestSelectPage />, '/hobbies/interests', '/hobbies/interests');
    expect(html).toMatch(/<button[^>]*aria-pressed="false"[^>]*>전체 취미 101<\/button>/);
  });
});
