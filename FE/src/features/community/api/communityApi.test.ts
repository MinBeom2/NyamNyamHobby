import { afterEach, describe, expect, it, vi } from 'vitest';
import { communityQueryKeys, getComments, getMyPosts, getPosts } from './communityApi';

afterEach(() => vi.unstubAllGlobals());

describe('community page requests', () => {
  it.each([
    ['전체 게시판', () => getPosts(null, { page: 2, keyword: '후기', type: 'REVIEW' }), '/api/v1/posts', 2],
    ['취미별 게시판', () => getPosts(7, { page: 1, keyword: '후기', type: 'REVIEW' }), '/api/v1/hobbies/7/posts', 1],
    ['내가 쓴 글', () => getMyPosts({ page: 3 }), '/api/v1/users/me/posts', 3],
    ['댓글', () => getComments(12, 1), '/api/v1/posts/12/comments', 1],
  ] as const)('requests the selected backend page for %s', async (_label, request, path, page) => {
    const data = { content: [], page: { number: page, size: 20, totalElements: 101, totalPages: 6, hasNext: true } };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ success: true, data, error: null }), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(request()).resolves.toEqual(data);
    const url = new URL(fetchMock.mock.calls[0][0], 'http://localhost');
    expect(url.pathname).toBe(path);
    expect(url.searchParams.get('page')).toBe(String(page));
    if (path.endsWith('/posts') && !path.includes('/users/')) {
      expect(url.searchParams.get('type')).toBe('REVIEW');
      expect(url.searchParams.get('keyword')).toBe('후기');
    }
  });

  it('can invalidate every comment page without invalidating other posts', () => {
    const prefix = communityQueryKeys.commentPages(12);
    for (const page of [0, 1, 4]) expect(communityQueryKeys.comments(12, page).slice(0, -1)).toEqual(prefix);
    expect(communityQueryKeys.comments(13).slice(0, -1)).not.toEqual(prefix);
  });
});
