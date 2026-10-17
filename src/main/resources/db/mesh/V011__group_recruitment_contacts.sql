ALTER TABLE course_group
 ADD COLUMN skills json,
 ADD COLUMN contact_wechat varchar(50) NOT NULL DEFAULT '',
 ADD COLUMN contact_note varchar(200) NOT NULL DEFAULT '';
