import { z } from 'zod';

export const oauthSignupSchema = z.object({
  nickname: z.string().regex(/^[가-힣a-zA-Z0-9]{2,12}$/, '2~12자의 한글, 영문, 숫자만 사용할 수 있어요.'),
});

export type OAuthSignupFormValues = z.infer<typeof oauthSignupSchema>;
