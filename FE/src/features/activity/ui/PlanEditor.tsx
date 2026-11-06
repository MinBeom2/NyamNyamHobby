import { useCallback, useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { managementError, type ManagementItem, type ManagementPlan } from '../model/managementAdapter';
import { planEditorSchema, planItemEditSchema } from '../model/planEditorSchema';
import { useAddPlanItem, useEditPlanItem, useEditorWeek } from '../hooks/usePlanEditor';
import { Button } from '../../../shared/ui/Button';
import { ManagementDialog } from './ManagementDialog';

function WeekSelect({ total, value, onChange, disabled }: { total: number; value: number; onChange: (value: number) => void; disabled?: boolean }) {
  return <select value={value} onChange={(event) => onChange(Number(event.target.value))} disabled={disabled}>{Array.from({ length: total }, (_, index) => <option key={index + 1} value={index + 1}>{index + 1}주차</option>)}</select>;
}

function EditItem({ item, plan, onSuccess, onDirty }: { item: ManagementItem; plan: ManagementPlan; onSuccess: (message: string) => void; onDirty: (id: string, dirty: boolean) => void }) {
  const [weekNo, setWeekNo] = useState(item.weekNo);
  const [minutes, setMinutes] = useState(String(item.expectedMinutes));
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [validation, setValidation] = useState('');
  const mutation = useEditPlanItem(plan.id, item.itemId, (deleted) => { setConfirmDelete(false); onSuccess(deleted ? '활동을 삭제했어요.' : '활동을 저장했어요.'); });
  const changed = weekNo !== item.weekNo || Number(minutes) !== item.expectedMinutes;
  useEffect(() => { onDirty(String(item.itemId), changed); return () => onDirty(String(item.itemId), false); }, [item.itemId, changed, onDirty]);
  return <article className="hm-edit-item">
    <div className="hm-edit-item__title"><h3>{item.title}</h3><span className={`hm-badge hm-badge--${item.status.toLowerCase()}`}>{item.statusLabel}</span></div>
    {item.description && <p className="hm-muted">{item.description}</p>}
    <p className="hm-muted">준비물 · {item.suppliesLabel}{item.doneCriteria && <><br />완료 기준 · {item.doneCriteria}</>}</p>
    <div className="hm-editor-row"><label>이동할 주차<WeekSelect total={plan.totalWeeks} value={weekNo} onChange={setWeekNo} disabled={mutation.isPending} /></label><label>예상 시간 (분)<input type="number" min={1} max={1440} step={1} value={minutes} disabled={mutation.isPending} onChange={(event) => setMinutes(event.target.value)} /></label></div>
    <div className="hm-actions"><Button disabled={!changed || mutation.isPending || !plan.canEdit} onClick={() => {
      const result = planItemEditSchema(plan.totalWeeks).safeParse({ weekNo, expectedMinutes: Number(minutes) });
      if (!result.success) { setValidation('주차와 시간을 확인해 주세요. 시간은 1~1440 사이 정수로 입력해 주세요.'); return; }
      setValidation(''); mutation.mutate({ type: 'save', request: { ...(weekNo !== item.weekNo ? { weekNo } : {}), ...(Number(minutes) !== item.expectedMinutes ? { expectedMinutes: Number(minutes) } : {}) } });
    }}>{mutation.isPending ? '처리 중…' : '변경 저장'}</Button><Button variant="text" disabled={mutation.isPending || !plan.canEdit} onClick={() => setConfirmDelete(true)}><Trash2 size={14} />삭제</Button></div>
    {validation && <p className="hm-error" role="alert">{validation}</p>}
    {mutation.isError && <p className="hm-error" role="alert">{managementError(mutation.error)} 다시 시도해 주세요.</p>}
    {confirmDelete && <ManagementDialog title="활동을 삭제할까요?" busy={mutation.isPending} onClose={() => setConfirmDelete(false)}><p>‘{item.title}’ 활동과 기록이 계획에서 삭제돼요. 삭제한 활동은 복구할 수 없어요.</p><div className="hm-dialog__actions"><Button disabled={mutation.isPending} onClick={() => mutation.mutate({ type: 'delete' })}>삭제</Button><Button variant="secondary" disabled={mutation.isPending} onClick={() => setConfirmDelete(false)}>취소</Button></div></ManagementDialog>}
  </article>;
}

function AddItemForm({ plan, week, onSuccess, onCancel, onDirty }: { plan: ManagementPlan; week: number; onSuccess: () => void; onCancel: () => void; onDirty: (id: string, dirty: boolean) => void }) {
  const [form, setForm] = useState({ weekNo: week, title: '', description: '', expectedMinutes: '30', supplies: '', doneCriteria: '' });
  const [error, setError] = useState('');
  const mutation = useAddPlanItem(plan.id, onSuccess);
  const changed = Boolean(form.title || form.description || form.supplies || form.doneCriteria || form.weekNo !== week || form.expectedMinutes !== '30');
  useEffect(() => { onDirty('new', changed); return () => onDirty('new', false); }, [changed, onDirty]);
  const field = (name: 'title' | 'description' | 'supplies' | 'doneCriteria' | 'expectedMinutes', value: string) => setForm((current) => ({ ...current, [name]: value }));
  return <form className="hm-add-form" onSubmit={(event) => {
    event.preventDefault();
    const result = planEditorSchema(plan.totalWeeks).safeParse({ ...form, expectedMinutes: Number(form.expectedMinutes) });
    if (!result.success) { setError(result.error.issues[0].message); return; }
    setError(''); mutation.mutate({ ...result.data, description: result.data.description || null, supplies: result.data.supplies || null, doneCriteria: result.data.doneCriteria || null });
  }}>
    <h3>새 활동 추가</h3><fieldset disabled={mutation.isPending || !plan.canEdit}>
      <div className="hm-editor-row"><label>주차<WeekSelect total={plan.totalWeeks} value={form.weekNo} onChange={(weekNo) => setForm((current) => ({ ...current, weekNo }))} /></label><label>예상 시간 (분)<input type="number" min={1} max={1440} step={1} required value={form.expectedMinutes} onChange={(event) => field('expectedMinutes', event.target.value)} /></label></div>
      <label>활동 제목 <span>필수 · 최대 50자</span><input required maxLength={50} value={form.title} onChange={(event) => field('title', event.target.value)} placeholder="어떤 활동을 해볼까요?" /></label>
      <label>설명 <span>최대 500자</span><textarea maxLength={500} value={form.description} onChange={(event) => field('description', event.target.value)} /></label>
      <label>준비물 <span>최대 200자</span><input maxLength={200} value={form.supplies} onChange={(event) => field('supplies', event.target.value)} /></label>
      <label>완료 기준 <span>최대 200자</span><input maxLength={200} value={form.doneCriteria} onChange={(event) => field('doneCriteria', event.target.value)} /></label>
      <div className="hm-actions"><Button type="submit">{mutation.isPending ? '추가 중…' : '활동 추가'}</Button><Button variant="secondary" onClick={onCancel}>취소</Button></div>
    </fieldset>{(error || mutation.isError) && <p role="alert" className="hm-error">{error || managementError(mutation.error)}</p>}
  </form>;
}

export function PlanEditor({ plan, onClose }: { plan: ManagementPlan; onClose: () => void }) {
  const [week, setWeek] = useState(plan.currentWeek);
  const [adding, setAdding] = useState(false);
  const [message, setMessage] = useState('');
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);
  const [drafts, setDrafts] = useState<Record<string, boolean>>({});
  const onDirty = useCallback((id: string, dirty: boolean) => setDrafts((previous) => previous[id] === dirty ? previous : { ...previous, [id]: dirty }), []);
  const { query, selected, busy } = useEditorWeek(plan.id, week);
  const guard = (action: () => void) => { if (busy) return; if (Object.values(drafts).some(Boolean)) setPendingAction(() => action); else action(); };
  return <ManagementDialog title="내 계획 수정" panel busy={busy} onClose={() => guard(onClose)}>
    <p className="hm-panel-intro">내 속도에 맞게 활동을 조정해 보세요.</p><p className="hm-notice">변경은 활동별로 저장돼요. 저장·삭제한 내용은 패널을 닫아도 유지돼요.</p>
    <label className="hm-editor-week">편집할 주차<WeekSelect total={plan.totalWeeks} value={week} disabled={busy} onChange={(next) => guard(() => { setWeek(next); setAdding(false); setMessage(''); })} /></label>
    <p className="hm-muted">기존 활동은 주차와 예상 시간을 바꿀 수 있어요. 이동한 활동은 해당 주차의 마지막에 추가돼요.</p>
    {message && <p role="status" className="hm-save-message">{message}</p>}{query.isPending && <p role="status">활동을 불러오고 있어요.</p>}
    {query.isError && <div role="alert"><p>{managementError(query.error)}</p><Button variant="secondary" onClick={() => void query.refetch()}>다시 시도</Button></div>}
    {selected && !selected.canEdit && <p role="status" className="hm-notice">현재 상태에서는 계획을 수정할 수 없어요.</p>}
    {selected?.items.map((item) => <EditItem key={`${item.itemId}-${item.weekNo}-${item.expectedMinutes}`} item={item} plan={selected} onSuccess={setMessage} onDirty={onDirty} />)}
    {selected && !selected.items.length && <p>이 주차에는 등록된 활동이 없어요.</p>}
    {selected?.canEdit && (adding ? <AddItemForm key={week} plan={selected} week={week} onDirty={onDirty} onCancel={() => guard(() => setAdding(false))} onSuccess={() => { setAdding(false); setMessage('활동을 추가했어요.'); }} /> : <Button variant="secondary" disabled={busy} onClick={() => setAdding(true)}><Plus size={16} /> 활동 추가</Button>)}
    {pendingAction && <ManagementDialog title="저장하지 않은 입력을 버릴까요?" onClose={() => setPendingAction(null)}><p>아직 저장하지 않은 입력은 사라져요. 이미 저장한 변경은 유지돼요.</p><div className="hm-dialog__actions"><Button onClick={() => { pendingAction(); setPendingAction(null); }}>변경 버리기</Button><Button variant="secondary" onClick={() => setPendingAction(null)}>계속 편집</Button></div></ManagementDialog>}
  </ManagementDialog>;
}
