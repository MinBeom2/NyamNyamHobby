import { z } from 'zod';

export const communityWriteSchema = z.object({
  hobbyId: z.number({ error: '게시판을 선택해 주세요.' }).int().positive('게시판을 선택해 주세요.'),
  title: z.string().trim()
    .min(1, '제목을 입력해 주세요.')
    .min(2, '제목은 2~50자로 입력해 주세요.')
    .max(50, '제목은 2~50자로 입력해 주세요.'),
  content: z.string().trim()
    .min(1, '내용을 입력해 주세요.')
    .min(10, '내용은 10~5,000자로 입력해 주세요.')
    .max(5000, '내용은 10~5,000자로 입력해 주세요.'),
});

export type CommunityWriteFormValues = z.infer<typeof communityWriteSchema>;
