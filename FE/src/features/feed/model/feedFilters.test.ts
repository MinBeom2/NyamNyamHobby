import { describe, expect, it } from 'vitest';
import { dateRangeLabel, presetRange, regionLabel } from './feedFilters';

// 2026-11-04는 수요일
const wednesday = new Date(2026, 10, 4);

describe('presetRange', () => {
  it('builds week, weekend and month ranges from today', () => {
    expect(presetRange('THIS_WEEK', wednesday)).toMatchObject({ from: '2026-11-04', to: '2026-11-08' });
    expect(presetRange('THIS_WEEKEND', wednesday)).toMatchObject({ from: '2026-11-07', to: '2026-11-08' });
    expect(presetRange('THIS_MONTH', wednesday)).toMatchObject({ from: '2026-11-04', to: '2026-11-30' });
    expect(presetRange('NEXT_MONTH', wednesday)).toMatchObject({ from: '2026-12-01', to: '2026-12-31' });
    expect(presetRange('NEXT_30_DAYS', wednesday)).toMatchObject({ from: '2026-11-04', to: '2026-12-03' });
  });

  it('treats a Sunday as the last day of its own week and weekend', () => {
    const sunday = new Date(2026, 10, 8);
    expect(presetRange('THIS_WEEK', sunday)).toMatchObject({ from: '2026-11-08', to: '2026-11-08' });
    expect(presetRange('THIS_WEEKEND', sunday)).toMatchObject({ from: '2026-11-08', to: '2026-11-08' });
  });
});

describe('labels', () => {
  it('shows the preset name or a short custom range', () => {
    expect(dateRangeLabel(presetRange('THIS_MONTH', wednesday))).toBe('이번 달');
    expect(dateRangeLabel({ from: '2026-11-04', to: '2026-11-20' })).toBe('11.4~11.20');
    expect(dateRangeLabel({ from: '2026-11-04' })).toBe('11.4부터');
    expect(dateRangeLabel({})).toBeNull();
  });

  it('summarizes several districts', () => {
    expect(regionLabel([])).toBeNull();
    expect(regionLabel(['중구'])).toBe('중구');
    expect(regionLabel(['중구', '종로구', '용산구'])).toBe('중구 외 2곳');
  });
});
