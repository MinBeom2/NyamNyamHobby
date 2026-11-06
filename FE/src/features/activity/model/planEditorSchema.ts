import { z } from 'zod';

export function planEditorSchema(totalWeeks: number) {
  return z.object({
    weekNo: z.number().int().min(1).max(totalWeeks),
    title: z.string().trim().min(1, '활동 제목을 입력해 주세요.').max(50, '제목은 50자 이하로 입력해 주세요.'),
    description: z.string().max(500, '설명은 500자 이하로 입력해 주세요.'),
    expectedMinutes: z.number().int('시간은 정수로 입력해 주세요.').min(1, '시간은 1분 이상이어야 해요.').max(1440, '시간은 1440분 이하로 입력해 주세요.'),
    supplies: z.string().max(200, '준비물은 200자 이하로 입력해 주세요.'),
    doneCriteria: z.string().max(200, '완료 기준은 200자 이하로 입력해 주세요.'),
  });
}

export function planItemEditSchema(totalWeeks: number) {
  return z.object({ weekNo: z.number().int().min(1).max(totalWeeks), expectedMinutes: z.number().int().min(1).max(1440) });
}
