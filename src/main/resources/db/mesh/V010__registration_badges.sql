INSERT INTO badge(code,name,description,symbol) VALUES ('PIONEER','先驱勋章','第 201–1000 位注册用户','✦');
UPDATE badge SET description='前 200 位注册用户专属' WHERE code='GENESIS';
CREATE TABLE badge_registration (
 user_id varchar(64) PRIMARY KEY,
 registration_rank bigint NOT NULL UNIQUE
) ENGINE=InnoDB;
INSERT INTO badge_registration(user_id,registration_rank)
 SELECT id,ROW_NUMBER() OVER (ORDER BY created_at,id) FROM user WHERE password IS NOT NULL;
CREATE TABLE badge_registration_sequence (
 id int PRIMARY KEY,
 last_rank bigint NOT NULL
) ENGINE=InnoDB;
INSERT INTO badge_registration_sequence(id,last_rank) SELECT 1,COALESCE(MAX(registration_rank),0) FROM badge_registration;
INSERT IGNORE INTO user_badge(user_id,badge_code)
 SELECT user_id,CASE WHEN registration_rank<=200 THEN 'GENESIS' ELSE 'PIONEER' END
 FROM badge_registration WHERE registration_rank<=1000;
