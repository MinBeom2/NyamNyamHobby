import { describe, expect, it } from 'vitest';
import { communityWriteSchema } from './communityWriteSchema';

const valid = { hobbyId: 3, title: '취미 후기', content: '처음 시작한 취미가 정말 재미있어요.' };

describe('community write validation', () => {
  it('explains all missing fields, including whitespace-only input', () => {
    const result = communityWriteSchema.safeParse({ hobbyId: 0, title: '   ', content: '\n  ' });
    expect(result.success).toBe(false);
    if (result.success) return;
    expect(result.error.flatten().fieldErrors).toMatchObject({
      hobbyId: ['게시판을 선택해 주세요.'],
      title: expect.arrayContaining(['제목을 입력해 주세요.']),
      content: expect.arrayContaining(['내용을 입력해 주세요.']),
    });
  });

  it.each([1, 51])('rejects a title of %i characters with the length requirement', (length) => {
    const result = communityWriteSchema.safeParse({ ...valid, title: '가'.repeat(length) });
    expect(result.success).toBe(false);
    if (result.success) return;
    expect(result.error.flatten().fieldErrors.title).toContain('제목은 2~50자로 입력해 주세요.');
  });

  it.each([9, 5001])('rejects content of %i characters with the length requirement', (length) => {
    const result = communityWriteSchema.safeParse({ ...valid, content: '가'.repeat(length) });
    expect(result.success).toBe(false);
    if (result.success) return;
    expect(result.error.flatten().fieldErrors.content).toContain('내용은 10~5,000자로 입력해 주세요.');
  });

  it.each([[2, 10], [50, 5000]])('accepts the title/content boundaries %i/%i', (titleLength, contentLength) => {
    expect(communityWriteSchema.safeParse({ ...valid, title: '가'.repeat(titleLength), content: '나'.repeat(contentLength) }).success).toBe(true);
  });

  it('validates trimmed lengths and returns trimmed text for submission', () => {
    expect(communityWriteSchema.safeParse({ ...valid, title: ' 가 ', content: ' 123456789 ' }).success).toBe(false);
    expect(communityWriteSchema.parse({ ...valid, title: ` ${valid.title} `, content: `\n${valid.content} ` })).toEqual(valid);
  });
});
