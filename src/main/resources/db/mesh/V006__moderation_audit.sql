CREATE TABLE moderation_audit(id bigint PRIMARY KEY,actor_id varchar(64) NOT NULL,target_type varchar(32) NOT NULL,target_id bigint NOT NULL,
 previous_status int NOT NULL,new_status int NOT NULL,reason varchar(500) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_moderation_target(target_type,target_id,created_at));
