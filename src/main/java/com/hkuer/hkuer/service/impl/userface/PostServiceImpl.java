package com.hkuer.hkuer.service.impl.userface;

import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.PostInsertRequest;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.entity.PO.PostContent;
import com.hkuer.hkuer.entity.PO.Board;
import com.hkuer.hkuer.entity.PO.Like;
import com.hkuer.hkuer.entity.PO.Favorite;
import com.hkuer.hkuer.entity.PO.User;
import com.hkuer.hkuer.entity.PO.CommentArticle;
import com.hkuer.hkuer.entity.PO.CommentRelation;
import com.hkuer.hkuer.entity.VO.PostResponse;
import com.hkuer.hkuer.entity.VO.LikedPostResponse;
import com.hkuer.hkuer.entity.VO.FavoritedPostResponse;
import com.hkuer.hkuer.mapper.userface.PostContentMapper;
import com.hkuer.hkuer.mapper.userface.PostMapper;
import com.hkuer.hkuer.mapper.userface.BoardMapper;
import com.hkuer.hkuer.mapper.userface.LikeMapper;
import com.hkuer.hkuer.mapper.userface.FavoriteMapper;
import com.hkuer.hkuer.mapper.userface.UserMapper;
import com.hkuer.hkuer.mapper.userface.CommentArticleMapper;
import com.hkuer.hkuer.mapper.userface.CommentRelationMapper;
import com.hkuer.hkuer.messaging.InteractionCacheService;
import com.hkuer.hkuer.service.userface.AdSensitiveWordService;
import com.hkuer.hkuer.service.userface.AttachmentService;
import com.hkuer.hkuer.service.userface.ContentSecurityService;
import com.hkuer.hkuer.service.userface.PostService;
import com.hkuer.hkuer.service.userface.ViewCountService;
import com.hkuer.hkuer.utils.SnowflakeIdGenerator;
import com.hkuer.hkuer.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    @org.springframework.beans.factory.annotation.Autowired
    private SnowflakeIdGenerator idGenerator;

    private final PostMapper postMapper;
    private final PostContentMapper postContentMapper;
    private final BoardMapper boardMapper;
    private final LikeMapper likeMapper;
    private final FavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final CommentArticleMapper commentArticleMapper;
    private final CommentRelationMapper commentRelationMapper;
    private final ViewCountService viewCountService;
    private final InteractionCacheService interactionCacheService;
    private final AttachmentService attachmentService;
    private final ContentSecurityService contentSecurityService;
    private final AdSensitiveWordService adSensitiveWordService;

    /**
     * 构建用于 XSS 清洗的白名单。
     * 基于 Safelist.relaxed()（允许常见格式标签），额外配置：
     * - 允许 img 标签的 src / alt / width / height / style 属性
     * - 允许 a 标签的 href / target 属性
     * - 严格禁止 script / iframe / object / embed / form 等危险标签
     */
    private static final Safelist SAFE_LIST = Safelist.relaxed()
            .preserveRelativeLinks(true)
            .addAttributes("img", "src", "alt", "width", "height", "style")
            .addAttributes("a", "href", "target")
            .addAttributes(":all", "class", "style")
            .addProtocols("img", "src", "http", "https")
            .removeTags("script", "iframe", "object", "embed", "form", "input");

    @Override
    @Transactional
    public Result insertPost(PostInsertRequest request) {
        String userId = UserContext.getCurrentUserId();
        String title = request.getTitle();
        String content = request.getContent();

        // ========== 第一步：XSS 安全清洗 ==========
        if (content != null) {
            content = Jsoup.clean(content, "https://content.invalid", SAFE_LIST,
                    new Document.OutputSettings().prettyPrint(false));
            log.info("帖子内容已完成 XSS 清洗，清洗后长度: {}", content.length());
        }

        // ========== 第二步：内容安全检测（阿里云 Green 2022） ==========
        // 提取纯文本进行检测（去除 HTML 标签）
        String plainText = content != null ? Jsoup.parse(content).text() : "";
        String fullText = (title != null ? title : "") + " " + plainText;
        if (!fullText.trim().isEmpty()) {
            contentSecurityService.checkText(fullText);
        }

        // ========== 第三步：广告敏感词检测 ==========
        // 检测标题和内容是否包含广告敏感词
        boolean containsAdSensitiveWord = false;
        if (adSensitiveWordService.containsAdSensitiveWord(fullText)) {
            containsAdSensitiveWord = true;
            Set<String> matchedWords = adSensitiveWordService.getAdSensitiveWords(fullText);
            log.warn("检测到广告敏感词，匹配的敏感词: {}", matchedWords);
        }

        // ========== 第四步：从清洗后的 HTML 中提取图片 URL ==========
        List<String> imageUrls = extractImageUrls(content);

        // ========== 构建帖子并保存 ==========

        Long id = idGenerator.nextId();
        Post post = new Post();
        post.setId(id);
        Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                .select("id")
                .eq("name", request.getBoardName()));
        if (board == null) {
            return Result.error("404", "板块不存在: " + request.getBoardName());
        }
        post.setBoardId(board.getId());
        post.setTitle(title);
        // 如果检测到广告敏感词，状态设为3（异常/申诉中），否则设为1（正常发布）
        post.setStatus(containsAdSensitiveWord ? 3 : 1);
        post.setAuthorId(userId);
        post.setCreatedAt(LocalDateTime.now().withNano(0));
        post.setPublishedAt(LocalDateTime.now().withNano(0));
        post.setLikeNum(0L);
        post.setFavNum(0L);

        postMapper.insert(post);

        // 保存清洗后的安全 HTML（永远不存原始 HTML）
        PostContent postContent = new PostContent();
        postContent.setPostId(id);
        postContent.setContentMd(content);
        postContentMapper.insert(postContent);

        // ========== 第五步：图片状态流转，PENDING(0) -> ACTIVE(1) ==========
        if (!imageUrls.isEmpty()) {
            attachmentService.activateByUrls(imageUrls);
            log.info("帖子 {} 引用了 {} 张图片，已转为正式状态", id, imageUrls.size());
        }

        // ========== 第六步：返回结果 ==========
        // 如果检测到广告敏感词，返回code=3000，但包含postId供前端申诉使用
        if (containsAdSensitiveWord) {
            log.info("帖子 {} 因包含广告敏感词被拦截，状态设为3（申诉中），返回code=3000", id);
            return Result.error("3000", "检测到广告敏感词，帖子已保存但需要审核，您可以进行申诉", id);
        }

        return Result.success(post.getId());
    }

    @Override
    @Transactional
    public Result updatePost(Long postId, PostInsertRequest request) {
        Post post = postMapper.selectByIdForUpdate(postId);
        if (post == null) return Result.error("404", "帖子不存在");
        String user = UserContext.getCurrentUserId();
        if (user == null || !user.equals(post.getAuthorId())) return Result.error("403", "只能编辑自己发布的内容");
        if (!Integer.valueOf(1).equals(post.getStatus()) && !Integer.valueOf(3).equals(post.getStatus()))
            return Result.error("403", "已下架的内容不能通过编辑重新发布");
        String content = Jsoup.clean(java.util.Objects.toString(request.getContent(), ""),
                "https://content.invalid", SAFE_LIST, new Document.OutputSettings().prettyPrint(false));
        String fullText = request.getTitle() + " " + Jsoup.parse(content).text();
        contentSecurityService.checkText(fullText);
        // Editing must not bypass a pending moderation/payment decision.
        boolean pending = Integer.valueOf(3).equals(post.getStatus()) || adSensitiveWordService.containsAdSensitiveWord(fullText);
        postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, postId)
                .set(Post::getTitle, request.getTitle()).set(Post::getStatus, pending ? 3 : 1));
        postContentMapper.update(null, new LambdaUpdateWrapper<PostContent>().eq(PostContent::getPostId, postId)
                .set(PostContent::getContentMd, content));
        attachmentService.activateByUrls(extractImageUrls(content));
        return pending ? Result.error("3000", "修改已保存，内容仍需审核", postId) : Result.success(postId);
    }

    /**
     * 从 HTML 内容中提取所有 <img> 标签的 src 属性
     *
     * @param html 清洗后的安全 HTML
     * @return 图片 URL 列表
     */
    private List<String> extractImageUrls(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null || html.trim().isEmpty()) {
            return urls;
        }
        Document doc = Jsoup.parse(html);
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            if (src != null && !src.trim().isEmpty()) {
                urls.add(src);
            }
        }
        return urls;
    }

    @Override
    @Transactional
    public Result deletePost(Long postId) {
        // 检查post是否存在
        Post post = postMapper.selectByIdForUpdate(postId);
        if (post == null) {
            return Result.error("404", "帖子不存在");
        }

        // 获取当前用户并检查权限：管理员可删除任何帖子，普通用户只能删除自己的帖子
        String currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            return Result.error("401", "未登录");
        }
        User currentUser = userMapper.selectById(currentUserId);
        boolean isAdmin = currentUser != null
                && currentUser.getUsertype() != null
                && currentUser.getUsertype() == 1;
        if (!isAdmin) {
            String authorId = post.getAuthorId();
            if (authorId == null || !authorId.equals(currentUserId)) {
                return Result.error("403", "只能删除自己发布的帖子");
            }
        }

        // 1. 删除帖子内容
        postContentMapper.delete(new QueryWrapper<PostContent>()
                .eq("post_id", postId));

        // 1'. 删除该帖子下的所有评论（硬删除）
        // 注意：必须先删除子表 comment_relation，再删除父表 comment_article
        // 因为 comment_relation.call_id 外键引用了 comment_article.call_id
        // 删除所有回复评论（comment_relation），包括多级回复
        commentRelationMapper.delete(new QueryWrapper<CommentRelation>()
                .eq("post_id", postId));
        // 删除所有根评论（comment_article）
        commentArticleMapper.delete(new QueryWrapper<CommentArticle>()
                .eq("post_id", postId));

        // 2. 删除MySQL中的点赞记录
        likeMapper.delete(new QueryWrapper<Like>()
                .eq("target_id", postId));

        // 2'. 删除MySQL中的收藏记录
        favoriteMapper.delete(new QueryWrapper<Favorite>()
                .eq("target_id", postId));

        // 4. 最后删除post记录
        int result = postMapper.deleteById(postId);

        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error("500", "删除失败");
        }
    }

    @Override
    public Result getPostPage(Integer current, Integer size, String boardName, String title, Long postId) {
        // 创建分页对象
        Page<Post> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();

        // 如果指定了postId，直接按ID查询
        if (postId != null) {
            queryWrapper.eq("id", postId);
            Post requested = postMapper.selectById(postId);
            if (requested == null || !canRead(requested)) return Result.error("404", "帖子不存在或暂不可见");
        } else {
            // 如果指定了板块名称，需要先查询板块ID
            if (boardName != null && !boardName.trim().isEmpty()) {
                Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                        .select("id")
                        .eq("name", boardName));
                if (board == null) {
                    return Result.error("404", "板块不存在: " + boardName);
                }
                queryWrapper.eq("board_id", board.getId());
            }

            // 如果指定了标题，进行模糊查询
            if (title != null && !title.trim().isEmpty()) {
                queryWrapper.like("title", title.trim());
            }

            // 只查询已发布的帖子
            queryWrapper.eq("status", 1);

            // 排除置顶帖子，只返回普通帖子（is_top=0 或 is_top IS NULL）
            queryWrapper.and(wrapper -> wrapper.eq("is_top", 0).or().isNull("is_top"));
        }

        // 按发布时间倒序排列
        queryWrapper.orderByDesc("published_at");

        // 执行分页查询
        Page<Post> resultPage = postMapper.selectPage(page, queryWrapper);

        // 转换为PostResponse对象
        Page<PostResponse> responsePage = new Page<>(current, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setPages(resultPage.getPages());
        responsePage.setCurrent(resultPage.getCurrent());
        responsePage.setSize(resultPage.getSize());

        // 获取当前用户ID（用于判断是否点赞）
        String currentUserId = UserContext.getCurrentUserId();

        // 组装PostResponse数据
        List<PostResponse> postResponses = resultPage.getRecords().stream().map(post -> {
            PostResponse response = new PostResponse();
            response.setId(post.getId());
            response.setTitle(post.getTitle());
            response.setStatus(post.getStatus());
            response.setCreatedAt(post.getCreatedAt());
            response.setPublishedAt(post.getPublishedAt());
            response.setLikedNum(post.getLikeNum() == null ? 0L : post.getLikeNum());
            response.setFavoritedNum(post.getFavNum() == null ? 0L : post.getFavNum());
            response.setViewCount(post.getViewCount() == null ? 0L : post.getViewCount());

            // 查询帖子内容
            PostContent postContent = postContentMapper.selectOne(new QueryWrapper<PostContent>()
                    .eq("post_id", post.getId()));
            if (postContent != null) {
                response.setContent(postContent.getContentMd());
            }

            // 查询板块名称
            Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                    .select("name")
                    .eq("id", post.getBoardId()));
            if (board != null) {
                response.setBoardName(board.getName());
            }

            // 查询作者昵称和头像
            if (post.getAuthorId() != null && !post.getAuthorId().isEmpty()) {
                User author = userMapper.selectById(post.getAuthorId());
                if (author != null) {
                    response.setAuthorNickname(author.getNickname());
                    response.setAuthorAvatar(author.getAvatar());
                }
            }

            // 判断当前用户是否已点赞
            if (currentUserId != null) {
                response.setIsLiked(interactionCacheService.isLiked(currentUserId, post.getId()));
            } else {
                // 未登录用户，默认为未点赞
                response.setIsLiked(false);
            }

            // 判断当前用户是否已收藏
            if (currentUserId != null) {
                response.setIsFavorited(interactionCacheService.isFavorited(currentUserId, post.getId()));
            } else {
                // 未登录用户，默认为未收藏
                response.setIsFavorited(false);
            }

            return response;
        }).collect(java.util.stream.Collectors.toList());

        responsePage.setRecords(postResponses);

        return Result.success(responsePage);
    }

    @Override
    public Result getLikedPostPage(Integer current, Integer size, String keyword) {
        // 从UserContext获取当前登录用户ID
        String targetUserId = UserContext.getCurrentUserId();
        if (targetUserId == null || targetUserId.isEmpty()) {
            return Result.error("401", "未登录");
        }

        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<LikedPostResponse> page = new Page<>(current, size);
        return Result.success(likeMapper.selectLikedPostPage(page, targetUserId, normalizedKeyword));
    }

    @Override
    public Result getLatestPosts() {
        // 查询最新的10条已发布帖子
        Page<Post> page = new Page<>(1, 10);
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 只查询已发布的帖子
        queryWrapper.orderByDesc("published_at"); // 按发布时间倒序
        Page<Post> resultPage = postMapper.selectPage(page, queryWrapper);

        // 获取当前用户ID（用于判断是否点赞）
        String currentUserId = UserContext.getCurrentUserId();

        // 组装PostResponse数据
        List<PostResponse> postResponses = resultPage.getRecords().stream().map(post -> {
            PostResponse response = new PostResponse();
            response.setId(post.getId());
            response.setTitle(post.getTitle());
            response.setStatus(post.getStatus());
            response.setCreatedAt(post.getCreatedAt());
            response.setPublishedAt(post.getPublishedAt());
            response.setLikedNum(post.getLikeNum() == null ? 0L : post.getLikeNum());
            response.setFavoritedNum(post.getFavNum() == null ? 0L : post.getFavNum());
            response.setViewCount(post.getViewCount() == null ? 0L : post.getViewCount());

            // 查询帖子内容
            PostContent postContent = postContentMapper.selectOne(new QueryWrapper<PostContent>()
                    .eq("post_id", post.getId()));
            if (postContent != null) {
                response.setContent(postContent.getContentMd());
            }

            // 查询板块名称
            Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                    .select("name")
                    .eq("id", post.getBoardId()));
            if (board != null) {
                response.setBoardName(board.getName());
            }

            // 查询作者昵称和头像
            if (post.getAuthorId() != null && !post.getAuthorId().isEmpty()) {
                User author = userMapper.selectById(post.getAuthorId());
                if (author != null) {
                    response.setAuthorNickname(author.getNickname());
                    response.setAuthorAvatar(author.getAvatar());
                }
            }

            // 判断当前用户是否已点赞
            if (currentUserId != null) {
                response.setIsLiked(interactionCacheService.isLiked(currentUserId, post.getId()));
            } else {
                // 未登录用户，默认为未点赞
                response.setIsLiked(false);
            }

            // 判断当前用户是否已收藏
            if (currentUserId != null) {
                response.setIsFavorited(interactionCacheService.isFavorited(currentUserId, post.getId()));
            } else {
                // 未登录用户，默认为未收藏
                response.setIsFavorited(false);
            }

            return response;
        }).collect(java.util.stream.Collectors.toList());

        return Result.success(postResponses);
    }

    @Override
    public Result getMyPosts(Integer current, Integer size, String title) {
        // 获取当前登录用户ID
        String currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            return Result.error("401", "未登录");
        }

        // 创建分页对象
        Page<Post> page = new Page<>(current, size);

        // 构建查询条件：查询当前用户发布的帖子
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("author_id", currentUserId);

        // 如果指定了标题，进行模糊查询
        if (title != null && !title.trim().isEmpty()) {
            queryWrapper.like("title", title.trim());
        }

        // 按发布时间倒序排列
        queryWrapper.orderByDesc("published_at");

        // 执行分页查询
        Page<Post> resultPage = postMapper.selectPage(page, queryWrapper);

        // 转换为PostResponse对象
        Page<PostResponse> responsePage = new Page<>(current, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setPages(resultPage.getPages());
        responsePage.setCurrent(resultPage.getCurrent());
        responsePage.setSize(resultPage.getSize());

        // 组装PostResponse数据
        List<PostResponse> postResponses = resultPage.getRecords().stream().map(post -> {
            PostResponse response = new PostResponse();
            response.setId(post.getId());
            response.setTitle(post.getTitle());
            response.setStatus(post.getStatus());
            response.setCreatedAt(post.getCreatedAt());
            response.setPublishedAt(post.getPublishedAt());
            response.setLikedNum(post.getLikeNum() == null ? 0L : post.getLikeNum());
            response.setFavoritedNum(post.getFavNum() == null ? 0L : post.getFavNum());

            // 查询帖子内容
            PostContent postContent = postContentMapper.selectOne(new QueryWrapper<PostContent>()
                    .eq("post_id", post.getId()));
            if (postContent != null) {
                response.setContent(postContent.getContentMd());
            }

            // 查询板块名称
            Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                    .select("name")
                    .eq("id", post.getBoardId()));
            if (board != null) {
                response.setBoardName(board.getName());
            }

            // 查询作者昵称和头像
            if (post.getAuthorId() != null && !post.getAuthorId().isEmpty()) {
                User author = userMapper.selectById(post.getAuthorId());
                if (author != null) {
                    response.setAuthorNickname(author.getNickname());
                    response.setAuthorAvatar(author.getAvatar());
                }
            }

            // 判断当前用户是否已点赞
            response.setIsLiked(interactionCacheService.isLiked(currentUserId, post.getId()));

            // 判断当前用户是否已收藏
            response.setIsFavorited(interactionCacheService.isFavorited(currentUserId, post.getId()));

            return response;
        }).collect(java.util.stream.Collectors.toList());

        responsePage.setRecords(postResponses);

        return Result.success(responsePage);
    }

    @Override
    public Result getFavoritedPostPage(Integer current, Integer size, String keyword) {
        // 从UserContext获取当前登录用户ID
        String targetUserId = UserContext.getCurrentUserId();
        if (targetUserId == null || targetUserId.isEmpty()) {
            return Result.error("401", "未登录");
        }

        List<Favorite> dbFavorites = favoriteMapper.selectList(
                new QueryWrapper<Favorite>()
                        .select("target_id")
                        .eq("user_id", targetUserId));
        if (dbFavorites == null || dbFavorites.isEmpty()) {
            Page<FavoritedPostResponse> empty = new Page<>(current, size);
            empty.setTotal(0);
            empty.setRecords(java.util.Collections.emptyList());
            return Result.success(empty);
        }

        Set<Long> favoritedPostIds = dbFavorites.stream()
                .map(Favorite::getTargetId)
                .collect(java.util.stream.Collectors.toSet());

        // 关键词过滤：按内容模糊查询
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<PostContent> matchContents = postContentMapper.selectList(
                    new QueryWrapper<PostContent>()
                            .select("post_id")
                            .like("content_md", keyword.trim()));

            if (matchContents == null || matchContents.isEmpty()) {
                Page<FavoritedPostResponse> empty = new Page<>(current, size);
                empty.setTotal(0);
                empty.setRecords(java.util.Collections.emptyList());
                return Result.success(empty);
            }

            Set<Long> matchedIds = matchContents.stream()
                    .map(PostContent::getPostId)
                    .collect(java.util.stream.Collectors.toSet());

            // 取交集
            favoritedPostIds.retainAll(matchedIds);

            if (favoritedPostIds.isEmpty()) {
                Page<FavoritedPostResponse> empty = new Page<>(current, size);
                empty.setTotal(0);
                empty.setRecords(java.util.Collections.emptyList());
                return Result.success(empty);
            }
        }

        // 分页查询帖子
        Page<Post> page = new Page<>(current, size);
        QueryWrapper<Post> qw = new QueryWrapper<>();
        qw.in("id", favoritedPostIds);
        qw.eq("status", 1);
        qw.orderByDesc("published_at");
        Page<Post> resultPage = postMapper.selectPage(page, qw);

        // 组装响应
        Page<FavoritedPostResponse> responsePage = new Page<>(current, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setPages(resultPage.getPages());
        responsePage.setCurrent(resultPage.getCurrent());
        responsePage.setSize(resultPage.getSize());

        List<FavoritedPostResponse> postResponses = resultPage.getRecords().stream().map(post -> {
            FavoritedPostResponse resp = new FavoritedPostResponse();
            resp.setPostId(post.getId());
            resp.setTitle(post.getTitle());

            PostContent pc = postContentMapper.selectOne(
                    new QueryWrapper<PostContent>().eq("post_id", post.getId()));
            if (pc != null)
                resp.setContent(pc.getContentMd());

            return resp;
        }).collect(java.util.stream.Collectors.toList());

        responsePage.setRecords(postResponses);
        return Result.success(responsePage);
    }

    @Override
    public Result getPostDetail(Long postId) {
        if (postId == null) {
            return Result.error("400", "帖子ID不能为空");
        }

        // 查询帖子
        Post post = postMapper.selectById(postId);
        if (post == null || !canRead(post)) {
            return Result.error("404", "帖子不存在");
        }

        // 获取当前用户ID
        String currentUserId = UserContext.getCurrentUserId();

        // 组装响应
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setStatus(post.getStatus());
        response.setCreatedAt(post.getCreatedAt());
        response.setPublishedAt(post.getPublishedAt());
        response.setLikedNum(post.getLikeNum() == null ? 0L : post.getLikeNum());
        response.setFavoritedNum(post.getFavNum() == null ? 0L : post.getFavNum());

        // 获取浏览量（数据库 + Redis 增量）
        response.setViewCount(viewCountService.getViewCount(postId));

        // 查询帖子内容
        PostContent postContent = postContentMapper.selectOne(new QueryWrapper<PostContent>()
                .eq("post_id", post.getId()));
        if (postContent != null) {
            response.setContent(postContent.getContentMd());
        }

        // 查询板块名称
        Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                .select("name")
                .eq("id", post.getBoardId()));
        if (board != null) {
            response.setBoardName(board.getName());
        }

        // 查询作者昵称和头像
        if (post.getAuthorId() != null && !post.getAuthorId().isEmpty()) {
            User author = userMapper.selectById(post.getAuthorId());
            if (author != null) {
                response.setAuthorNickname(author.getNickname());
                response.setAuthorAvatar(author.getAvatar());
            }
        }

        // 判断当前用户是否已点赞
        if (currentUserId != null) {
            response.setIsLiked(interactionCacheService.isLiked(currentUserId, post.getId()));
        } else {
            response.setIsLiked(false);
        }

        // 判断当前用户是否已收藏
        if (currentUserId != null) {
            response.setIsFavorited(interactionCacheService.isFavorited(currentUserId, post.getId()));
        } else {
            response.setIsFavorited(false);
        }

        return Result.success(response);
    }

    private boolean canRead(Post post) {
        if (Integer.valueOf(1).equals(post.getStatus())) return true;
        String viewer=UserContext.getCurrentUserId();
        if (viewer==null) return false;
        if (viewer.equals(post.getAuthorId())) return true;
        User user=userMapper.selectById(viewer);
        return user!=null && Integer.valueOf(1).equals(user.getUsertype());
    }

    @Override
    public Long getPostCount() {
        return postMapper.selectCount(null);
    }

    @Override
    public Result getTopPosts() {
        // 查询条件：status=1 且 is_top=1
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 已发布
        queryWrapper.eq("is_top", 1); // 置顶
        queryWrapper.isNotNull("top_time"); // 必须有置顶时间

        // 计算3天前的时间点（只显示3天内的置顶帖子）
        // LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        // queryWrapper.ge("top_time", threeDaysAgo);
        // top_time >= 3天前（即3天内）

        // 按 top_time 倒序排列（越晚置顶的越靠前）
        queryWrapper.orderByDesc("top_time");

        // 执行查询（不限制数量，返回所有符合条件的置顶帖子）
        List<Post> topPosts = postMapper.selectList(queryWrapper);

        // 获取当前用户ID（用于判断是否点赞）
        String currentUserId = UserContext.getCurrentUserId();

        // 组装 PostResponse 数据
        List<PostResponse> postResponses = topPosts.stream().map(post -> {
            PostResponse response = new PostResponse();
            response.setId(post.getId());
            response.setTitle(post.getTitle());
            response.setStatus(post.getStatus());
            response.setCreatedAt(post.getCreatedAt());
            response.setPublishedAt(post.getPublishedAt());
            response.setLikedNum(post.getLikeNum() == null ? 0L : post.getLikeNum());
            response.setFavoritedNum(post.getFavNum() == null ? 0L : post.getFavNum());
            response.setViewCount(post.getViewCount() == null ? 0L : post.getViewCount());

            // 查询帖子内容
            PostContent postContent = postContentMapper.selectOne(new QueryWrapper<PostContent>()
                    .eq("post_id", post.getId()));
            if (postContent != null) {
                response.setContent(postContent.getContentMd());
            }

            // 查询板块名称
            Board board = boardMapper.selectOne(new QueryWrapper<Board>()
                    .select("name")
                    .eq("id", post.getBoardId()));
            if (board != null) {
                response.setBoardName(board.getName());
            }

            // 查询作者昵称和头像
            if (post.getAuthorId() != null && !post.getAuthorId().isEmpty()) {
                User author = userMapper.selectById(post.getAuthorId());
                if (author != null) {
                    response.setAuthorNickname(author.getNickname());
                    response.setAuthorAvatar(author.getAvatar());
                }
            }

            // 判断当前用户是否已点赞
            if (currentUserId != null) {
                response.setIsLiked(interactionCacheService.isLiked(currentUserId, post.getId()));
            } else {
                response.setIsLiked(false);
            }

            // 判断当前用户是否已收藏
            if (currentUserId != null) {
                response.setIsFavorited(interactionCacheService.isFavorited(currentUserId, post.getId()));
            } else {
                response.setIsFavorited(false);
            }

            return response;
        }).collect(java.util.stream.Collectors.toList());

        return Result.success(postResponses);
    }

    @Override
    @Transactional
    public Result toggleTop(Long postId) {
        String currentUserId = UserContext.getCurrentUserId();
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        if (currentUser == null || currentUser.getUsertype() == null || currentUser.getUsertype() != 1) {
            return Result.error("403", "只有管理员可以取消帖子置顶");
        }

        // 1. 查询帖子
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return Result.error("404", "帖子不存在");
        }

        // 2. 检查帖子状态（只有已发布的帖子才能置顶）
        if (post.getStatus() == null || post.getStatus() != 1) {
            return Result.error("400", "只能置顶已发布的帖子");
        }

        // 新增置顶必须由支付回调履行，禁止直接调用接口绕过付款。
        if (post.getIsTop() == null || post.getIsTop() == 0) {
            return Result.error("402", "置顶帖子必须先完成支付");
        }

        // 此接口仅保留管理员取消置顶能力。
        int result = postMapper.update(null,
                new LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .set(Post::getIsTop, 0)
                        .set(Post::getTopTime, null));
        if (result > 0) {
            log.info("管理员 {} 已取消帖子 {} 的置顶", currentUserId, postId);
            return Result.success("取消置顶成功");
        } else {
            return Result.error("500", "操作失败");
        }
    }

    @Override
    @Transactional
    public int cleanExpiredTopPosts() {
        // 计算3天前的时间点
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // 查询所有超过3天的置顶帖子
        List<Post> expiredTopPosts = postMapper.selectList(
                new QueryWrapper<Post>()
                        .eq("is_top", 1)
                        .isNotNull("top_time")
                        .lt("top_time", threeDaysAgo));

        if (expiredTopPosts.isEmpty()) {
            return 0;
        }

        // 批量更新：将 is_top 设为 0，top_time 设为 null
        int count = 0;
        for (Post post : expiredTopPosts) {
            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setIsTop(0);
            postMapper.updateById(updatePost);
            count++;
            log.info("帖子 {} 置顶已过期，自动恢复为普通状态", post.getId());
        }

        log.info("清理过期置顶帖子完成，共清理 {} 条", count);
        return count;
    }
}
