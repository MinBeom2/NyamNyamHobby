import { X } from 'lucide-react';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Button } from '../../../shared/ui/Button';
import { datePresetLabels, presetRange, type DatePreset, type DateRange } from '../model/feedFilters';

type FilterModalProps = {
  title: string;
  description: string;
  onClose: () => void;
  children: ReactNode;
  footer: ReactNode;
};

/** 정보 피드 필터 모달 틀. 바깥을 누르거나 Esc를 누르면 닫힌다(적용 전 선택은 버림) */
function FilterModal({ title, description, onClose, children, footer }: FilterModalProps) {
  const dialogRef = useRef<HTMLElement>(null);
  // 부모가 다시 그려질 때마다 포커스가 튀지 않게 열릴 때 한 번만 포커스를 옮기고, 닫기 함수는 최신 것을 쓴다
  const onCloseRef = useRef(onClose);
  useEffect(() => { onCloseRef.current = onClose; }, [onClose]);
  useEffect(() => {
    dialogRef.current?.focus();
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') onCloseRef.current(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
    <section ref={dialogRef} tabIndex={-1} className="feed-filter-modal" role="dialog" aria-modal="true" aria-labelledby="feed-filter-title" onMouseDown={(event) => event.stopPropagation()}>
      <header>
        <div><h2 id="feed-filter-title">{title}</h2><p>{description}</p></div>
        <button className="feed-filter-modal__close" type="button" aria-label="닫기" onClick={onClose}><X /></button>
      </header>
      {children}
      <footer>{footer}</footer>
    </section>
  </div>;
}

const presets: DatePreset[] = ['THIS_WEEK', 'THIS_WEEKEND', 'THIS_MONTH', 'NEXT_MONTH', 'NEXT_30_DAYS'];

export function DateFilterModal({ value, onApply, onClose }: { value: DateRange; onApply: (range: DateRange) => void; onClose: () => void }) {
  const [draft, setDraft] = useState<DateRange>(value);
  const invalid = Boolean(draft.from && draft.to && draft.from > draft.to);
  const setCustom = (field: 'from' | 'to', date: string) => setDraft((current) => ({ from: current.from, to: current.to, [field]: date || undefined }));

  return <FilterModal
    title="날짜 설정"
    description="선택한 기간에 진행되는 소식만 보여 드려요. 기간이 없는 입문 자료는 빠져요."
    onClose={onClose}
    footer={<>
      <Button variant="secondary" onClick={() => setDraft({})}>초기화</Button>
      <Button disabled={invalid} onClick={() => onApply(draft)}>적용</Button>
    </>}
  >
    <div className="feed-filter-modal__group" role="group" aria-label="빠른 선택">
      {presets.map((preset) => <button
        aria-pressed={draft.preset === preset}
        className={draft.preset === preset ? 'active' : ''}
        key={preset}
        type="button"
        onClick={() => setDraft(presetRange(preset))}
      >{datePresetLabels[preset]}</button>)}
    </div>
    <div className="feed-filter-modal__dates">
      <label>시작일<input type="date" value={draft.from ?? ''} max={draft.to} onChange={(event) => setCustom('from', event.target.value)} /></label>
      <span aria-hidden="true">~</span>
      <label>종료일<input type="date" value={draft.to ?? ''} min={draft.from} onChange={(event) => setCustom('to', event.target.value)} /></label>
    </div>
    {invalid && <p className="feed-filter-modal__error" role="alert">종료일은 시작일보다 같거나 늦어야 해요.</p>}
  </FilterModal>;
}

export type RegionOption = { id: number; name: string };

export function RegionFilterModal({ options, value, loading, error, onApply, onClose }: {
  options: RegionOption[];
  value: number[];
  loading: boolean;
  error: boolean;
  onApply: (ids: number[]) => void;
  onClose: () => void;
}) {
  const [draft, setDraft] = useState<number[]>(value);
  const toggle = (id: number) => setDraft((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);

  return <FilterModal
    title="지역 설정"
    description="서울 자치구를 여러 곳 고를 수 있어요. 지역이 없는 입문 자료와 일부 공연은 빠져요."
    onClose={onClose}
    footer={<>
      <Button variant="secondary" onClick={() => setDraft([])}>선택 해제</Button>
      <Button onClick={() => onApply(draft)}>{draft.length ? `${draft.length}곳 적용` : '전체 지역 적용'}</Button>
    </>}
  >
    {loading && <p role="status">지역을 불러오고 있어요.</p>}
    {error && <p className="feed-filter-modal__error" role="alert">지역을 불러오지 못했어요.</p>}
    <div className="feed-filter-modal__group feed-filter-modal__group--grid" role="group" aria-label="서울 자치구">
      {options.map((option) => <button
        aria-pressed={draft.includes(option.id)}
        className={draft.includes(option.id) ? 'active' : ''}
        key={option.id}
        type="button"
        onClick={() => toggle(option.id)}
      >{option.name}</button>)}
    </div>
  </FilterModal>;
}
