import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { CommunityWritePage } from './CommunityPages';

describe('community write form', () => {
  it.each([
    ['/community/new', '/community/new', '등록'],
    ['/community/1/edit', '/community/:postId/edit', '저장'],
  ])('allows submitting incomplete input at %s so validation can explain the errors', (url, path, label) => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    try {
      const html = renderToStaticMarkup(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[url]}>
            <Routes><Route path={path} element={<CommunityWritePage />} /></Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );
      const button = html.match(/<button\b[^>]*type="submit"[^>]*>.*?<\/button>/)?.[0];
      expect(button).toContain(label);
      expect(button).not.toContain('disabled');
      expect(html).not.toContain('role="alert"');
    } finally {
      client.clear();
    }
  });
});
