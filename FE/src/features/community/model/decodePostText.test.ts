import { describe, expect, it } from 'vitest';
import type { CommentDto, PostDetailDto } from '../api/communityApi';
import { decodeComment, decodeHtmlEntities, decodePostDetail } from './decodePostText';

describe('decodeHtmlEntities', () => {
  it('restores the five characters the server escapes', () => {
    expect(decodeHtmlEntities('&quot;Hey&quot; &amp; I&#39;m &lt;3 &gt;_&lt;')).toBe('"Hey" & I\'m <3 >_<');
  });

  it('decodes only once, so text the user typed as an entity stays as typed', () => {
    expect(decodeHtmlEntities('&amp;lt;b&amp;gt; &amp;#39;')).toBe('&lt;b&gt; &#39;');
  });

  it('keeps line breaks and unrelated ampersands', () => {
    expect(decodeHtmlEntities('A&B\n\n&nbsp; &#40;')).toBe('A&B\n\n&nbsp; &#40;');
  });
});

describe('community response decoding', () => {
  it('decodes the post title and body', () => {
    const post = { title: 'I&#39;m back', content: '&quot;한 번 더&quot;\n나에게' } as PostDetailDto;
    expect(decodePostDetail(post)).toMatchObject({ title: "I'm back", content: '"한 번 더"\n나에게' });
  });

  it('decodes nested replies and leaves deleted comments null', () => {
    const reply = { content: 'Tom &amp; Jerry', replies: [] } as unknown as CommentDto;
    const comment = { content: null, replies: [reply] } as unknown as CommentDto;
    const decoded = decodeComment(comment);
    expect(decoded.content).toBeNull();
    expect(decoded.replies[0]?.content).toBe('Tom & Jerry');
  });
});
