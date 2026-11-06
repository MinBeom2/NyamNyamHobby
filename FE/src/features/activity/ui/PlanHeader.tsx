import { MapPin, ArrowUpRight, Pencil, Pause, Play } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { ManagementPlan, progressView } from '../model/managementAdapter';
import { Button } from '../../../shared/ui/Button';

export function PlanProgress({ progress }: { progress: ReturnType<typeof progressView> }) {
  return <div className="hm-progress">
    <div className="hm-progress__track" role="progressbar" aria-label="전체 계획 완료율" aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress.percent} aria-valuetext={`${progress.percent}%, ${progress.label}`}>
      <span className="hm-progress__done" style={{ width: `${progress.doneWidth}%` }} />{progress.skippedWidth > 0 && <span className="hm-progress__skipped" style={{ width: `${progress.skippedWidth}%` }} />}
    </div><div className="hm-progress__caption"><span>전체 {progress.label}</span><strong>{progress.percent}%</strong></div>
  </div>;
}

export function PlanHeader({ plan, onEdit, onStatus, busy, error }: { plan: ManagementPlan; onEdit: () => void; onStatus: () => void; busy: boolean; error?: string }) {
  return <header className="hm-plan-header">
    <p className="hm-plan-header__status"><span className={`hm-status-dot ${plan.paused ? 'hm-status-dot--paused' : ''}`} />{plan.statusLabel}{plan.startedLabel && <><span aria-hidden="true">·</span>{plan.startedLabel} 시작</>}</p>
    <h1>{plan.title}</h1>
    <PlanProgress progress={plan.progress} />
    <div className="hm-plan-header__bottom"><div className="hm-actions">
      {plan.canEdit && <Button variant="secondary" onClick={onEdit} disabled={busy}><Pencil size={15} /> 내 계획 수정</Button>}
      <Button variant="secondary" onClick={onStatus} disabled={busy}>{plan.paused ? <Play size={15} /> : <Pause size={15} />}{busy ? '처리 중…' : plan.paused ? '다시 시작' : '일시중지'}</Button>
    </div><div className="hm-aux-links"><Link to={`/hobbies/${plan.hobbyId}`}>취미 정보 보기 <ArrowUpRight size={14} /></Link><Link to={`/places?hobbyId=${plan.hobbyId}`}><MapPin size={14} />장소 찾기</Link></div></div>
    {error && <p role="alert" className="hm-error">{error} 버튼을 눌러 다시 시도해 주세요.</p>}
  </header>;
}
