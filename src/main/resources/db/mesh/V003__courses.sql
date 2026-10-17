CREATE TABLE course (id bigint PRIMARY KEY,code varchar(32) NOT NULL,name varchar(200) NOT NULL,subtitle varchar(255),
 level varchar(8) NOT NULL,subject varchar(64) NOT NULL,credits decimal(4,1),description text,status int NOT NULL DEFAULT 1,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_course_code(code),KEY idx_course_filter(level,subject,status,code)) ENGINE=InnoDB;
CREATE TABLE course_participation (course_id bigint NOT NULL,user_id varchar(64) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(course_id,user_id),KEY idx_course_participant(user_id,created_at)) ENGINE=InnoDB;
CREATE TABLE course_review (id bigint PRIMARY KEY,course_id bigint NOT NULL,user_id varchar(64) NOT NULL,semester varchar(32) NOT NULL,
 overall int NOT NULL,difficulty int NOT NULL,workload int NOT NULL,teaching int NOT NULL,exam_difficulty int NOT NULL,
 midterm boolean NOT NULL DEFAULT false,final_exam boolean NOT NULL DEFAULT false,open_book boolean NOT NULL DEFAULT false,
 content varchar(1000) NOT NULL,anonymous boolean NOT NULL DEFAULT false,status int NOT NULL DEFAULT 1,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_course_review(course_id,user_id,semester),KEY idx_course_review(course_id,status,created_at),
 CHECK(overall BETWEEN 1 AND 5),CHECK(difficulty BETWEEN 1 AND 5),CHECK(workload BETWEEN 1 AND 5),CHECK(teaching BETWEEN 1 AND 5),CHECK(exam_difficulty BETWEEN 1 AND 5)) ENGINE=InnoDB;
CREATE TABLE course_material (id bigint PRIMARY KEY,course_id bigint NOT NULL,user_id varchar(64) NOT NULL,title varchar(200) NOT NULL,
 category varchar(32) NOT NULL,url varchar(1024) NOT NULL,status int NOT NULL DEFAULT 1,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_course_material(course_id,status,category)) ENGINE=InnoDB;
CREATE TABLE course_group (id bigint PRIMARY KEY,course_id bigint NOT NULL,owner_id varchar(64) NOT NULL,name varchar(100) NOT NULL,
 description varchar(500),capacity int NOT NULL DEFAULT 20,status int NOT NULL DEFAULT 1,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE course_group_member(group_id bigint NOT NULL,user_id varchar(64) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(group_id,user_id)) ENGINE=InnoDB;
CREATE TABLE course_vote(course_id bigint NOT NULL,user_id varchar(64) NOT NULL,recommend boolean NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(course_id,user_id)) ENGINE=InnoDB;
CREATE TABLE course_bookmark(course_id bigint NOT NULL,user_id varchar(64) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(course_id,user_id)) ENGINE=InnoDB;
