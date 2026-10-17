package com.hkuer.hkuer.service.impl.userface;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.CommentArticleRequest;
import com.hkuer.hkuer.entity.DTO.CommentReplyRequest;
import com.hkuer.hkuer.entity.PO.CommentArticle;
import com.hkuer.hkuer.entity.PO.CommentRelation;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.VO.CommentThreadResponse;
import com.hkuer.hkuer.entity.VO.ReplyResponse;
import com.hkuer.hkuer.entity.VO.UserCommentResponse;
import com.hkuer.hkuer.mapper.userface.CommentArticleMapper;
import com.hkuer.hkuer.mapper.userface.CommentRelationMapper;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.service.userface.CommentService;
import com.hkuer.hkuer.utils.SensitiveWordFilter;
import com.hkuer.hkuer.utils.SnowflakeIdGenerator;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    @org.springframework.beans.factory.annotation.Autowired
    private SnowflakeIdGenerator idGenerator;

    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private MeshNotificationService meshNotifications;
    private final com.hkuer.hkuer.service.userface.MuteService muteService;
    private final CommentArticleMapper commentArticleMapper;
    private final CommentRelationMapper commentRelationMapper;
    private final UserMapper userMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final com.hkuer.hkuer.mapper.userface.PostMapper postMapper;

    private void published(Long id) {
        var post=postMapper.selectById(id);
        if(post==null || !Integer.valueOf(1).equals(post.getStatus()))throw new com.hkuer.hkuer.exception.BusinessException("404","内容不存在或暂不可见");
    }
    private String cleanComment(String text) {
        if(text==null || text.isBlank() || text.length()>5000)throw new com.hkuer.hkuer.exception.BusinessException("400","评论需为 1–5000 字");
        if(muteService.isMuted(UserContext.getCurrentUserId()))throw new com.hkuer.hkuer.exception.BusinessException("403","禁言期间暂不能评论");
        String clean=org.jsoup.Jsoup.clean(text,org.jsoup.safety.Safelist.basic());
        if(org.jsoup.Jsoup.parse(clean).text().isBlank())throw new com.hkuer.hkuer.exception.BusinessException("400","评论不能为空");
        return clean;
    }

    @Value("${sensitive-word.mode:replace}")
    private String sensitiveWordMode;

    @Override
    @Transactional
    public Result addArticleComment(CommentArticleRequest request) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null)
            return Result.error("401", "未登录");

        // 敏感词检测
        published(request.getPostId());
        String content = cleanComment(request.getContentMd());
        if (content != null && sensitiveWordFilter.containsSensitiveWord(content)) {
            if ("reject".equals(sensitiveWordMode)) {
                return Result.error("400", "评论内容包含敏感词，无法发布");
            } else {
                // replace模式：自动替换敏感词
                content = sensitiveWordFilter.replaceSensitiveWords(content);
            }
        }

        SnowflakeIdGenerator idGen = idGenerator;
        Long callId = idGen.nextId();
        CommentArticle ca = new CommentArticle();
        ca.setCallId(callId);
        ca.setPostId(request.getPostId());
        ca.setUserId(userId);
        ca.setContentMd(content);
        ca.setStatus(1);
        ca.setCreatedAt(LocalDateTime.now().withNano(0));
        ca.setUpdatedAt(LocalDateTime.now().withNano(0));
        commentArticleMapper.insert(ca);
        if(meshNotifications!=null)meshNotifications.post(ca.getPostId(),userId,"comment","你的内容有了新评论",content);
        return Result.success(callId);
    }

    @Override
    @Transactional
    public Result addReply(CommentReplyRequest request) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null)
            return Result.error("401", "未登录");

        Long parentId = request.getParentId();
        CommentArticle parentRoot = commentArticleMapper.selectById(parentId);
        CommentRelation parentReply = null;
        boolean parentIsRoot = parentRoot != null;
        if (!parentIsRoot) {
            parentReply = commentRelationMapper.selectById(parentId);
            if (parentReply == null)
                return Result.error("404", "父评论不存在");
        }

        SnowflakeIdGenerator idGen = idGenerator;
        Long reId = idGen.nextId();

        // 敏感词检测
        published(parentIsRoot?parentRoot.getPostId():parentReply.getPostId());
        if(!Integer.valueOf(1).equals(parentIsRoot?parentRoot.getStatus():parentReply.getStatus()))return Result.error("404","父评论不可见");
        String replyContent = cleanComment(request.getContentMd());
        if (replyContent != null && sensitiveWordFilter.containsSensitiveWord(replyContent)) {
            if ("reject".equals(sensitiveWordMode)) {
                return Result.error("400", "回复内容包含敏感词，无法发布");
            } else {
                // replace模式：自动替换敏感词
                replyContent = sensitiveWordFilter.replaceSensitiveWords(replyContent);
            }
        }

        CommentRelation rel = new CommentRelation();
        rel.setReId(reId);
        rel.setReUserId(userId);
        rel.setReComment(replyContent);
        if (parentIsRoot) {
            rel.setCallId(parentRoot.getCallId());
            rel.setPostId(parentRoot.getPostId());
            rel.setParentReId(null);
        } else {
            rel.setCallId(parentReply.getCallId());
            rel.setPostId(parentReply.getPostId());
            rel.setParentReId(parentId);
        }
        rel.setStatus(1);
        rel.setCreatedAt(LocalDateTime.now().withNano(0));
        rel.setUpdatedAt(LocalDateTime.now().withNano(0));
        commentRelationMapper.insert(rel);
        if(meshNotifications!=null)meshNotifications.post(rel.getPostId(),userId,"comment","你的内容有了新回复",replyContent);
        return Result.success(reId);
    }

    @Override
    public Result listByPostId(Long postId) {
        published(postId);
        // 1) 根评论
        List<CommentArticle> roots = commentArticleMapper.selectList(
                new QueryWrapper<CommentArticle>().eq("post_id", postId).eq("status", 1).orderByAsc("created_at"));
        if (roots.isEmpty())
            return Result.success(Collections.emptyList());

        Set<Long> callIds = roots.stream().map(CommentArticle::getCallId).collect(Collectors.toSet());
        List<CommentRelation> relations = callIds.isEmpty() ? Collections.emptyList()
                : commentRelationMapper.selectList(new QueryWrapper<CommentRelation>()
                        .eq("post_id", postId)
                        .in("call_id", callIds)
                        .eq("status", 1)
                        .orderByAsc("created_at"));

        // 4) 计算被回复人
        Set<String> userIds = new HashSet<>();
        Map<Long, String> parentAuthor = new HashMap<>();
        for (CommentArticle ca : roots) {
            parentAuthor.put(ca.getCallId(), ca.getUserId());
            userIds.add(ca.getUserId());
        }
        for (CommentRelation r : relations)
            parentAuthor.put(r.getReId(), r.getReUserId());
        for (CommentRelation r : relations) {
            userIds.add(r.getReUserId());
            String toId = parentAuthor.get(r.getParentReId() == null ? r.getCallId() : r.getParentReId());
            if (toId != null)
                userIds.add(toId);
        }

        Map<String, String> nick = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectList(new QueryWrapper<User>().in("id", userIds));
            for (User u : users)
                nick.put(u.getId(), u.getNickname());
        }
        List<CommentThreadResponse> result = new ArrayList<>();
        Map<Long, CommentThreadResponse> callIdToThread = new LinkedHashMap<>();
        for (CommentArticle ca : roots) {
            CommentThreadResponse thread = new CommentThreadResponse();
            thread.setCallId(ca.getCallId());
            thread.setPostId(ca.getPostId());
            thread.setUserId(ca.getUserId());
            thread.setNickname(nick.getOrDefault(ca.getUserId(), ""));
            thread.setContentMd(ca.getContentMd());
            thread.setCreatedAt(ca.getCreatedAt());
            thread.setReplies(new ArrayList<>());
            callIdToThread.put(ca.getCallId(), thread);
            result.add(thread);
        }

        Map<Long, ReplyResponse> idToReply = new HashMap<>();
        for (CommentRelation r : relations) {
            ReplyResponse rv = new ReplyResponse();
            rv.setId(r.getReId());
            rv.setFromUserId(r.getReUserId());
            rv.setFromNickname(nick.getOrDefault(r.getReUserId(), ""));
            String toId = parentAuthor.get(r.getParentReId() == null ? r.getCallId() : r.getParentReId());
            rv.setToUserId(toId);
            rv.setToNickname(toId == null ? "" : nick.getOrDefault(toId, ""));
            rv.setContentMd(r.getReComment());
            rv.setCreatedAt(r.getCreatedAt());
            rv.setChildren(new ArrayList<>());
            idToReply.put(r.getReId(), rv);
        }

        for (CommentRelation r : relations) {
            ReplyResponse rv = idToReply.get(r.getReId());
            if (r.getParentReId() == null && callIdToThread.containsKey(r.getCallId())) {
                // 父是根
                callIdToThread.get(r.getCallId()).getReplies().add(rv);
            } else {
                ReplyResponse parent = idToReply.get(r.getParentReId());
                if (parent != null) {
                    parent.getChildren().add(rv);
                }
            }
        }

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result deleteComment(Long targetId) {
        // 获取当前用户并检查权限：管理员可删任意评论，普通用户只能删自己的评论
        String currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }
        User currentUser = userMapper.selectById(currentUserId);
        boolean isAdmin = currentUser != null
                && currentUser.getUsertype() != null
                && currentUser.getUsertype() == 1;

        // 先判断是否根评论
        CommentArticle root = commentArticleMapper.selectById(targetId);
        if (root != null) {
            if (!isAdmin) {
                String ownerId = root.getUserId();
                if (ownerId == null || !ownerId.equals(currentUserId)) {
                    return Result.error("403", "只能删除自己发布的评论");
                }
            }

            Long cnt = commentRelationMapper.selectCount(new QueryWrapper<CommentRelation>()
                    .eq("call_id", root.getCallId()));
            if (cnt != null && cnt > 0) {
                // 软删：仅替换正文
                CommentArticle update = new CommentArticle();
                update.setCallId(root.getCallId());
                update.setContentMd("该评论已被删除");
                update.setUpdatedAt(LocalDateTime.now().withNano(0));
                commentArticleMapper.updateById(update);
                return Result.success("soft_deleted");
            } else {
                // 真删
                commentArticleMapper.deleteById(root.getCallId());
                return Result.success("deleted");
            }
        }
        CommentRelation rel = commentRelationMapper.selectById(targetId);
        if (rel == null) {
            return Result.error("404", "评论不存在");
        }
        if (!isAdmin) {
            String ownerId = rel.getReUserId();
            if (ownerId == null || !ownerId.equals(currentUserId)) {
                return Result.error("403", "只能删除自己发布的评论");
            }
        }
        Long cnt = commentRelationMapper.selectCount(new QueryWrapper<CommentRelation>()
                .eq("parent_re_id", rel.getReId()));
        if (cnt != null && cnt > 0) {
            CommentRelation update = new CommentRelation();
            update.setReId(rel.getReId());
            update.setReComment("该评论已被删除");
            update.setUpdatedAt(LocalDateTime.now().withNano(0));
            commentRelationMapper.updateById(update);
            return Result.success("soft_deleted");
        } else {
            commentRelationMapper.deleteById(rel.getReId());
            return Result.success("deleted");
        }
    }

    @Override
    public Result listByUserId(Integer pageNo, Integer pageSize, String keyword) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("401", "请先登录");
        }
        if (pageNo == null || pageNo < 1)
            pageNo = 1;
        if (pageSize == null || pageSize < 1)
            pageSize = 10;

        QueryWrapper<CommentArticle> rootQ = new QueryWrapper<CommentArticle>()
                .eq("user_id", userId)
                .eq("status", 1)
                .ne("content_md", "该评论已被删除");
        if (keyword != null && !keyword.isEmpty()) {
            rootQ.like("content_md", keyword);
        }

        QueryWrapper<CommentRelation> replyQ = new QueryWrapper<CommentRelation>()
                .eq("re_user_id", userId)
                .eq("status", 1)
                .ne("re_comment", "该评论已被删除");
        if (keyword != null && !keyword.isEmpty()) {
            replyQ.like("re_comment", keyword);
        }

        List<CommentArticle> roots = commentArticleMapper.selectList(rootQ);
        List<CommentRelation> replies = commentRelationMapper.selectList(replyQ);

        List<UserCommentResponse> merged = new ArrayList<>();
        for (CommentArticle ca : roots) {
            UserCommentResponse r = new UserCommentResponse();
            r.setPostId(ca.getPostId());
            r.setContent(ca.getContentMd());
            r.setCreatedAt(ca.getCreatedAt());
            merged.add(r);
        }
        for (CommentRelation cr : replies) {
            UserCommentResponse r = new UserCommentResponse();
            r.setPostId(cr.getPostId());
            r.setContent(cr.getReComment());
            r.setCreatedAt(cr.getCreatedAt());
            merged.add(r);
        }

        merged.sort(Comparator.comparing(UserCommentResponse::getCreatedAt).reversed());

        int total = merged.size();
        int from = Math.min((pageNo - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<UserCommentResponse> pageList = merged.subList(from, to);

        Map<String, Object> payload = new HashMap<>();
        payload.put("total", total);
        payload.put("pageNo", pageNo);
        payload.put("pageSize", pageSize);
        payload.put("list", pageList);
        return Result.success(payload);
    }

    @Override
    public Long getCommentCount() {
        // 统计根评论数量
        Long articleCount = commentArticleMapper.selectCount(null);
        // 统计回复评论数量
        Long relationCount = commentRelationMapper.selectCount(null);
        // 返回总数
        return (articleCount != null ? articleCount : 0L) + (relationCount != null ? relationCount : 0L);
    }
}
