import { Children, isValidElement, type ButtonHTMLAttributes } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from './Pagination';
import { readPageNumber, withPageNumber } from '../hooks/pageSearchParams';

describe('pagination', () => {
  it('hides page buttons for an empty result', () => {
    expect(renderToStaticMarkup(<Pagination page={0} totalPages={0} onPageChange={() => undefined} />)).toBe('');
  });

  it.each([[0, 1, '이전 페이지'], [0, 4, '이전 페이지'], [3, 4, '다음 페이지']])(
    'disables the boundary button at page %s of %s', (page, totalPages, label) => {
      const nav = Pagination({ page, totalPages, onPageChange: () => undefined });
      const button = Children.toArray(nav?.props.children).find((child) =>
        isValidElement<ButtonHTMLAttributes<HTMLButtonElement>>(child) && child.props['aria-label'] === label);
      expect(isValidElement<ButtonHTMLAttributes<HTMLButtonElement>>(button) && button.props.disabled).toBe(true);
    },
  );

  it('moves beyond the initial three pages and sends zero-based page numbers', () => {
    const onPageChange = vi.fn();
    const nav = Pagination({ page: 3, totalPages: 5, onPageChange });
    const html = renderToStaticMarkup(nav);
    expect(html).toContain('aria-current="page"');
    expect(html).toContain('aria-label="4페이지"');
    expect(html).toContain('aria-label="5페이지"');
    expect(html).not.toContain('aria-label="6페이지"');
    const button = Children.toArray(nav?.props.children).find((child) =>
      isValidElement<ButtonHTMLAttributes<HTMLButtonElement>>(child) && child.props['aria-label'] === '다음 페이지');
    if (!isValidElement<ButtonHTMLAttributes<HTMLButtonElement>>(button)) throw new Error('다음 페이지 버튼 누락');
    expect(button.props.disabled).toBe(false);
    // onClick은 이벤트 객체를 사용하지 않는다.
    button.props.onClick?.({} as React.MouseEvent<HTMLButtonElement>);
    expect(onPageChange).toHaveBeenCalledWith(4);
  });

  it('disables navigation while the page is loading', () => {
    const nav = Pagination({ page: 1, totalPages: 3, disabled: true, onPageChange: () => undefined });
    expect(Children.toArray(nav?.props.children).every((child) =>
      isValidElement<ButtonHTMLAttributes<HTMLButtonElement>>(child) && child.props.disabled)).toBe(true);
  });
});

describe('page URL state', () => {
  it.each([null, '', '0', '-1', '1.5', 'abc', 'Infinity', '2147483648'])('treats invalid page %s as the first page', (value) => {
    expect(readPageNumber(value)).toBe(0);
  });
  it('converts displayed page numbers to backend page numbers', () => {
    expect(readPageNumber('1')).toBe(0);
    expect(readPageNumber('3')).toBe(2);
  });
  it('preserves filters and unrelated parameters when changing or resetting the page', () => {
    const original = new URLSearchParams('page=3&hobbyId=7&type=REVIEW&keyword=등산');
    const next = withPageNumber(original, 4);
    expect(next.get('page')).toBe('5');
    expect(next.get('keyword')).toBe('등산');
    expect(next.get('hobbyId')).toBe('7');
    expect(withPageNumber(next, 0).has('page')).toBe(false);
    expect(original.get('page')).toBe('3');
    expect(withPageNumber(original, 1, 'commentPage').get('commentPage')).toBe('2');
  });
});
