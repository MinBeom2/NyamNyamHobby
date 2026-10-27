import { z } from 'zod';

const passwordRule = /^(?:(?=.*[A-Za-z])(?=.*\d)|(?=.*[A-Za-z])(?=.*[^A-Za-z0-9])|(?=.*\d)(?=.*[^A-Za-z0-9])).{8,20}$/;

export const signupSchema = z.object({
  email: z.email('올바른 이메일 주소를 입력해 주세요.').max(100),
  nickname: z.string().regex(/^[가-힣a-zA-Z0-9]{2,12}$/, '2~12자의 한글, 영문, 숫자만 사용할 수 있어요.'),
  password: z.string().regex(passwordRule, '8~20자, 영문·숫자·특수문자 중 2종 이상을 조합해 주세요.'),
  passwordConfirm: z.string(),
  terms: z.boolean().refine(Boolean, '서비스 이용약관에 동의해 주세요.'),
  privacy: z.boolean().refine(Boolean, '개인정보 수집·이용에 동의해 주세요.'),
}).refine((value) => value.password === value.passwordConfirm, {
  path: ['passwordConfirm'],
  message: '비밀번호가 일치하지 않아요.',
});

export type SignupFormValues = z.infer<typeof signupSchema>;

export const passwordResetSchema = z.object({
  newPassword: z.string().regex(passwordRule, '8~20자, 영문·숫자·특수문자 중 2종 이상을 조합해 주세요.'),
  passwordConfirm: z.string(),
}).refine((value) => value.newPassword === value.passwordConfirm, {
  path: ['passwordConfirm'],
  message: '비밀번호가 일치하지 않아요.',
});

export type PasswordResetFormValues = z.infer<typeof passwordResetSchema>;
