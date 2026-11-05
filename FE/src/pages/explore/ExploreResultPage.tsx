import { Check, ChevronRight, Image } from 'lucide-react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { useRecommendationResult } from '../../features/recommendation/model/useRecommendationResult';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';
import { RestartExploreControl } from './RestartExploreControl';

/**
 * 탐색 완료 화면(/explore/result/:recommendationId).
 * 결과를 주소로 가지고 있어서 취미 상세로 갔다가 뒤로가기·새로고침해도 이 화면으로 돌아온다.
 * 쇼츠 탐색 훅을 쓰지 않으므로 이 화면을 여는 것만으로 새 탐색 세션이 생기지 않는다.
 */
export function ExploreResultPage() {
  const { recommendationId } = useParams();
  const id = Number(recommendationId);
  if (!Number.isInteger(id) || id <= 0) return <Navigate replace to="/explore" />;
  return <div className="site-shell">
    <AppHeader />
    <main className="page-container explore-page">
      <RecommendationComplete recommendationId={id} />
    </main>
  </div>;
}

function RecommendationComplete({ recommendationId }: { recommendationId: number }) {
  const navigate = useNavigate();
  const resultQuery = useRecommendationResult(recommendationId);
  const result = resultQuery.data;
  return <section className="explore-complete">
    <div className="explore-complete__icon"><Check /></div><img src={characterAssets.celebration} alt="탐색 완료를 축하하는 냠냠취미 캐릭터" />
    <h1>탐색을 마쳤어요!</h1>
    {resultQuery.isPending && <p role="status">추천 결과를 불러오고 있어요.</p>}
    {resultQuery.isError && <p role="alert">추천 결과를 불러오지 못했어요.</p>}
    {result && <>
      <ul className="recommend-result-list" aria-label="추천 취미">
        {result.interests.map((hobby) => <li key={hobby.hobbyId}>
          <Link className="recommend-result" to={`/hobbies/${hobby.hobbyId}`} aria-label={`${hobby.name} 취미 정보 보기`}>
            <span className="recommend-result__thumb">{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <Image aria-hidden="true" />}</span>
            <span className="recommend-result__body"><strong>{hobby.name}</strong><span>{hobby.reason}</span></span>
            <ChevronRight className="recommend-result__arrow" aria-hidden="true" />
          </Link>
        </li>)}
      </ul>
      {result.reasonPending && <p className="recommend-result__pending" role="status">추천 이유를 정리하고 있어요.</p>}
      {result.related.length > 0 && <div className="recommend-related">
        <h2>그 외의 유사한 취미들이에요</h2>
        <div className="explore-complete__chips">{result.related.map((hobby) => <Link key={hobby.hobbyId} to={`/hobbies/${hobby.hobbyId}`}>{hobby.name}</Link>)}</div>
      </div>}
    </>}
    {/* /explore를 새로 열면 탐색 세션을 새로 시작한다(기존 '다시 탐색하기'와 같은 동작) */}
    <div className="explore-complete__actions"><RestartExploreControl onConfirm={() => navigate('/explore')} /><Link className="button button--primary" to="/home">홈으로 가기</Link></div>
  </section>;
}
