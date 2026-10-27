import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().trim().min(1, '이메일을 입력해 주세요.').email('올바른 이메일 형식이 아니에요.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
