import { useEffect, useRef } from 'react';
import type { ManagementPlan } from '../model/managementAdapter';

export function WeekTabs({ plan, onSelect }: { plan: ManagementPlan; onSelect: (week: number) => void }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const container = ref.current;
    const selected = container?.querySelector<HTMLButtonElement>('[aria-selected="true"]');
    // Move only the tab strip; scrollIntoView also moves the entire page.
    if (container && selected) container.scrollLeft = selected.offsetLeft - container.offsetLeft - 8;
  }, [plan.selectedWeek]);
  return <div ref={ref} className="hm-week-tabs" role="tablist" aria-label="계획 주차" onKeyDown={(event) => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const tabs = Array.from(ref.current?.querySelectorAll<HTMLButtonElement>('[role="tab"]') ?? []);
    const index = tabs.indexOf(event.target as HTMLButtonElement);
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
    tabs[next]?.focus();
  }}>
    {plan.weeks.map((week) => <button key={week.number} type="button" role="tab" id={`hm-tab-${plan.id}-${week.number}`} aria-controls={`hm-week-${plan.id}`} aria-selected={week.number === plan.selectedWeek} tabIndex={week.number === plan.selectedWeek ? 0 : -1} onClick={() => onSelect(week.number)}>
      {week.number}주차 {week.current && <span>이번 주</span>}{week.overdue > 0 && <span className="hm-overdue-dot" aria-label={`미완료 ${week.overdue}개`} />}
    </button>)}
  </div>;
}
