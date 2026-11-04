import { useEffect, useRef } from 'react';
import { ApiError } from '../../../shared/api/client';
import { Button } from '../../../shared/ui/Button';

export function HobbyStartErrorModal({ error, onClose }: { error: unknown; onClose: () => void }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const isLimitError = error instanceof ApiError && error.code === 'MY_HOBBY_LIMIT';
  const message = isLimitError
    ? '동시에 진행할 수 있는 취미는 최대 3개예요. 내 취미에서 기존 취미를 일시중지하거나 그만둔 뒤 다시 시작해 주세요.'
    : error instanceof Error && error.message.trim() ? error.message : '잠시 후 다시 시도해 주세요.';

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

  return (
    <dialog ref={dialogRef} className="hobby-start-error-modal" role="dialog" aria-modal="true" aria-labelledby="hobby-start-error-title" aria-describedby="hobby-start-error-description" onCancel={onClose} data-lenis-prevent>
      <h2 id="hobby-start-error-title">{isLimitError ? '진행 중인 취미가 3개예요' : '취미를 시작하지 못했어요'}</h2>
      <p id="hobby-start-error-description">{message}</p>
      <div>
        {isLimitError && <Button variant="secondary" to="/my-hobbies">내 취미 관리</Button>}
        <Button onClick={onClose} autoFocus>확인</Button>
      </div>
    </dialog>
  );
}
