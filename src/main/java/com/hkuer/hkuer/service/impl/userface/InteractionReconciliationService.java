package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.TeacherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionReconciliationService {

    private final PostMapper postMapper;
    private final TeacherMapper teacherMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void reconcile() {
        int posts = postMapper.reconcileInteractionCounts();
        int teachers = teacherMapper.reconcileRatingStats();
        if (posts > 0 || teachers > 0) {
            log.warn("互动聚合数据已对账修复，posts: {}, teachers: {}", posts, teachers);
        }
    }
}
