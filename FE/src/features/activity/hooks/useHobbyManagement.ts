import { useEffect } from 'react';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { activityQueryKeys, getPlan, getTodoSummary, updateUserHobbyStatus } from '../api/activityApi';
import { deleteUserHobby, getUserHobbies, userQueryKeys } from '../../user/api/userApi';
import { hobbyListView, planLink, planView, positiveInteger } from '../model/managementAdapter';

export function useRefreshManagement() {
  const client = useQueryClient();
  return () => Promise.all([
    client.invalidateQueries({ queryKey: activityQueryKeys.all }),
    client.invalidateQueries({ queryKey: userQueryKeys.all }),
  ]);
}

export function useHobbyManagement() {
  const { hobbyId: rawHobbyId } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const client = useQueryClient();
  const hobbyId = positiveInteger(rawHobbyId ?? null);
  const requestedPlanId = positiveInteger(params.get('planId'));
  const week = positiveInteger(params.get('week'));
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const summaryQuery = useQuery({ queryKey: activityQueryKeys.summary(), queryFn: getTodoSummary });
  const list = hobbyListView(hobbiesQuery.data ?? []);
  const hobby = hobbiesQuery.data?.find((item) => item.hobbyId === hobbyId);
  const planId = requestedPlanId ?? hobby?.planId ?? 0;
  const invalidUrl = (rawHobbyId !== undefined && !hobbyId) || (params.has('planId') && !requestedPlanId)
    || (params.has('week') && !week) || Boolean(hobby && requestedPlanId && hobby.planId !== requestedPlanId);
  const planQuery = useQuery({ queryKey: activityQueryKeys.plan(planId, week), queryFn: () => getPlan(planId, week), enabled: Boolean(hobbyId && planId && !invalidUrl), retry: false });
  const wrongPlan = Boolean(planQuery.data && (planQuery.data.hobbyId !== hobbyId || planQuery.data.planId !== planId));
  const plan = planQuery.data && !invalidUrl && !wrongPlan ? planView(planQuery.data) : null;
  const refresh = useRefreshManagement();
  const statusMutation = useMutation({ mutationFn: (status: 'PAUSED' | 'IN_PROGRESS') => updateUserHobbyStatus(hobbyId!, status), onSuccess: refresh });
  const unsaveMutation = useMutation({ mutationFn: deleteUserHobby, onSuccess: refresh });

  useEffect(() => { statusMutation.reset(); }, [hobbyId]);

  useEffect(() => {
    if (rawHobbyId === undefined && list.plans[0]) navigate(list.plans[0].link, { replace: true });
  }, [rawHobbyId, list.plans[0]?.link, navigate]);

  useEffect(() => {
    if (plan && (!params.has('week') || !params.has('planId') || location.pathname.endsWith('/todos'))) {
      client.setQueryData(activityQueryKeys.plan(plan.id, plan.selectedWeek), planQuery.data);
      navigate(planLink(plan.hobbyId, plan.id, plan.selectedWeek), { replace: true });
    }
  }, [plan?.id, plan?.selectedWeek, params, location.pathname, navigate]);

  return {
    hobbyId, planId, list, plan, hobbiesQuery, planQuery, statusMutation, unsaveMutation,
    invalidPlan: invalidUrl || wrongPlan,
    inProgressCount: summaryQuery.data?.inProgressCount ?? list.activeCount,
    maxInProgressCount: summaryQuery.data?.maxInProgressCount ?? 3,
    goWeek: (number: number) => { if (plan && number !== plan.selectedWeek) navigate(planLink(plan.hobbyId, plan.id, number)); },
  };
}
