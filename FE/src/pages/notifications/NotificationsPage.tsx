import { Bell, CalendarDays, Flag, Heart, MessageSquareText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useSupportMockStore, type AppNotification } from '../../features/support/model/supportMockStore';
import { AppHeader } from '../../shared/ui/AppHeader';

const notificationIcons: Record<AppNotification['type'], React.ReactNode> = {
  COMMENT: <MessageSquareText />, TODO: <CalendarDays />, INQUIRY_ANSWER: <MessageSquareText />, NOTICE: <Bell />, INTEREST: <Heart />, REPORT: <Flag />,
};

export function NotificationsPage() {
  const navigate = useNavigate();
  const notifications = useSupportMockStore((state) => state.notifications);
  const markRead = useSupportMockStore((state) => state.markNotificationRead);
  const markAll = useSupportMockStore((state) => state.markAllNotificationsRead);
  const unreadCount = notifications.filter((item) => !item.read).length;
  const openNotification = (item: AppNotification) => { markRead(item.id); if (item.inquiryId) navigate(`/support/inquiries/${item.inquiryId}`); };
  return <div className="site-shell"><AppHeader authenticated notificationCount={unreadCount} /><main className="page-container notifications-page">
    <header><div><h1>알림</h1><nav><button className="active" type="button">전체</button><span>안 읽음 {unreadCount}</span></nav></div><button type="button" onClick={markAll}>모두 읽음</button></header>
    <div className="notification-list">{notifications.map((item) => <button key={item.id} type="button" className={item.read ? 'notification-item' : 'notification-item unread'} onClick={() => openNotification(item)}>
      {!item.read && <i /> }<span>{notificationIcons[item.type]}</span><div><strong>{item.title}</strong><p>{item.description}</p><time>{item.createdAt}</time></div>
    </button>)}</div><p className="support-caption">알림은 90일 동안 보관돼요.</p>
  </main></div>;
}
