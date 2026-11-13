# ExamSphere – Detailed Project Handoff

## 1. Project overview

**Project name:** ExamSphere – Online Examination System

**Purpose:** Provide a small web-based examination workflow. An administrator creates an examination and its MCQ questions. A student registers, signs in, attempts a timed examination, submits answers, and views the score history.

**Implementation:** The application is deliberately Java-first. Java's built-in `HttpServer` serves the HTML/CSS/JavaScript files and exposes HTTP API endpoints. The Java service layer uses JDBC to read and write MySQL tables hosted by XAMPP.

> Syllabus alignment note: the exact case-study document and official lab syllabus were not attached in this conversation. The mapping below therefore uses a typical Java OOP + JDBC lab syllabus. Compare it with your faculty's prescribed list before submission and rename/add mapping rows if needed.

## 2. Architecture

```text
Browser
  |
  | HTML + CSS + vanilla JavaScript (fetch)
  v
ExamSphereServer.java  -- Java built-in HttpServer
  |
  | calls service methods
  v
ExamService.java       -- application rules and JDBC operations
  |
  +--> User.java / Exam.java / Question.java (model classes)
  |
  +--> Database.java --> MySQL Connector/J --> XAMPP MySQL
```

### Why this architecture?

- **Frontend:** HTML defines page structure, CSS controls the visual design, and JavaScript handles interaction.
- **HTTP server:** Java `HttpServer` avoids a separate web framework and makes the request/response cycle visible for a viva.
- **Service layer:** `ExamService` keeps business logic out of the HTTP routing code.
- **Database layer:** `Database` creates JDBC connections. `PreparedStatement` is used for parameterized SQL.
- **Models:** `User`, `Exam`, and `Question` represent application data.

## 3. Main user workflows

### Student

1. Open the home page.
2. Register with a name, email and password (student registration only).
3. Sign in using the Student role.
4. View active examinations.
5. Select **Start examination**.
6. Answer the displayed MCQs before the countdown ends.
7. Submit the exam.
8. Java retrieves the correct answers from MySQL and calculates the score.
9. The attempt and selected answers are saved.
10. The dashboard shows the result history.

### Administrator

1. Sign in using the pre-created admin account: `admin@examsphere.local` / `admin123`; choose **Administrator** in the role selector. Admin accounts are not created through public student registration.
2. Create an examination with a title, description and duration.
3. Select the examination in the question form.
4. Enter the question, four options, correct option and marks.
5. Students can now see the active exam.

## 4. Java class-by-class explanation

### `Database.java`

- Stores the JDBC URL, username and password.
- Reads optional environment variables so local database settings can be changed without editing source code.
- `getConnection()` returns a JDBC `Connection`.
- It is a small utility class with a private constructor because objects of this class are not needed.

### `PasswordUtil.java`

- Converts a password into a SHA-256 hexadecimal hash.
- The database stores the hash rather than the original password.
- `MessageDigest` is a standard Java security API.
- Viva point: SHA-256 is used here to demonstrate hashing, but a production login should use a unique salt and a password-specific algorithm such as PBKDF2, BCrypt or Argon2.

### `User.java`

- Represents a user.
- Fields are `private final`, demonstrating encapsulation and immutable object state.
- Constructor initializes the object; getters provide read access.

### `Exam.java`

- Represents an examination with an ID, title, description and duration.
- It is a model / entity class.

### `Question.java`

- Represents one MCQ without exposing the correct answer to the student-facing API.
- The correct option is retrieved only inside the scoring service.

### `ExamService.java`

This is the business/service layer.

- `authenticate()` checks email, password hash and role using a prepared query.
- `register()` inserts a student record.
- `getExams()` returns active exams.
- `getQuestions()` retrieves the question text and options, not the answer key.
- `createExam()` inserts an exam and reads its generated ID.
- `addQuestion()` inserts an MCQ.
- `submitAttempt()` calculates the score on the server, then stores the attempt and answers in one transaction.
- `getResultsJson()` retrieves the signed-in student's previous attempts.

### `HttpUtil.java`

- Reads `application/x-www-form-urlencoded` request data.
- Sends UTF-8 HTTP responses.
- Escapes values placed in JSON strings.
- Provides small validation helpers.

### `ExamSphereServer.java`

- Starts the built-in HTTP server on port 8080 by default.
- `/` serves `index.html`; other static paths serve CSS and JavaScript.
- `/api/...` routes handle registration, login, logout, exams, questions, submission, results and admin actions.
- A random UUID session token is stored in an in-memory map and sent as an HttpOnly cookie.
- Role checks prevent student accounts from using administrator routes.

## 5. OOP concepts demonstrated

