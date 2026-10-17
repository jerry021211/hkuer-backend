ALTER TABLE user_profile ADD COLUMN hku_email varchar(254), ADD COLUMN active_badge varchar(32), ADD UNIQUE KEY uk_profile_hku_email(hku_email);
CREATE TABLE badge(code varchar(32) PRIMARY KEY,name varchar(64) NOT NULL,description varchar(255) NOT NULL,symbol varchar(16) NOT NULL);
CREATE TABLE user_badge(user_id varchar(64) NOT NULL,badge_code varchar(32) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,badge_code));
INSERT INTO badge(code,name,description,symbol) VALUES ('GENESIS','创世勋章','社区的早期建设者','✋'),('HKU','港大认证','已验证港大邮箱','🎓');
