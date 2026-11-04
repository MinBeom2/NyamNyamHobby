import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { ApiError } from '../../../shared/api/client';
import { HobbyStartErrorModal } from './HobbyStartErrorModal';

function render(error: unknown) {
  return renderToStaticMarkup(<MemoryRouter><HobbyStartErrorModal error={error} onClose={() => undefined} /></MemoryRouter>);
}

describe('hobby start failure dialog', () => {
  it('explains the concurrent limit and links to hobby management', () => {
    const html = render(new ApiError(409, {
      code: 'MY_HOBBY_LIMIT', message: '동시에 진행할 수 있는 취미는 3개입니다.', fields: null, retryAfterSeconds: null,
    }));
    expect(html).toContain('role="dialog"');
    expect(html).toContain('aria-modal="true"');
    expect(html).toContain('진행 중인 취미가 3개예요');
    expect(html).toContain('기존 취미를 일시중지하거나 그만둔 뒤');
    expect(html).toContain('href="/my-hobbies"');
    expect(html).toContain('내 취미 관리');
    expect(html).toContain('확인');
  });

  it('shows other server failures without suggesting the limit was reached', () => {
    const html = render(new ApiError(409, {
      code: 'MY_PLAN_NOT_READY', message: '아직 준비된 계획이 없어요.', fields: null, retryAfterSeconds: null,
    }));
    expect(html).toContain('취미를 시작하지 못했어요');
    expect(html).toContain('아직 준비된 계획이 없어요.');
    expect(html).not.toContain('진행 중인 취미가 3개예요');
    expect(html).not.toContain('내 취미 관리');
  });

  it.each([null, new Error('')])('provides a fallback when the failure has no message (%s)', (error) => {
    expect(render(error)).toContain('잠시 후 다시 시도해 주세요.');
  });
});
