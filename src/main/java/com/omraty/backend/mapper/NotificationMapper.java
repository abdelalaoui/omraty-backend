package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.NotificationResponse;
import com.omraty.backend.entities.Notification;
import java.util.List;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.title(),
                notification.message(),
                notification.read(),
                notification.createdAt());
    }

    public static List<NotificationResponse> toResponseList(List<Notification> notifications) {
        return notifications.stream().map(NotificationMapper::toResponse).toList();
    }
}
