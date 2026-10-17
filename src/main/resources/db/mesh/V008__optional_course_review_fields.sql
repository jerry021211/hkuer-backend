-- Only overall is required. An unanswered exam question must not be counted as "No".
ALTER TABLE course_review
 MODIFY difficulty INT NULL,
 MODIFY workload INT NULL,
 MODIFY teaching INT NULL,
 MODIFY exam_difficulty INT NULL,
 MODIFY midterm BOOLEAN NULL DEFAULT NULL,
 MODIFY final_exam BOOLEAN NULL DEFAULT NULL,
 MODIFY open_book BOOLEAN NULL DEFAULT NULL;