| OOP concept | Where it appears | Viva explanation |
|---|---|---|
| Class and object | `User`, `Exam`, `Question`, `ExamService` | A class defines fields and methods; an object is an instance of a class. |
| Encapsulation | Private fields in model classes | Data is kept inside a class and exposed through public methods. |
| Abstraction | `ExamService` methods | The caller requests an operation without needing to know every SQL step. |
| Constructor | Model constructors | Initializes an object's state when it is created. |
| Method overloading | Not required in this version | Can be added if the prescribed syllabus explicitly requires it. |
| Inheritance | Not required in this version | The project favors composition; do not claim inheritance is implemented. |
| Polymorphism | Not explicitly central to this version | Can be demonstrated with an interface-based repository extension if required by the lab. |
| Exception handling | `try/catch`, `throws SQLException` | Handles database and request errors instead of silently ignoring them. |
| Collections | `List<Exam>`, `List<Question>`, `Map<Integer,String>` | Stores a variable number of objects and submitted answers. |
| Encapsulation / access modifiers | `private`, `public` | Controls where class members can be accessed. |

**Important viva honesty:** not every OOP feature is forced into the project. If the syllabus specifically awards marks for inheritance, interfaces, abstract classes or method overloading, extend the model with a meaningful role hierarchy or service interface rather than claiming those concepts already exist.

## 6. JDBC concepts demonstrated

1. `DriverManager.getConnection()` obtains a database connection.
2. `Connection` represents the active connection to MySQL.
3. `PreparedStatement` executes parameterized SQL and helps prevent SQL injection.
4. `ResultSet` reads query results row by row.
5. `executeQuery()` is used for SELECT statements.
6. `executeUpdate()` is used for INSERT statements.
7. `getGeneratedKeys()` reads auto-generated primary keys.
8. `setAutoCommit(false)`, `commit()` and `rollback()` provide transaction handling for exam submission.
9. Try-with-resources automatically closes JDBC resources.

## 7. Database design

### `users`

Stores student and administrator accounts. `email` is unique. `role` is either `STUDENT` or `ADMIN`.

### `exams`

Stores the exam title, description, duration and active flag.

### `questions`

Stores each MCQ and its answer key. `exam_id` is a foreign key to `exams`.

### `attempts`

Stores one submitted attempt, the user, exam, score and total marks.

### `attempt_answers`

Stores each selected option for an attempt. It references both the attempt and the question.

### Relationships

- One exam has many questions.
- One user can have many attempts.
- One exam can have many attempts.
- One attempt has many selected answers.

## 8. Syllabus mapping (typical Java OOP lab)

| Common syllabus topic | Project implementation |
|---|---|
| Java classes and objects | Model classes and service objects |
| Constructors and methods | Model constructors and service methods |
| Access modifiers | Private fields and public methods |
| Encapsulation | Private state with getters |
| Strings and arrays | String processing and option arrays in frontend |
| Collections Framework | `ArrayList`, `List`, `Map` |
| Exception handling | SQL and request error handling |
| Packages | `com.examsphere` |
| File / resource handling | Static web file serving; try-with-resources |
| JDBC | Connection, PreparedStatement, ResultSet |
| MySQL CRUD | Register, create exam, add question, retrieve data, save attempts |
| Basic web technologies | HTML, CSS and JavaScript |

This is a generic mapping, not a claim that it exactly matches a syllabus that was not supplied.

## 9. API reference

All API form submissions use `application/x-www-form-urlencoded`. JSON is returned.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/register` | Create a student account |
| POST | `/api/login` | Sign in as student/admin |
| POST | `/api/logout` | End current session |
| GET | `/api/me` | Check current session |
| GET | `/api/exams` | List active examinations |
| GET | `/api/questions?id=1` | Retrieve question text and options |
| POST | `/api/submit` | Submit answers and calculate score |
| GET | `/api/results` | Get current student's results |
| POST | `/api/admin/exams` | Create an examination (admin only) |
| POST | `/api/admin/questions` | Add an MCQ (admin only) |

## 10. Setup and execution

Follow the exact instructions in `README.md`. Short version:

1. Install JDK 17+ and XAMPP.
2. Start MySQL from XAMPP.
3. Import `database/examsphere.sql` in phpMyAdmin.
4. Download MySQL Connector/J and place its JAR in `lib/`.
5. Compile the Java files with `javac` and the connector JAR on the classpath.
6. Run `com.examsphere.ExamSphereServer`.
7. Open `http://localhost:8080`.

## 11. Test plan

