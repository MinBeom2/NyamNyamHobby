import { useEffect, useRef, useState } from 'react';
import { Button } from '../../../shared/ui/Button';

export type DeleteTarget = {
  type: 'post' | 'comment' | 'reply';
  id: number;
  content: string;
};

const targetLabels = { post: '게시글', comment: '댓글', reply: '답글' };

export function DeleteConfirmModal({ target, onConfirm, onClose }: {
  target: DeleteTarget;
  onConfirm: () => Promise<unknown>;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const submittingRef = useRef(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const label = targetLabels[target.type];

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialog.showModal();
    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  async function confirm() {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setPending(true);
    setError(null);
    try {
      await onConfirm();
      onClose();
    } catch (reason: unknown) {
      setError(reason instanceof Error && reason.message.trim() ? reason.message : '삭제하지 못했어요. 다시 시도해 주세요.');
    } finally {
      submittingRef.current = false;
      setPending(false);
    }
  }

  return (
    <dialog ref={dialogRef} className="delete-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="delete-confirm-title" aria-describedby="delete-confirm-description delete-confirm-content" aria-busy={pending} onCancel={(event) => {
      event.preventDefault();
      if (!submittingRef.current) onClose();
    }} data-lenis-prevent>
      <h2 id="delete-confirm-title">{label} 삭제</h2>
      <p id="delete-confirm-description">{label}을 삭제합니다.</p>
      <blockquote id="delete-confirm-content">{target.content}</blockquote>
      {error && <p className="delete-confirm-modal__error" role="alert">{error}</p>}
      <div>
        <Button variant="secondary" disabled={pending} onClick={() => { if (!submittingRef.current) onClose(); }} autoFocus>취소</Button>
        <Button disabled={pending} onClick={confirm}>{pending ? '삭제 중…' : '확인'}</Button>
      </div>
    </dialog>
  );
}
