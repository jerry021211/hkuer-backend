package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.mapper.userface.MeshContentMapper;
import com.hkuer.hkuer.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;

/** Persists local community notices in the caller's existing business transaction. */
@Service
@Profile("mesh")
@RequiredArgsConstructor
public class MeshNotificationService {
    private final MeshContentMapper content;
    private final SnowflakeIdGenerator ids;

    public void post(long id, String actor, String kind, String title, String text) {
        var post = content.detail(id, actor, true);
        if (post == null) return;
        String recipient = post.get("authorId").toString();
        if (recipient.equals(actor) && !"system".equals(kind)) return;
        String path = switch (post.get("kind").toString()) {
            case "market" -> "/marketplace?id=" + id;
            case "rental" -> "/rental?id=" + id;
            default -> "/question/" + id;
        };
        String summary = Jsoup.parse(text == null ? post.get("title").toString() : text).text();
        content.notify(ids.nextId(), recipient, actor, kind, title,
                summary.substring(0, Math.min(200, summary.length())), path);
    }
}
