import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getMyInterests, getOnboardingOptions, updateMyInterests, userQueryKeys } from '../../features/user/api/userApi';
import { AppHeader } from '../../shared/ui/AppHeader';

const MAX_INTERESTS = 5;

export function MyInterestsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const initialized = useRef(false);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const optionsQuery = useQuery({
    queryKey: userQueryKeys.onboardingOptions(),
    queryFn: getOnboardingOptions,
  });
  // 내 취미(interest_selections)는 취미 찾기의 관심 저장(user_hobbies)과 별개라 그쪽 목록은 읽지 않는다.
  const interestsQuery = useQuery({
    queryKey: userQueryKeys.interests(),
    queryFn: getMyInterests,
  });
  const groupedHobbies = useMemo(() => {
    const groups = new Map<number, { categoryName: string; hobbies: NonNullable<typeof optionsQuery.data>['hobbies'] }>();
    for (const hobby of optionsQuery.data?.hobbies ?? []) {
      const group = groups.get(hobby.categoryId) ?? { categoryName: hobby.categoryName, hobbies: [] };
      group.hobbies.push(hobby);
      groups.set(hobby.categoryId, group);
    }
    return [...groups.values()];
  }, [optionsQuery.data]);

  useEffect(() => {
    if (initialized.current || !optionsQuery.data || !interestsQuery.data) return;
    setSelectedIds(interestsQuery.data.map((hobby) => hobby.hobbyId));
    initialized.current = true;
  }, [optionsQuery.data, interestsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => updateMyInterests(selectedIds),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: userQueryKeys.all });
      navigate('/mypage', { replace: true });
    },
  });

  const toggle = (hobbyId: number) => {
    setSelectedIds((current) => {
      if (current.includes(hobbyId)) return current.filter((id) => id !== hobbyId);
      if (current.length >= MAX_INTERESTS) return current;
      return [...current, hobbyId];
    });
  };

  const selectedNames = (optionsQuery.data?.hobbies ?? [])
    .filter((hobby) => selectedIds.includes(hobby.hobbyId))
    .map((hobby) => hobby.name);
  const isLoading = optionsQuery.isLoading || interestsQuery.isLoading;
  const isError = optionsQuery.isError || interestsQuery.isError;

  return <div className="site-shell">
    <AppHeader />
    <main className="page-container onboarding-page">
      <section className="onboarding-heading">
        <div><h1>내 취미 선택</h1><p>내가 수행할 취미를 최대 5개까지 선택할 수 있어요.</p></div>
      </section>
      {isLoading && <p role="status">내 취미를 불러오고 있어요.</p>}
      {isError && <p role="alert">내 취미를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
      {!isLoading && !isError && groupedHobbies.length === 0 && <p>선택할 수 있는 취미가 없어요.</p>}
      {!isLoading && !isError && groupedHobbies.length > 0 && <>
        <div className="choice-panel">
          {groupedHobbies.map((group) => <div className="choice-row" key={group.categoryName}>
            <strong>{group.categoryName}</strong>
            <div>{group.hobbies.map((hobby) => {
              const selected = selectedIds.includes(hobby.hobbyId);
              return <button
                aria-pressed={selected}
                className={selected ? 'selected' : ''}
                key={hobby.hobbyId}
                onClick={() => toggle(hobby.hobbyId)}
                type="button"
              >{selected ? '✓ ' : ''}{hobby.name}</button>;
            })}</div>
          </div>)}
          <div className="selection-summary">
            <strong>선택한 취미 {selectedIds.length}개</strong>
            <span>{selectedNames.length > 0 ? selectedNames.join(' · ') : '선택한 취미가 없어요.'}</span>
          </div>
        </div>
        <div className="onboarding-actions">
          <button className="button button--secondary onboarding-cancel" onClick={() => navigate('/mypage')} type="button">취소</button>
          <div className="onboarding-actions__right">
            <button className="button button--primary onboarding-save" disabled={saveMutation.isPending} onClick={() => saveMutation.mutate()} type="button">
              {saveMutation.isPending ? '저장 중' : '저장'}
            </button>
          </div>
        </div>
        {saveMutation.isError && <p role="alert">내 취미를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
      </>}
    </main>
  </div>;
}
