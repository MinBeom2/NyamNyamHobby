import { useIsMutating, useMutation, useQuery } from '@tanstack/react-query';
import { activityQueryKeys, createPlanItem, deletePlanItem, getPlan, updatePlanItem, type CreatePlanItemRequest, type UpdatePlanItemRequest } from '../api/activityApi';
import { planView } from '../model/managementAdapter';
import { useRefreshManagement } from './useHobbyManagement';

const mutationKey = (planId: number) => [...activityQueryKeys.all, 'editing', planId];

export function useEditorWeek(planId: number, week: number) {
  const query = useQuery({ queryKey: activityQueryKeys.plan(planId, week), queryFn: () => getPlan(planId, week) });
  return { query, selected: query.data ? planView(query.data) : null, busy: useIsMutating({ mutationKey: mutationKey(planId) }) > 0 };
}

export function useEditPlanItem(planId: number, itemId: number, onSaved: (deleted: boolean) => void) {
  const refresh = useRefreshManagement();
  return useMutation({
    mutationKey: mutationKey(planId),
    mutationFn: async (action: { type: 'save'; request: UpdatePlanItemRequest } | { type: 'delete' }) => {
      if (action.type === 'delete') await deletePlanItem(planId, itemId);
      else await updatePlanItem(planId, itemId, action.request);
    },
    onSuccess: async (_, action) => { await refresh(); onSaved(action.type === 'delete'); },
    onError: () => refresh(),
  });
}

export function useAddPlanItem(planId: number, onSaved: () => void) {
  const refresh = useRefreshManagement();
  return useMutation({ mutationKey: mutationKey(planId), mutationFn: (request: CreatePlanItemRequest) => createPlanItem(planId, request), onSuccess: async () => { await refresh(); onSaved(); }, onError: () => refresh() });
}
