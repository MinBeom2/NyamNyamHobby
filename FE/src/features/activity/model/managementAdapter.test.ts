import { describe, expect, it } from 'vitest';
import { activityView, hobbyListView, planView, positiveInteger, progressView } from './managementAdapter';
import { planEditorSchema, planItemEditSchema } from './planEditorSchema';
import { managementHobbies, managementItems, managementPlan } from '../../../mocks/activityManagementFixtures';

describe('management display and API constraints', () => {
  it('counts skipped work separately from the completion percentage', () => {
    expect(progressView({ total: 16, done: 5, skipped: 1, rate: 5 / 16 })).toMatchObject({ percent: 31, doneWidth: 31.25, skippedWidth: 6.25, label: '16개 중 5개 완료 · 건너뜀 1개' });
    expect(progressView({ total: 0, done: 0, skipped: 0, rate: 0 })).toMatchObject({ percent: 0, doneWidth: 0, skippedWidth: 0 });
  });
  it('uses Korea time and does not invent a missing start date', () => {
    expect(planView(managementPlan()).startedLabel).toBe('2026.10.29');
    expect(planView(managementPlan(2, [], { startedAt: null })).startedLabel).toBeNull();
  });
  it('describes an all-skipped week as processed and keeps hobby state unchanged', () => {
    const items = managementItems().map((item) => ({ ...item, status: 'SKIPPED' as const }));
    const plan = planView(managementPlan(2, items));
    expect(plan.completion).toBe('2주차 활동을 정리했어요. 완료 0개 · 건너뜀 4개');
    expect(plan.planCompletion).toContain('완료 0개 · 건너뜀 16개');
    expect(plan.statusLabel).toBe('진행 중인 취미');
    expect(plan.progress.percent).toBe(0);
  });
  it('keeps server order within each status and separates saved hobbies', () => {
    const hobbies = managementHobbies();
    const view = hobbyListView([hobbies[1], hobbies[2], hobbies[0], { ...hobbies[0], hobbyId: 7 }]);
    expect(view.plans.map((hobby) => hobby.id)).toEqual([1, 7, 2]);
    expect(view.saved.map((hobby) => hobby.id)).toEqual([3]);
    expect(view.plans[0].thumbnail).toBe('/assets/hobbies/candle.webp');
    expect(view.plans[2].thumbnail).toBe('/assets/hobbies/drawing.webp');
    expect(view.saved[0].thumbnail).toBe('/assets/hobbies/pottery.webp');
  });
  it('does not fabricate supplies or completion criteria', () => {
    const view = activityView({ ...managementItems()[0], supplies: null, doneCriteria: null });
    expect(view.suppliesLabel).toBe('준비물 없음');
    expect(view.doneCriteria).toBeNull();
  });
  it.each(['', '0', '-1', '1.5', 'NaN', '9007199254740992'])('rejects an invalid route number %s', (value) => expect(positiveInteger(value)).toBeUndefined());
  it('validates exact creation boundaries and rejects unsupported time values', () => {
    const form = { title: '가'.repeat(50), weekNo: 8, expectedMinutes: 1440, description: '가'.repeat(500), supplies: '가'.repeat(200), doneCriteria: '가'.repeat(200) };
    expect(planEditorSchema(8).safeParse(form).success).toBe(true);
    for (const patch of [{ title: ' ' }, { title: '가'.repeat(51) }, { description: '가'.repeat(501) }, { supplies: '가'.repeat(201) }, { doneCriteria: '가'.repeat(201) }, { weekNo: 9 }, { expectedMinutes: 0 }, { expectedMinutes: 1.5 }, { expectedMinutes: 1441 }]) {
      expect(planEditorSchema(8).safeParse({ ...form, ...patch }).success).toBe(false);
    }
    expect(planItemEditSchema(8).safeParse({ weekNo: 8, expectedMinutes: 1 }).success).toBe(true);
  });
});
