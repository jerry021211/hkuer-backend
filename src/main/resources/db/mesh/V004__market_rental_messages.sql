CREATE TABLE market_item(post_id bigint PRIMARY KEY,price decimal(12,2),negotiable boolean NOT NULL DEFAULT false,brand varchar(64),
 trade_location varchar(100),condition_label varchar(32),sale_status varchar(16) NOT NULL DEFAULT 'selling',batch_id varchar(64),
 KEY idx_market_price(sale_status,price)) ENGINE=InnoDB;
CREATE TABLE rental_post(post_id bigint PRIMARY KEY,rental_type varchar(16) NOT NULL,rent decimal(12,2),negotiable boolean NOT NULL DEFAULT false,
 approximate boolean NOT NULL DEFAULT false,budget_min decimal(12,2),budget_max decimal(12,2),room_types json,
 lease_term varchar(32),move_in date,gender varchar(32),habits json,listing_status varchar(16) NOT NULL DEFAULT 'active',
 KEY idx_rental_type(rental_type,listing_status,rent)) ENGINE=InnoDB;
CREATE TABLE conversation(id bigint PRIMARY KEY,user_low varchar(64) NOT NULL,user_high varchar(64) NOT NULL,context_post_id bigint NOT NULL DEFAULT 0,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_conversation_pair(user_low,user_high,context_post_id)) ENGINE=InnoDB;
CREATE TABLE conversation_member(conversation_id bigint NOT NULL,user_id varchar(64) NOT NULL,last_read_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(conversation_id,user_id),KEY idx_conversation_member(user_id,conversation_id)) ENGINE=InnoDB;
CREATE TABLE chat_message(id bigint PRIMARY KEY,conversation_id bigint NOT NULL,sender_id varchar(64) NOT NULL,content varchar(2000) NOT NULL,
 client_id varchar(64) NOT NULL,created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_message_retry(sender_id,client_id),KEY idx_message_cursor(conversation_id,id)) ENGINE=InnoDB;
CREATE TABLE notification(id bigint PRIMARY KEY,user_id varchar(64) NOT NULL,actor_id varchar(64),kind varchar(32) NOT NULL,
 title varchar(200) NOT NULL,content varchar(1000),target_path varchar(255),is_read boolean NOT NULL DEFAULT false,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,KEY idx_notification_user(user_id,is_read,created_at,id)) ENGINE=InnoDB;
