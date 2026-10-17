INSERT INTO board(id,slug,name) VALUES
 (1,'qa','问答'),(2,'job','求职'),(3,'life','生活'),(4,'market','二手'),(5,'rental','租房'),
 (6,'school-life','School Life'),(7,'learning-exchange','Learning Exchange'),(8,'employment','Employment'),(9,'internship','Intership'),(10,'tree-hole','Tree Hole');
CREATE TABLE user_profile (
 user_id varchar(64) PRIMARY KEY,bio varchar(100) NOT NULL DEFAULT '',skills json,github varchar(512),linkedin varchar(512),website varchar(512),
 verified_hku boolean NOT NULL DEFAULT false,smoking varchar(32),sleep_schedule varchar(32),cleanliness varchar(32),occupation varchar(64),
 updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE post_meta (
 post_id bigint PRIMARY KEY,kind varchar(16) NOT NULL,category varchar(32),anonymous boolean NOT NULL DEFAULT false,
 images json,tags json,region varchar(64),rating int,company varchar(100),role varchar(100),course_id bigint,
 KEY idx_meta_kind(kind,category),KEY idx_meta_course(course_id)
) ENGINE=InnoDB;
CREATE TABLE answer (
 id bigint PRIMARY KEY,question_id bigint NOT NULL,author_id varchar(64) NOT NULL,content text NOT NULL,
 anonymous boolean NOT NULL DEFAULT false,accepted boolean NOT NULL DEFAULT false,status int NOT NULL DEFAULT 1,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_answer_question(question_id,status,created_at,id),KEY idx_answer_author(author_id,status,created_at)
) ENGINE=InnoDB;
CREATE TABLE question_interaction (
 question_id bigint NOT NULL,user_id varchar(64) NOT NULL,kind varchar(16) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(question_id,user_id,kind),KEY idx_question_interaction_user(user_id,kind,created_at)
) ENGINE=InnoDB;
CREATE TABLE answer_like (answer_id bigint NOT NULL,user_id varchar(64) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(answer_id,user_id)) ENGINE=InnoDB;
ALTER TABLE attachment ADD COLUMN owner_id varchar(64), ADD COLUMN original_name varchar(255), ADD COLUMN content_type varchar(128), ADD COLUMN size_bytes bigint;
CREATE TABLE announcement (id bigint PRIMARY KEY,title varchar(200) NOT NULL,content text NOT NULL,status int NOT NULL DEFAULT 1,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;
INSERT INTO announcement(id,title,content) VALUES(1,'欢迎来到 HKUer','这里是独立的新开发环境。欢迎分享问题、课程经验与校园生活。');
