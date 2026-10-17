# 接口分组与复用

前端开发服务器将 `/java/*` 转发到后端并移除 `/java` 前缀。下表是后端路径；完整参数以 Controller、DTO 和前端 API 类型为准。

| 分组 | 主要路径 | 说明 |
|---|---|---|
| 原账号 | `/user/public-key`、`/user/login`、`/user/insert`、`/user/refresh`、`/user/logout` | RSA 登录、注册、续期与登出 |
| 原帖子互动 | `/like/do`、`/like/undo`、`/favorite/do`、`/favorite/undo`、`/comment/article` | 继续复用原实现 |
| 原内容与治理 | `/post/*`、`/file/upload`、`/report/submit`、`/post/appeal` | 内容、上传、举报与申诉 |
| 社区公开查询 | `/mesh/public/session`、`/mesh/public/home`、`/mesh/public/content` | 游客会话、首页及列表 |
| 社区内容 | `/mesh/content`、`/mesh/content/{id}/edit`、`/mesh/content/{id}/status` | 新内容、本人编辑与状态 |
| 课程 | `/mesh/public/courses`、`/mesh/courses/{id}/*` | 查询、评价、投票、参与、资料与组队 |
| 小组 | `/mesh/groups/{id}`、`/mesh/groups/{id}/edit`、`/mesh/groups/{id}/membership` | 成员详情、管理与加入/退出 |
| 个人与消息 | `/mesh/profile`、`/mesh/conversations`、`/mesh/notifications` | 资料、私信、通知 |
| 找回与认证 | `/mesh/public/password/*`、`/mesh/verification/*` | 验证码、密码重置、HKU 邮箱认证 |
| 管理扩展 | `/console/mesh/*` | 社区内容、公告与课程管理，仅管理员可调用 |

业务响应沿用 `Result`。`code` 是字符串；除成功外，发布内容可能返回 `3000` 表示进入审核流程，不能把它当成网络失败后无限重试。重复提交应使用已有幂等机制；不能仅靠前端禁用按钮保证一致性。

公开接口不等于可以读取全部内容：游客正文、匿名作者、私人收藏、组内联系方式等仍由服务端按场景限制。
