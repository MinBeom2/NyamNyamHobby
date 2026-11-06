import { useState } from 'react';
import { CalendarDays, MessageSquareText, Newspaper } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getNotifications,
  notificationQueryKeys,
  readAllNotifications,
  readNotification,
  type NotificationDto,
  type NotificationType,
} from '../../features/notification/api/notificationApi';
import { formatNotificationTime } from '../../features/notification/model/formatNotificationTime';
import { useUnreadNotificationCount } from '../../features/notification/model/useUnreadNotificationCount';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';

const notificationIcons: Record<NotificationType, React.ReactNode> = {
  COMMENT: <MessageSquareText />, REPLY: <MessageSquareText />, WEEKLY_TODO: <CalendarDays />, NEW_FEED: <Newspaper />,
};

// 09-1 알림: 전체·안 읽음 탭, 최신순 20개씩 더 보기, 누르면 읽음 처리 후 대상 화면으로 이동, 모두 읽음
export function NotificationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const unreadCount = useUnreadNotificationCount(true);
  const listQuery = useInfiniteQuery({
    queryKey: notificationQueryKeys.list(unreadOnly),
    queryFn: ({ pageParam }) => getNotifications(unreadOnly, pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page.hasNext ? last.page.number + 1 : undefined),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: notificationQueryKeys.all });
  const readMutation = useMutation({ mutationFn: readNotification, onSuccess: refresh });
  const readAllMutation = useMutation({ mutationFn: readAllNotifications, onSuccess: refresh });
  const notifications = listQuery.data?.pages.flatMap((page) => page.content) ?? [];

  const openNotification = (item: NotificationDto) => {
    if (!item.isRead) readMutation.mutate(item.id);
    navigate(item.linkPath);
  };

  return <div className="site-shell"><AppHeader /><main className="page-container notifications-page">
    <header><div><h1>알림</h1><nav>
      <button className={unreadOnly ? '' : 'active'} type="button" onClick={() => setUnreadOnly(false)}>전체</button>
      <button className={unreadOnly ? 'active' : ''} type="button" onClick={() => setUnreadOnly(true)}>안 읽음 {unreadCount}</button>
    </nav></div><button type="button" disabled={unreadCount === 0 || readAllMutation.isPending} onClick={() => readAllMutation.mutate()}>모두 읽음</button></header>
    {listQuery.isLoading && <p className="support-caption">알림을 불러오는 중이에요.</p>}
    {listQuery.isError && <p role="alert" className="support-caption">알림을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
    {listQuery.isSuccess && notifications.length === 0 && <p className="support-caption">새 알림이 없어요</p>}
    <div className="notification-list">{notifications.map((item) => <button key={item.id} type="button" className={item.isRead ? 'notification-item' : 'notification-item unread'} onClick={() => openNotification(item)}>
      {!item.isRead && <i /> }<span>{notificationIcons[item.type] ?? <MessageSquareText />}</span><div><strong>{item.title}</strong>{item.body && <p>{item.body}</p>}<time dateTime={item.createdAt}>{formatNotificationTime(item.createdAt)}</time></div>
    </button>)}</div>
    {listQuery.hasNextPage && <Button variant="secondary" disabled={listQuery.isFetchingNextPage} onClick={() => listQuery.fetchNextPage()}>{listQuery.isFetchingNextPage ? '불러오는 중…' : '더 보기'}</Button>}
    <p className="support-caption">알림은 90일 동안 보관돼요.</p>
  </main></div>;
}
