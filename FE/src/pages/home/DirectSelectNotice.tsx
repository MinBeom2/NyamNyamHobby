import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * 홈 '관심사 직접 선택' 버튼. 바로 이동하지 않고 먼저 안내 모달을 띄운다.
 * 직접 고르면 홈 '맞춤 추천 취미'가 새 결과로 바뀌고, 마이페이지 '내 취미'는 그대로라는 점을 미리 알려 오해를 막는다.
 */
export function DirectSelectButton({ hasRecommendation }: { hasRecommendation: boolean }) {
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

  return <>
    <button className="button button--secondary" type="button" onClick={() => setOpen(true)}>관심사 직접 선택</button>
    {open && <div className="explore-restart-backdrop" role="presentation" onMouseDown={() => setOpen(false)}>
      <section className="explore-restart-modal direct-select-notice" role="dialog" aria-modal="true" aria-labelledby="direct-select-notice-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="direct-select-notice-title">관심 있는 취미를 직접 고를까요?</h2>
        <p>최대 10개를 고르면 그중 지금 이용 상황으로 시작하기 좋은 취미와 비슷한 취미를 추천해 드려요.</p>
        <ul>
          <li>{hasRecommendation ? '홈의 맞춤 추천 취미가 새로 고른 취미 기준으로 바뀌어요.' : '추천 결과는 홈의 맞춤 추천 취미에 보여드려요.'}</li>
          <li>마이페이지의 '내 취미'와 관심 저장한 취미는 바뀌지 않아요.</li>
        </ul>
        <div>
          <button className="button button--secondary" type="button" onClick={() => setOpen(false)}>취소</button>
          <button className="button button--primary" type="button" onClick={() => { setOpen(false); navigate('/hobbies/interests'); }}>취미 고르러 가기</button>
        </div>
      </section>
    </div>}
  </>;
}
