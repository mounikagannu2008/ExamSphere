# ExamSphere – Online Examination System

A beginner-friendly Java OOP lab project with a browser-based HTML/CSS/JavaScript frontend, Java HTTP server, JDBC, and MySQL (XAMPP). It intentionally avoids Maven, Spring Boot, and frameworks.

## Features

- Student registration and sign-in
- Administrator sign-in
- Admin can create examinations and add four-option MCQs
- Admin dashboard shows registered user details, every submitted result, and platform activity counts (passwords/hashes are never exposed)
- Students can view available examinations and take timed tests
- Server-side scoring and saved answer history
- Student result history
- Responsive pink, white, and light-blue interface
- JDBC with `PreparedStatement`, generated keys, and transaction handling

## Technology

- Java 17 or later (uses `HttpServer` and modern Java APIs)
- HTML5, CSS3, vanilla JavaScript
- MySQL through XAMPP
- MySQL Connector/J JAR (the only external Java dependency)

## 1. Start MySQL in XAMPP

1. Install XAMPP and open the XAMPP Control Panel.
2. Start **MySQL**. Apache is not required because the Java server serves the frontend.
3. Open `http://localhost/phpmyadmin`.
4. Choose **Import**, select `database/examsphere.sql`, and click **Go**.

The script creates the `examsphere` database, tables, demo admin, a starter Java OOP exam, and a shared **Basic Python Quiz** (10 questions, 15 minutes) visible to all students.

## 2. Download MySQL Connector/J

Download the platform-independent MySQL Connector/J ZIP from the official MySQL Connector/J download page:
https://dev.mysql.com/downloads/connector/j/

Extract it and copy the JAR (for example `mysql-connector-j-9.x.x.jar`) into the project folder `lib/`. Create `lib` if it does not exist. The JAR is not bundled because it is a separately licensed third-party dependency and its version may change.

## 3. Compile and run

Open a terminal in the `ExamSphere` project folder.

**Windows (Command Prompt):**

```bat
mkdir out
javac --add-modules jdk.httpserver -cp "lib\mysql-connector-j-9.x.x.jar" -d out src\main\java\com\examsphere\*.java
java --add-modules jdk.httpserver -cp "out;lib\mysql-connector-j-9.x.x.jar" com.examsphere.ExamSphereServer
```

Replace the JAR filename with the exact file you downloaded.

**Linux/macOS:**

```bash
mkdir -p out
javac --add-modules jdk.httpserver -cp "lib/mysql-connector-j-9.x.x.jar" -d out src/main/java/com/examsphere/*.java
java --add-modules jdk.httpserver -cp "out:lib/mysql-connector-j-9.x.x.jar" com.examsphere.ExamSphereServer
```

Open **http://localhost:8080** in your browser. Keep the terminal running while using the application.

## 4. Demo accounts

- Admin email: `admin@examsphere.local`
- Admin password: `admin123`
- Student: create an account from the registration tab.

## Database configuration

Defaults target a local XAMPP MySQL installation:

- URL: `jdbc:mysql://localhost:3306/examsphere?useSSL=false&serverTimezone=UTC`
- Username: `root`
- Password: blank

If your MySQL root account has a password or uses another port, set environment variables before starting Java:

```bat
set EXAMSPHERE_DB_URL=jdbc:mysql://localhost:3306/examsphere?useSSL=false&serverTimezone=UTC
set EXAMSPHERE_DB_USER=root
set EXAMSPHERE_DB_PASSWORD=your_password
```

Optional server port: `EXAMSPHERE_PORT=8080`.

## Project structure

```text
ExamSphere/
├── src/main/java/com/examsphere/  Java source files
├── web/                           HTML, CSS, JavaScript
├── database/examsphere.sql        Database schema and sample data
├── lib/                           Put MySQL Connector/J JAR here
├── README.md
├── REQUIREMENTS.md
└── HANDOFF.md
```

## Important scope and security notes

This is a lab/demo application, not a production examination platform. Password hashing uses SHA-256 to keep the example easy to explain; production systems should use salted PBKDF2, BCrypt or Argon2, HTTPS, CSRF protection, secure session expiry, rate limiting, audit logging and stronger input validation. The timer is a browser-side convenience; server-side exam deadlines and attempt limits are not enforced. The in-memory session map is cleared when the Java server stops. Do not deploy this demo on a public network with real student data.

## Troubleshooting

- **`No suitable driver`**: check that the Connector/J JAR is in `lib` and included in both classpaths.
- **Connection refused**: start MySQL in XAMPP and confirm the port.
- **Access denied for root**: configure `EXAMSPHERE_DB_PASSWORD`.
- **Port 8080 already in use**: set `EXAMSPHERE_PORT` to another free port and open that port in the browser.
- **No questions appear**: sign in as admin, create an exam, then add questions; or re-import the SQL seed into a clean database.
