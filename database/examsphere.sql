-- ExamSphere database. Import this file using phpMyAdmin in XAMPP.
CREATE DATABASE IF NOT EXISTS examsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE examsphere;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash CHAR(64) NOT NULL,
  role ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exams (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  duration_minutes INT NOT NULL DEFAULT 30,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS questions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  exam_id INT NOT NULL,
  question_text TEXT NOT NULL,
  option_a VARCHAR(500) NOT NULL,
  option_b VARCHAR(500) NOT NULL,
  option_c VARCHAR(500) NOT NULL,
  option_d VARCHAR(500) NOT NULL,
  correct_option CHAR(1) NOT NULL,
  marks INT NOT NULL DEFAULT 1,
  CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attempts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  exam_id INT NOT NULL,
  score INT NOT NULL,
  total_marks INT NOT NULL,
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_attempt_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
);

CREATE TABLE IF NOT EXISTS attempt_answers (
  id INT AUTO_INCREMENT PRIMARY KEY,
  attempt_id INT NOT NULL,
  question_id INT NOT NULL,
  selected_option CHAR(1) NOT NULL DEFAULT '',
  CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
  CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

-- Demo administrator credentials:
-- Email: admin@examsphere.local
-- Password: admin123
-- SHA-256 hash of "admin123" (demo only; see security note in README).
INSERT INTO users(full_name,email,password_hash,role)
VALUES ('ExamSphere Admin','admin@examsphere.local',
'240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','ADMIN')
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), password_hash=VALUES(password_hash), role='ADMIN';

INSERT INTO exams(title,description,duration_minutes,active)
SELECT 'Java OOP Fundamentals','A starter assessment covering classes, objects, inheritance and polymorphism.',20,TRUE
WHERE NOT EXISTS (SELECT 1 FROM exams WHERE title='Java OOP Fundamentals');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which keyword is used to create a subclass in Java?','this','extends','implements','super','B',1
FROM exams e WHERE e.title='Java OOP Fundamentals'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id);

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which concept allows one interface to have multiple implementations?','Encapsulation','Compilation','Polymorphism','Casting','C',1
FROM exams e WHERE e.title='Java OOP Fundamentals'
AND (SELECT COUNT(*) FROM questions q WHERE q.exam_id=e.id)=1;

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which access modifier makes a member accessible only within its class?','public','protected','default','private','D',1
FROM exams e WHERE e.title='Java OOP Fundamentals'
AND (SELECT COUNT(*) FROM questions q WHERE q.exam_id=e.id)=2;


-- Shared starter quiz: every registered student can see and attempt this exam.
INSERT INTO exams(title,description,duration_minutes,active)
SELECT 'Basic Python Quiz','A beginner-friendly quiz covering Python syntax, data types, operators, conditions, loops and functions.',15,TRUE
WHERE NOT EXISTS (SELECT 1 FROM exams WHERE title='Basic Python Quiz');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which symbol is used to start a single-line comment in Python?','#','//','--','/*','A',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which symbol is used to start a single-line comment in Python?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which data type is used to store True or False values?','str','bool','int','list','B',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which data type is used to store True or False values?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'What is the output of print(2 ** 3)?','6','8','9','5','B',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='What is the output of print(2 ** 3)?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which keyword is used to define a function in Python?','function','fun','def','define','C',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which keyword is used to define a function in Python?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which collection stores an ordered, changeable sequence of items?','tuple','set','list','frozenset','C',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which collection stores an ordered, changeable sequence of items?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'What is the result of len("Python")?','5','6','7','8','B',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='What is the result of len("Python")?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which operator checks whether two values are equal?','=','==','!=','>=','B',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which operator checks whether two values are equal?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which statement is used to make a decision based on a condition?','for','while','if','def','C',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which statement is used to make a decision based on a condition?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'What values are produced by range(3) when iterated?','1, 2, 3','0, 1, 2','0, 1, 2, 3','3 only','B',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='What values are produced by range(3) when iterated?');

INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks)
SELECT e.id,'Which built-in function converts a value to an integer when possible?','str()','float()','input()','int()','D',1
FROM exams e WHERE e.title='Basic Python Quiz'
AND NOT EXISTS (SELECT 1 FROM questions q WHERE q.exam_id=e.id AND q.question_text='Which built-in function converts a value to an integer when possible?');
