-- Complete schema for a NEW database only. Preserves existing entity names and payment binding.
CREATE TABLE `user` (
 id varchar(64) PRIMARY KEY, username varchar(64), password varchar(255), nickname varchar(64), avatar varchar(1024),
 email varchar(254), verification_code varchar(16), usertype int NOT NULL DEFAULT 0, status int NOT NULL DEFAULT 0,
 mute_end_time datetime, ban_end_time datetime, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_user_username(username), UNIQUE KEY uk_user_email(email)
) ENGINE=InnoDB;
CREATE TABLE board (id bigint PRIMARY KEY, slug varchar(64) NOT NULL, name varchar(64) NOT NULL,
 UNIQUE KEY uk_board_slug(slug), UNIQUE KEY uk_board_name(name)) ENGINE=InnoDB;
CREATE TABLE post (
 id bigint PRIMARY KEY, board_id bigint NOT NULL, author_id varchar(64) NOT NULL, title varchar(200) NOT NULL,
 status int NOT NULL DEFAULT 1, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, published_at datetime,
 like_num bigint NOT NULL DEFAULT 0, fav_num bigint NOT NULL DEFAULT 0, view_count bigint NOT NULL DEFAULT 0,
 is_top int NOT NULL DEFAULT 0, top_time datetime,
 KEY idx_post_feed(board_id,status,created_at,id), KEY idx_post_author(author_id,created_at,id)
) ENGINE=InnoDB;
CREATE TABLE post_content (post_id bigint PRIMARY KEY, content_md mediumtext) ENGINE=InnoDB;
CREATE TABLE comment_article (
 call_id bigint PRIMARY KEY, post_id bigint NOT NULL, user_id varchar(64) NOT NULL, content_md text NOT NULL,
 status int NOT NULL DEFAULT 1, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at datetime,
 KEY idx_comment_post(post_id,status,created_at), KEY idx_comment_user(user_id,created_at)
) ENGINE=InnoDB;
CREATE TABLE comment_relation (
 re_id bigint PRIMARY KEY, post_id bigint NOT NULL, call_id bigint NOT NULL, re_user_id varchar(64) NOT NULL,
 re_comment text NOT NULL, parent_re_id bigint, status int NOT NULL DEFAULT 1,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at datetime, KEY idx_reply_parent(call_id,status,created_at)
) ENGINE=InnoDB;
CREATE TABLE post_like (
 like_id bigint PRIMARY KEY, user_id varchar(64) NOT NULL, target_id bigint NOT NULL, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_post_like_user_target(user_id,target_id), KEY idx_like_target(target_id)
) ENGINE=InnoDB;
CREATE TABLE post_favorite (
 favorite_id bigint PRIMARY KEY, user_id varchar(64) NOT NULL, target_id bigint NOT NULL, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_post_favorite_user_target(user_id,target_id), KEY idx_favorite_target(target_id)
) ENGINE=InnoDB;
CREATE TABLE teacher (id varchar(64) PRIMARY KEY, name varchar(128) NOT NULL, total_score bigint NOT NULL DEFAULT 0,
 rating_count int NOT NULL DEFAULT 0, average_rating decimal(4,2) NOT NULL DEFAULT 0) ENGINE=InnoDB;
CREATE TABLE ratings (id varchar(64) PRIMARY KEY, user_id varchar(64) NOT NULL, target_id varchar(64) NOT NULL,
 score int NOT NULL, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_rating_user_target(user_id,target_id)) ENGINE=InnoDB;
CREATE TABLE course_comment (id bigint PRIMARY KEY,user_id varchar(64) NOT NULL,teacher_id varchar(64) NOT NULL,
 content text NOT NULL,status int NOT NULL DEFAULT 1,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_teacher_comment(teacher_id,created_at)) ENGINE=InnoDB;
CREATE TABLE report (
 id bigint PRIMARY KEY,reporter_id varchar(64) NOT NULL,create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 target_id varchar(64) NOT NULL,target_type varchar(32) NOT NULL,target_user_id varchar(64),reason varchar(100),reason_detail text,images json,
 status int NOT NULL DEFAULT 0,handler_id varchar(64),handle_time datetime,handle_note text,KEY idx_report_status(status,create_time)
) ENGINE=InnoDB;
CREATE TABLE post_appeal (id bigint PRIMARY KEY,post_id bigint NOT NULL,user_id varchar(64) NOT NULL,reason text,status int NOT NULL DEFAULT 0,
 admin_reply text,handle_time datetime,create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,KEY idx_appeal_status(status,create_time)) ENGINE=InnoDB;
CREATE TABLE attachment (id bigint AUTO_INCREMENT PRIMARY KEY,url varchar(1024) NOT NULL,status int NOT NULL DEFAULT 0,
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,KEY idx_attachment_status(status,create_time)) ENGINE=InnoDB;
CREATE TABLE pay_order (
 id bigint AUTO_INCREMENT PRIMARY KEY,order_id varchar(64) NOT NULL,trade_no varchar(64),amount decimal(12,2) NOT NULL,actual_amount decimal(12,2),
 pay_type varchar(32),user_id varchar(64),business_type varchar(32),business_id bigint,status int NOT NULL DEFAULT 0,
 goods_name varchar(255),goods_detail text,client_ip varchar(64),create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,pay_time datetime,update_time datetime,
 UNIQUE KEY uk_pay_order_order_id(order_id),UNIQUE KEY uk_pay_order_trade_no(trade_no), KEY idx_pay_business(user_id,business_type,business_id,status)
) ENGINE=InnoDB;
CREATE TABLE outbox_event (
 event_id char(32) PRIMARY KEY,aggregate_type varchar(32) NOT NULL,aggregate_id varchar(64) NOT NULL,event_type varchar(64) NOT NULL,
 routing_key varchar(128) NOT NULL,payload json NOT NULL,status varchar(16) NOT NULL DEFAULT 'NEW',retry_count int NOT NULL DEFAULT 0,
 next_retry_at datetime(3) NOT NULL,last_error varchar(1000),created_at datetime(3) NOT NULL,published_at datetime(3),KEY idx_outbox_pending(status,next_retry_at,created_at)
) ENGINE=InnoDB;
CREATE TABLE view_count_flush_batch (batch_id char(32) PRIMARY KEY,item_count int NOT NULL,total_delta bigint NOT NULL,
 applied_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),KEY idx_view_batch_time(applied_at)) ENGINE=InnoDB;
