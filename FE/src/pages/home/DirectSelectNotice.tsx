import { useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * 홈 상단의 추천 받기 버튼들. 바로 이동하지 않고 안내 모달을 먼저 띄운다.
 * 두 경로 모두 끝나면 홈 '맞춤 추천 취미'가 새 결과로 바뀌고, 마이페이지 '내 취미'는 그대로라는 점을 미리 알려 오해를 막는다.
 */
function NoticeButton({ label, variant, title, description, items, confirmLabel, to }: {
  label: string;
  variant: 'primary' | 'secondary';
  title: string;
  description: string;
  items: ReactNode[];
  confirmLabel: string;
  to: string;
}) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [open]);

  const titleId = `notice-${to.replace(/\W/g, '')}-title`;
  return <>
    <button className={`button button--${variant}`} type="button" onClick={() => setOpen(true)}>{label}</button>
    {open && <div className="explore-restart-backdrop" role="presentation" onMouseDown={() => setOpen(false)}>
      <section className="explore-restart-modal direct-select-notice" role="dialog" aria-modal="true" aria-labelledby={titleId} onMouseDown={(event) => event.stopPropagation()}>
        <h2 id={titleId}>{title}</h2>
        <p>{description}</p>
        <ul>{items.map((item, index) => <li key={index}>{item}</li>)}</ul>
        <div>
          <button className="button button--secondary" type="button" onClick={() => setOpen(false)}>취소</button>
          <button className="button button--primary" type="button" onClick={() => { setOpen(false); navigate(to); }}>{confirmLabel}</button>
        </div>
      </section>
    </div>}
  </>;
}

const KEEPS_MY_HOBBIES = "마이페이지의 '내 취미'와 관심 저장한 취미는 바뀌지 않아요.";

/** '숏폼 추천 다시받기'(추천 이력이 없으면 '숏폼 탐색 시작하기') → 맞춤 추천용 숏폼 탐색(/explore) */
export function ExploreAgainButton({ hasRecommendation }: { hasRecommendation: boolean }) {
  return <NoticeButton
    label={hasRecommendation ? '숏폼 추천 다시받기' : '숏폼 탐색 시작하기'}
    variant="primary"
    title={hasRecommendation ? '숏폼으로 추천을 다시 받을까요?' : '숏폼으로 추천을 받아 볼까요?'}
    description="숏폼 12개를 보며 좋아요·별로예요를 남기면 그 반응으로 취미를 추천해 드려요."
    items={[
      hasRecommendation ? '끝까지 보면 홈의 맞춤 추천 취미가 새 결과로 바뀌어요.' : '끝까지 보면 추천 결과를 홈의 맞춤 추천 취미에 보여드려요.',
      '남긴 반응은 다음 추천에도 계속 반영돼요.',
      KEEPS_MY_HOBBIES,
    ]}
    confirmLabel="숏폼 보러 가기"
    to="/explore"
  />;
}

/** '관심사 직접 선택' → 관심사 선택 화면(/hobbies/interests) */
export function DirectSelectButton({ hasRecommendation }: { hasRecommendation: boolean }) {
  return <NoticeButton
    label="관심사 직접 선택"
    variant="secondary"
    title="관심 있는 취미를 직접 고를까요?"
    description="최대 10개를 고르면 그중 지금 이용 상황으로 시작하기 좋은 취미와 비슷한 취미를 추천해 드려요."
    items={[
      hasRecommendation ? '홈의 맞춤 추천 취미가 새로 고른 취미 기준으로 바뀌어요.' : '추천 결과는 홈의 맞춤 추천 취미에 보여드려요.',
      KEEPS_MY_HOBBIES,
    ]}
    confirmLabel="취미 고르러 가기"
    to="/hobbies/interests"
  />;
}
