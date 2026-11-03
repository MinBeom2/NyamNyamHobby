import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  deleteUserHobby,
  getOnboardingOptions,
  getUserHobbies,
  saveUserHobby,
  userQueryKeys,
} from '../../features/user/api/userApi';
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
  const hobbiesQuery = useQuery({
    queryKey: userQueryKeys.hobbies(),
    queryFn: () => getUserHobbies(),
  });
  const currentHobbies = hobbiesQuery.data ?? [];
  const lockedIds = useMemo(
    () => new Set(currentHobbies.filter((hobby) => hobby.status !== 'SAVED').map((hobby) => hobby.hobbyId)),
    [currentHobbies],
  );
  const savedIds = useMemo(
    () => currentHobbies.filter((hobby) => hobby.status === 'SAVED').map((hobby) => hobby.hobbyId),
    [currentHobbies],
  );
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
    if (initialized.current || !optionsQuery.data || !hobbiesQuery.data) return;
    setSelectedIds(hobbiesQuery.data.map((hobby) => hobby.hobbyId));
    initialized.current = true;
  }, [optionsQuery.data, hobbiesQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const selected = new Set(selectedIds);
      const previousSaved = new Set(savedIds);
      await Promise.all([
        ...selectedIds.filter((id) => !previousSaved.has(id) && !lockedIds.has(id)).map(saveUserHobby),
        ...savedIds.filter((id) => !selected.has(id)).map(deleteUserHobby),
      ]);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: userQueryKeys.all });
      navigate('/mypage', { replace: true });
    },
  });

  const toggle = (hobbyId: number) => {
    if (lockedIds.has(hobbyId)) return;
    setSelectedIds((current) => {
      if (current.includes(hobbyId)) return current.filter((id) => id !== hobbyId);
      if (current.length >= MAX_INTERESTS) return current;
      return [...current, hobbyId];
    });
  };

  const selectedNames = (optionsQuery.data?.hobbies ?? [])
    .filter((hobby) => selectedIds.includes(hobby.hobbyId))
    .map((hobby) => hobby.name);
  const isLoading = optionsQuery.isLoading || hobbiesQuery.isLoading;
  const isError = optionsQuery.isError || hobbiesQuery.isError;

  return <div className="site-shell">
    <AppHeader />
    <main className="page-container onboarding-page">
      <section className="onboarding-heading">
        <div><h1>관심 취미</h1><p>관심 있는 취미를 최대 5개까지 선택할 수 있어요.</p></div>
      </section>
      {isLoading && <p role="status">관심 취미를 불러오고 있어요.</p>}
      {isError && <p role="alert">관심 취미를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
      {!isLoading && !isError && groupedHobbies.length === 0 && <p>선택할 수 있는 취미가 없어요.</p>}
      {!isLoading && !isError && groupedHobbies.length > 0 && <>
        <div className="choice-panel">
          {groupedHobbies.map((group) => <div className="choice-row" key={group.categoryName}>
            <strong>{group.categoryName}</strong>
            <div>{group.hobbies.map((hobby) => {
              const selected = selectedIds.includes(hobby.hobbyId);
              const locked = lockedIds.has(hobby.hobbyId);
              return <button
                aria-pressed={selected}
                className={selected ? 'selected' : ''}
                disabled={locked}
                key={hobby.hobbyId}
                onClick={() => toggle(hobby.hobbyId)}
                type="button"
                title={locked ? '진행 중인 취미는 여기서 해제할 수 없어요.' : undefined}
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
        {saveMutation.isError && <p role="alert">관심 취미를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
      </>}
    </main>
  </div>;
}
