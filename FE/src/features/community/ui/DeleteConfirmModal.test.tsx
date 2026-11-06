import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { DeleteConfirmModal, type DeleteTarget } from './DeleteConfirmModal';

describe('community deletion confirmation', () => {
  it.each([
    { type: 'post' as const, label: '게시글', content: '클라이밍 입문 후기' },
    { type: 'comment' as const, label: '댓글', content: '좋은 정보 감사합니다.' },
    { type: 'reply' as const, label: '답글', content: '도움이 되었다니 기뻐요.' },
  ])('identifies the $label and its content before deleting', ({ type, label, content }) => {
    const onConfirm = vi.fn(async () => {});
    const onClose = vi.fn();
    const target: DeleteTarget = { type, id: 15, content };
    const html = renderToStaticMarkup(<DeleteConfirmModal target={target} onConfirm={onConfirm} onClose={onClose} />);

    expect(html).toContain('role="dialog"');
    expect(html).toContain('aria-modal="true"');
    expect(html).toContain(`${label}을 삭제합니다.`);
    expect(html).toContain(content);
    expect(html).toContain('>취소</button>');
    expect(html).toContain('>확인</button>');
    expect(onConfirm).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('renders user-written text safely inside the confirmation', () => {
    const html = renderToStaticMarkup(<DeleteConfirmModal
      target={{ type: 'comment', id: 20, content: '<script>alert("test")</script>' }}
      onConfirm={async () => {}}
      onClose={() => {}}
    />);
    expect(html).toContain('&lt;script&gt;');
    expect(html).not.toContain('<script>');
  });
});
