CREATE TABLE publish_batch (
  user_id VARCHAR(64) NOT NULL,
  request_key VARCHAR(64) NOT NULL,
  payload_hash CHAR(64) NOT NULL,
  result_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(user_id,request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE course_vote ADD COLUMN rating TINYINT NULL;
CREATE INDEX idx_post_meta_course ON post_meta(course_id,kind);
CREATE INDEX idx_notification_user_kind ON notification(user_id,kind,id);
