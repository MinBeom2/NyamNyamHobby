import { useRef, useState } from 'react';
import { useBeforeUnload } from 'react-router-dom';
import { updatePlanItem, type UpdatePlanItemRequest } from '../api/activityApi';
import { managementError, type ManagementItem } from '../model/managementAdapter';
import { useRefreshManagement } from './useHobbyManagement';

export function useActivityRecords(planId: number, items: ManagementItem[]) {
  const [drafts, setDrafts] = useState<Record<number, string>>({});
  const [pending, setPending] = useState<Record<number, boolean>>({});
  const [errors, setErrors] = useState<Record<number, string>>({});
  const [messages, setMessages] = useState<Record<number, string>>({});
  const busy = useRef(new Set<number>());
  const refresh = useRefreshManagement();
  const dirtyIds = items.filter((item) => drafts[item.itemId] !== undefined && drafts[item.itemId] !== (item.memo ?? '')).map((item) => item.itemId);
  const dirty = dirtyIds.length > 0;
  useBeforeUnload((event) => { if (dirty || busy.current.size) { event.preventDefault(); event.returnValue = ''; } });

  function close(itemId: number) {
    setDrafts((previous) => { const next = { ...previous }; delete next[itemId]; return next; });
    setErrors((previous) => ({ ...previous, [itemId]: '' }));
  }

  async function save(itemId: number, request: UpdatePlanItemRequest) {
    if (busy.current.has(itemId)) return false;
    const draft = drafts[itemId];
    const item = items.find((entry) => entry.itemId === itemId);
    const hasDraft = draft !== undefined && draft !== (item?.memo ?? '');
    if (hasDraft && draft.length > 500) {
      setErrors((previous) => ({ ...previous, [itemId]: '메모는 500자 이하로 입력해 주세요.' }));
      return false;
    }
    const payload = hasDraft ? { ...request, memo: draft === '' ? null : draft } : request;
    busy.current.add(itemId);
    setPending((previous) => ({ ...previous, [itemId]: true }));
    setErrors((previous) => ({ ...previous, [itemId]: '' }));
    setMessages((previous) => ({ ...previous, [itemId]: '' }));
    try {
      await updatePlanItem(planId, itemId, payload);
      await refresh();
      close(itemId);
      setMessages((previous) => ({ ...previous, [itemId]: request.status ? '활동 상태를 저장했어요.' : '메모를 저장했어요.' }));
      return true;
    } catch (error) {
      setErrors((previous) => ({ ...previous, [itemId]: `${managementError(error)} 작성 내용은 유지했어요. 버튼을 눌러 다시 시도해 주세요.` }));
      await refresh();
      return false;
    } finally {
      busy.current.delete(itemId);
      setPending((previous) => ({ ...previous, [itemId]: false }));
    }
  }

  return {
    drafts, pending, errors, messages, dirty, saving: Object.values(pending).some(Boolean),
    edit: (itemId: number, value: string) => setDrafts((previous) => ({ ...previous, [itemId]: value })),
    close, save, discard: () => setDrafts({}),
    saveAll: async () => { for (const id of dirtyIds) { if (!await save(id, { memo: drafts[id] === '' ? null : drafts[id] })) return false; } return true; },
  };
}
