package wcg.yhm.notification.biz.constants;

public interface NotificationConstants {
    int TYPE_FOLLOW = 1;
    int TYPE_NOTE_LIKE = 2;
    int TYPE_NOTE_COLLECT = 3;
    int TYPE_NOTE_DOWNED = 4;
    int STATUS_REVOKED = 0;
    int STATUS_ACTIVE = 1;

    String EVENT_FOLLOW = "FOLLOW";
    String EVENT_UNFOLLOW = "UNFOLLOW";
    String EVENT_NOTE_LIKE = "NOTE_LIKE";
    String EVENT_NOTE_UNLIKE = "NOTE_UNLIKE";
    String EVENT_NOTE_COLLECT = "NOTE_COLLECT";
    String EVENT_NOTE_UNCOLLECT = "NOTE_UNCOLLECT";
    String EVENT_NOTE_DOWNED = "NOTE_DOWNED";
}
