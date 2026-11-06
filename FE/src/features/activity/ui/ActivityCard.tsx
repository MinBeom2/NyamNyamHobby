import { Check, Clock3, Package, Flag, MessageSquare, SkipForward, RotateCcw } from 'lucide-react';
import type { ManagementItem } from '../model/managementAdapter';
import type { useActivityRecords } from '../hooks/useActivityRecords';
import { Button } from '../../../shared/ui/Button';

export function ActivityCard({ item, canRecord, records }: { item: ManagementItem; canRecord: boolean; records: ReturnType<typeof useActivityRecords> }) {
  const id = item.itemId;
  const draft = records.drafts[id];
  const editing = draft !== undefined;
  const tooLong = editing && draft.length > 500;
  const pending = records.pending[id];
  const done = item.status === 'DONE';
  return <article className={`hm-activity hm-activity--${item.status.toLowerCase()}`} aria-labelledby={`hm-item-${id}`}>
    <div className="hm-activity__heading"><label className="hm-check"><input type="checkbox" checked={done} disabled={!canRecord || pending || tooLong} aria-label={`${item.title} 완료`} onChange={() => void records.save(id, { status: done ? 'TODO' : 'DONE' })} /><span>{done && <Check size={16} />}</span></label><h3 id={`hm-item-${id}`}>{item.title}</h3><span className={`hm-badge hm-badge--${item.status.toLowerCase()}`}>{item.statusLabel}</span></div>
    <div className="hm-activity__body">
      {item.description && <p className="hm-activity__description">{item.description}</p>}
      <div className="hm-activity__facts"><span><Clock3 size={14} />예상 {item.duration}</span><span><Package size={14} />준비물 · {item.suppliesLabel}</span>{item.doneCriteria && <span><Flag size={14} />완료 기준 · {item.doneCriteria}</span>}</div>
      {item.memo && !editing && <div className="hm-memo"><MessageSquare size={15} aria-hidden="true" /><p>{item.memo}</p></div>}
      {editing && <div className="hm-memo-editor">
        <label htmlFor={`hm-memo-${id}`}>{item.title} 활동 메모</label>
        <textarea id={`hm-memo-${id}`} value={draft} placeholder="활동하며 느낀 점이나 기억할 점을 남겨 보세요" disabled={pending || !canRecord} aria-invalid={tooLong} aria-describedby={`hm-memo-count-${id}`} onChange={(event) => records.edit(id, event.target.value)} />
        <div className="hm-memo-editor__footer"><span id={`hm-memo-count-${id}`} className={tooLong ? 'hm-error' : ''}>{draft.length}/500{tooLong && ' · 500자 이하로 입력해 주세요.'}</span><div className="hm-actions"><Button disabled={pending || tooLong || draft === (item.memo ?? '') || !canRecord} onClick={() => void records.save(id, { memo: draft === '' ? null : draft })}>{pending ? '저장 중…' : '저장'}</Button><Button variant="secondary" disabled={pending} onClick={() => records.close(id)}>취소</Button></div></div>
      </div>}
      {canRecord && <div className="hm-activity__actions">
        <Button variant={item.status === 'TODO' ? 'primary' : 'secondary'} disabled={pending || tooLong} onClick={() => void records.save(id, { status: done || item.status === 'SKIPPED' ? 'TODO' : 'DONE' })}>
          {done ? <RotateCcw size={14} /> : item.status === 'SKIPPED' ? <RotateCcw size={14} /> : <Check size={14} />}{done ? '완료 취소' : item.status === 'SKIPPED' ? '다시 하기' : '완료'}
        </Button>
        {item.status === 'TODO' && <Button variant="secondary" disabled={pending || tooLong} onClick={() => void records.save(id, { status: 'SKIPPED' })}><SkipForward size={14} />건너뛰기</Button>}
        {!editing && <Button variant="text" disabled={pending} onClick={() => { records.edit(id, item.memo ?? ''); requestAnimationFrame(() => document.getElementById(`hm-memo-${id}`)?.focus()); }}><MessageSquare size={14} />{item.memo ? '메모 수정' : '메모'}</Button>}
        {pending && <span className="hm-muted" role="status">저장 중…</span>}
      </div>}
      {records.errors[id] && <p className="hm-error" role="alert">{records.errors[id]}</p>}
      {records.messages[id] && <p className="hm-save-message" role="status">{records.messages[id]}</p>}
    </div>
  </article>;
}
