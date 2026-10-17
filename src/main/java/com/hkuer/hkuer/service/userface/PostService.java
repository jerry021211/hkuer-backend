package com.hkuer.hkuer.service.userface;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hkuer.hkuer.common.Result;
import com.hkuer.hkuer.entity.DTO.PostInsertRequest;
import com.hkuer.hkuer.entity.PO.Post;
import com.hkuer.hkuer.entity.VO.PostResponse;

public interface PostService {
    public Result insertPost(PostInsertRequest request);
    Result updatePost(Long postId, PostInsertRequest request);

    public Result deletePost(Long postId);

    public Result getPostPage(Integer current, Integer size, String boardName, String title, Long postId);

    public Result getLikedPostPage(Integer current, Integer size, String keyword);

    public Result getFavoritedPostPage(Integer current, Integer size, String keyword);

    public Result getLatestPosts();

    public Result getMyPosts(Integer current, Integer size, String title);
    
    /**
     * 获取帖子详情（包含浏览量）
     */
    public Result getPostDetail(Long postId);
    
    /**
     * 获取帖子总数
     */
    public Long getPostCount();

    /**
     * 获取置顶帖子列表
     * 查询条件：status=1 且 is_top=1，按 top_time 倒序排列
     *
     * @return 置顶帖子列表
     */
    public Result getTopPosts();

    /**
     * 切换帖子置顶状态
     * 如果当前是普通状态(0)，则置顶(1)并设置 top_time 为当前时间
     * 如果当前是置顶状态(1)，则取消置顶(0)并清空 top_time
     * 注意：置顶有效期为3天，超过3天的置顶帖子会由定时任务自动清理
     *
     * @param postId 帖子ID
     * @return 操作结果
     */
    public Result toggleTop(Long postId);

    /**
     * 清理过期的置顶帖子
     * 将超过3天的置顶帖子（is_top=1）自动恢复为普通状态（is_top=0, top_time=null）
     *
     * @return 清理的帖子数量
     */
    public int cleanExpiredTopPosts();
}
