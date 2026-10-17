package com.hkuer.hkuer.messaging;

public final class InteractionEventTypes {

    public static final String POST_LIKE_CHANGED = "POST_LIKE_CHANGED";
    public static final String POST_FAVORITE_CHANGED = "POST_FAVORITE_CHANGED";
    public static final String TEACHER_RATING_CHANGED = "TEACHER_RATING_CHANGED";

    private InteractionEventTypes() {
    }

    public static String routingKey(String eventType) {
        return switch (eventType) {
            case POST_LIKE_CHANGED -> "interaction.post.like.changed";
            case POST_FAVORITE_CHANGED -> "interaction.post.favorite.changed";
            case TEACHER_RATING_CHANGED -> "interaction.teacher.rating.changed";
            default -> throw new IllegalArgumentException("未知互动事件类型: " + eventType);
        };
    }
}