| Test | Steps | Expected result |
|---|---|---|
| Student registration | Register a new email | Account is created |
| Duplicate email | Register same email again | Friendly database error is shown |
| Student login | Use valid student credentials | Student dashboard opens |
| Wrong password | Use incorrect password | Login is rejected |
| Admin login | Use demo admin credentials and Admin role | Admin dashboard opens |
| Create exam | Admin submits title and duration | Exam appears in admin selector and student list |
| Add question | Admin adds an MCQ | Question is available in exam |
| Take exam | Student selects options and submits | Server returns calculated score |
| Result history | Student returns to dashboard | Saved attempt is listed |
| Access control | Student attempts an admin endpoint | Request is rejected |

## 12. Viva questions and sample answers

**Q1. What is ExamSphere?**  
It is a web-based online examination system. An administrator creates MCQ examinations and students attempt them and view their scores.

**Q2. Why did you use Java?**  
Java supports object-oriented programming, has JDBC support for database connectivity, and provides a built-in HTTP server for a small framework-free web application.

**Q3. What is JDBC?**  
JDBC stands for Java Database Connectivity. It is a Java API used to connect to relational databases and execute SQL statements.

**Q4. What is the role of MySQL Connector/J?**  
It is the JDBC driver that allows Java applications to communicate with MySQL.

**Q5. What is a class? What is an object?**  
A class is a blueprint defining data and behavior. An object is a runtime instance of that class.

**Q6. What is encapsulation?**  
Encapsulation means keeping data and the methods that operate on it together and controlling access, commonly using private fields and public methods.

**Q7. Why are model fields private?**  
Private fields prevent unrelated classes from directly changing an object's internal state. Access is provided through methods such as getters.

**Q8. Why use PreparedStatement instead of Statement?**  
PreparedStatement separates SQL structure from supplied values, supports parameters and helps prevent SQL injection.

**Q9. What is ResultSet?**  
ResultSet holds the rows returned by a SELECT query. The program moves through rows using `next()` and reads columns using getter methods.

**Q10. What is a primary key?**  
A primary key uniquely identifies each row in a table.

**Q11. What is a foreign key?**  
A foreign key links a column in one table to a key in another table and helps maintain referential integrity.

**Q12. Why use a transaction while submitting an exam?**  
The attempt and its answers should be saved together. If saving fails, rollback prevents a partially stored submission.

**Q13. What is the difference between executeQuery and executeUpdate?**  
`executeQuery()` is normally used for SELECT and returns a ResultSet. `executeUpdate()` is used for INSERT, UPDATE and DELETE and returns the affected-row count.

**Q14. How is the score calculated?**  
The Java service reads the answer key from MySQL, compares each selected option, adds the marks for correct answers and stores the score.

**Q15. Why is scoring done on the server?**  
A browser can be modified by a user. Calculating the score on the Java server avoids trusting a score sent by the frontend.

**Q16. What is a session?**  
A session is server-side information used to recognize a signed-in user across multiple HTTP requests.

**Q17. What is the purpose of the service layer?**  
It separates application rules and database operations from HTTP request handling, making the code easier to test and maintain.

**Q18. What is the difference between GET and POST?**  
GET requests retrieve information. POST requests send data that may create or change server-side information.

**Q19. What is exception handling?**  
It is the mechanism for detecting and handling runtime problems using constructs such as try, catch, finally and throws.

**Q20. What improvements would you make for production?**  
Use salted password hashing, HTTPS, CSRF protection, session expiry, rate limiting, server-enforced exam deadlines, audit logs, stronger validation and automated tests.

## 13. Current limitations / future enhancements

- The countdown is enforced in the browser only; the server does not enforce an absolute deadline.
- Sessions are stored in memory and are lost when the server restarts.
- The demo does not include negative marking, randomized question order, exam scheduling, attempt limits, or downloadable reports.
- SHA-256 is educational only; replace it with a password-specific salted hash before any real deployment.
- Add unit tests and a repository interface if the lab syllabus requires interfaces or polymorphism.
- Add a proper role hierarchy only if it serves the case study and is required by the syllabus.

## 14. Suggested viva demonstration order

1. Show the database tables in phpMyAdmin.
2. Sign in as admin.
3. Create a small exam and add two questions.
4. Register a student in another browser/private window.
5. Sign in as the student and take the exam.
6. Submit it and show the result history.
7. Open `ExamService.submitAttempt()` and explain server-side scoring and transaction handling.
8. Open `Database.getConnection()` and explain JDBC configuration.


## Administrator reports update

The administrator dashboard includes a platform overview, a registered-users table, and an all-attempts results table. The Java server exposes `/api/admin/stats`, `/api/admin/users`, and `/api/admin/results`; each endpoint calls `requireAdmin` before reading data. User reports intentionally omit password hashes. Recompile the Java sources after updating this project. No database schema change is required for these reports.
