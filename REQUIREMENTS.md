# Requirements

## Software

| Requirement | Version / notes |
|---|---|
| Operating system | Windows 10/11, Linux, or macOS |
| Java JDK | Java 17 or later |
| XAMPP | Any recent release containing MySQL/MariaDB compatible with MySQL Connector/J |
| MySQL Connector/J | 8.4 LTS or 9.x JAR; place in `lib/` |
| Browser | Recent Chrome, Firefox, Edge, or Safari |
| Editor / IDE | VS Code, IntelliJ IDEA Community, Eclipse, or a text editor |
| Build system | None; compile with `javac` |

## Hardware

- 4 GB RAM recommended
- Approximately 100 MB free disk space, excluding XAMPP and JDK
- Internet is only needed to download the JDK/Connector JAR and load the optional Google Fonts stylesheet. The application itself runs locally.

## Java modules

- `jdk.httpserver` is used for the lightweight built-in HTTP server.
- `java.sql`, `java.net`, `java.nio`, and standard security APIs are part of the JDK.

## Functional requirements

1. A student can register with a name, email, and password.
2. A student or administrator can sign in using the correct role.
3. An administrator can create an examination.
4. An administrator can add MCQ questions and mark the correct option.
5. A student can view active examinations and answer MCQs.
6. The application calculates marks on the Java server.
7. The application stores each attempt and its selected answers.
8. A student can view their own result history.

## Non-functional requirements

- Responsive, readable interface using pink, white, and light blue.
- JDBC prepared statements for user-supplied SQL values.
- Separation of model, service, database and HTTP responsibilities.
- Clear setup instructions and no framework/build-tool dependency.
