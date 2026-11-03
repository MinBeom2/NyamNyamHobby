import { useQuery } from '@tanstack/react-query';
import { getUnreadNotificationCount, notificationQueryKeys } from '../api/notificationApi';

// 헤더 알림 배지용 안 읽은 개수. 로그인했을 때만 부르고, 60초마다·창으로 돌아올 때 다시 가져온다(FR-NOTI-01).
export function useUnreadNotificationCount(enabled: boolean) {
  const query = useQuery({
    queryKey: notificationQueryKeys.unreadCount(),
    queryFn: getUnreadNotificationCount,
    enabled,
    refetchInterval: 60_000,
    refetchOnWindowFocus: true,
  });
  return enabled ? query.data?.unreadCount ?? 0 : 0;
}
