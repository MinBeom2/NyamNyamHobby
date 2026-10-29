import { RotateCcw } from 'lucide-react';
import { useEffect, useState } from 'react';

export function RestartExploreControl({ onConfirm }: { onConfirm: () => void }) {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [open]);

  const confirm = () => {
    setOpen(false);
    onConfirm();
  };

  return <>
    <button className="button button--secondary" type="button" onClick={() => setOpen(true)}><RotateCcw size={16} />다시 탐색하기</button>
    {open && <div className="explore-restart-backdrop" role="presentation" onMouseDown={() => setOpen(false)}>
      <section className="explore-restart-modal" role="dialog" aria-modal="true" aria-labelledby="explore-restart-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="explore-restart-title">탐색 기록을 초기화할까요?</h2>
        <p>지금까지 남긴 반응이 지워지고 처음부터 다시 시작해요. 보관함은 그대로 남아요.</p>
        <div>
          <button className="button button--secondary" type="button" onClick={() => setOpen(false)}>취소</button>
          <button className="button button--primary" type="button" onClick={confirm}>초기화</button>
        </div>
      </section>
    </div>}
  </>;
}
