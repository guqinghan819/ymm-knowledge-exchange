import request from '@/axios'

export function getNotificationList(data) {
  return request({
    url: '/notification/notification/list',
    method: 'post',
    data
  })
}

export function getNotificationUnreadCount() {
  return request({
    url: '/notification/notification/unread-count',
    method: 'get'
  })
}

export function readNotification(notificationId) {
  return request({
    url: '/notification/notification/read',
    method: 'post',
    data: { notificationId }
  })
}

export function readAllNotifications() {
  return request({
    url: '/notification/notification/read-all',
    method: 'post'
  })
}
