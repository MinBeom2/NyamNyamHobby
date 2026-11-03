import { apiRequest } from '../../../shared/api/client';

// 웹 알림 API(명세 3.9-1·2). 모두 로그인 필요, 본인 알림만 다룬다.
export type NotificationType = 'COMMENT' | 'REPLY' | 'WEEKLY_TODO';

export type NotificationDto = {
  id: number;
  type: NotificationType;
  title: string;
  body: string | null;
  linkPath: string;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
};

export type NotificationPageDto = {
  content: NotificationDto[];
  page: { number: number; size: number; totalElements: number; totalPages: number; hasNext: boolean };
};

export const NOTIFICATION_PAGE_SIZE = 20;

export const notificationQueryKeys = {
  all: ['notifications'] as const,
  list: (unreadOnly: boolean) => ['notifications', 'list', unreadOnly ? 'UNREAD' : 'ALL'] as const,
  unreadCount: () => ['notifications', 'unread-count'] as const,
};

export function getNotifications(unreadOnly: boolean, page = 0) {
  const params = new URLSearchParams({ unreadOnly: String(unreadOnly), page: String(page), size: String(NOTIFICATION_PAGE_SIZE) });
  return apiRequest<NotificationPageDto>(`/notifications?${params}`);
}

export function getUnreadNotificationCount() {
  return apiRequest<{ unreadCount: number }>('/notifications/unread-count');
}

export function readNotification(id: number) {
  return apiRequest<NotificationDto>(`/notifications/${id}/read`, { method: 'PATCH' });
}

export function readAllNotifications() {
  return apiRequest<{ updatedCount: number }>('/notifications/read-all', { method: 'PATCH' });
}
