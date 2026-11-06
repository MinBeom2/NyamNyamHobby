import { useBlocker } from 'react-router-dom';
import { useState } from 'react';
import { Button } from '../../../shared/ui/Button';
import { ManagementDialog } from './ManagementDialog';

export function DraftPrompt({ save, discard, cancel }: { save: () => Promise<boolean>; discard: () => void; cancel: () => void }) {
  const [saving, setSaving] = useState(false);
  const [failed, setFailed] = useState(false);
  return <ManagementDialog title="저장하지 않은 메모가 있어요" onClose={cancel} busy={saving}>
    <p>이동하기 전에 작성한 메모를 저장할까요?</p>
    {failed && <p className="hm-error" role="alert">저장하지 못했어요. 작성한 내용은 그대로 있어요. 다시 시도하거나 계속 작성해 주세요.</p>}
    <div className="hm-dialog__actions">
      <Button disabled={saving} onClick={async () => { setSaving(true); const success = await save(); setSaving(false); setFailed(!success); }}>{saving ? '저장 중…' : '저장 후 이동'}</Button>
      <Button variant="secondary" disabled={saving} onClick={discard}>변경 버리기</Button>
      <Button variant="text" disabled={saving} onClick={cancel}>계속 작성</Button>
    </div>
  </ManagementDialog>;
}

// The app uses a data router so links, browser Back/Forward, and programmatic
// navigation all share the same draft protection.
export function RouterDraftGuard({ dirty, saving, saveAll, discard }: { dirty: boolean; saving: boolean; saveAll: () => Promise<boolean>; discard: () => void }) {
  const blocker = useBlocker(dirty || saving);
  if (blocker.state !== 'blocked') return null;
  return <DraftPrompt cancel={() => blocker.reset()} discard={() => { if (!saving) { discard(); blocker.proceed(); } }} save={async () => {
    if (saving || !await saveAll()) return false;
    blocker.proceed();
    return true;
  }} />;
}
