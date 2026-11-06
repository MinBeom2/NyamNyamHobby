import { useEffect, useRef, type PropsWithChildren } from 'react';
import { X } from 'lucide-react';

export function ManagementDialog({ title, onClose, busy = false, panel = false, children }: PropsWithChildren<{ title: string; onClose: () => void; busy?: boolean; panel?: boolean }>) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const previous = document.activeElement;
    const dialog = ref.current;
    dialog?.showModal();
    return () => {
      dialog?.close();
      if (previous instanceof HTMLElement && previous.isConnected) previous.focus();
    };
  }, []);
  return <dialog ref={ref} className={`hm-dialog ${panel ? 'hm-dialog--panel' : ''}`} aria-label={title} aria-modal="true" onCancel={(event) => { event.preventDefault(); if (!busy) onClose(); }}>
    <header className="hm-dialog__header"><h2>{title}</h2><button type="button" className="hm-icon-button" aria-label={`${title} 닫기`} disabled={busy} onClick={onClose}><X size={20} /></button></header>
    {children}
  </dialog>;
}
