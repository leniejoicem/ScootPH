# ScootPH

A desktop payroll and HR system for a scooter company. It covers attendance, leave, payslips,
employee records and user access, all in one app with role-based access control.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 23 |
| Build | Apache Maven |
| IDE | Apache NetBeans |
| Database | PostgreSQL 16 with JDBC (`org.postgresql` 42.7) |
| User interface | Java Swing with [FlatLaf](https://www.formdev.com/flatlaf/) 3.5 and SVG graphics (`flatlaf-extras`) |
| Date picker | JCalendar 1.4 |
| Reports | JasperReports 7.0 with PDF export |
| Password hashing | jBCrypt (BCrypt) |
| Two-factor authentication | GoogleAuth (TOTP) and ZXing for QR codes |
| Input validation | Hibernate Validator 8 (Jakarta Bean Validation) with validation groups |
| Input sanitising | OWASP Java HTML Sanitizer |
| Code quality | SonarQube scanner for Maven |

## Architecture

```
com.payroll
├── ScootPH          entry point
├── UI               Swing screens
│   ├── auth         sign in, sign up, forgot password, two-factor setup
│   ├── pages        one class per screen
│   ├── kit          reusable components: responsive grid, cards, forms, tables, toasts
│   └── theme        colours, fonts, FlatLaf setup
├── service          business rules, validation and transactions
├── security         roles, permissions and access control (RBAC)
├── DAO              SQL through JDBC prepared statements
├── domain           data classes
├── subdomain        lookup values
├── validation       validation groups, password rules, sanitising
└── util             database connection, schema migrations, 2FA
```

Each layer only talks to the one below it: screens call services, services call DAOs, and DAOs
run SQL.

* **Layered design with object-oriented principles:** the UI holds no business rules. Services
  validate input and report errors per field, so forms show each error under the right input.
* **RBAC:** four roles (Employee, HR, Finance, IT), each mapped to a set of permissions. The
  menu shows only the pages a role may use, and every navigation is checked again.
* **Transactions:** multi-step changes such as adding an employee or approving leave either
  fully happen or not at all.
* **Background work:** database calls run on one worker thread, so the window never freezes.
* **Schema migrations:** versioned SQL scripts in `src/main/resources/db/migration` run once on
  start-up and are recorded in `scootph_schema_version`.
* **Responsive layout:** pages reflow from three columns to one as the window narrows.
* **Reports:** Jasper templates (`.jrxml`) for payslips, timecards, employee profiles and
  masterlists. They can be previewed, printed or saved as PDF.

## Security

* Passwords are stored as BCrypt hashes.
* Optional two-factor authentication works with Google or Microsoft Authenticator.
* After 5 failed sign-ins, that username is locked for 5 minutes.
* Sessions end after 15 minutes without activity.
* All SQL uses prepared statements, and free-text input is sanitised.

## Running it

Requirements: JDK 23, PostgreSQL 16, and NetBeans or Maven.

1. Restore the database into a database named `scootph_db`:
   ```bash
   createdb -U postgres scootph_db
   pg_restore -U postgres -d scootph_db --no-owner scootph_db.dump
   ```
2. Set the connection in `src/main/resources/config/db.properties`:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/scootph_db
   db.username=postgres
   db.password=yourpassword
   ```
   Or set `SCOOTPH_DB_URL`, `SCOOTPH_DB_USER` and `SCOOTPH_DB_PASSWORD` instead. They take
   priority over the file.
3. Run it from NetBeans (main class `com.payroll.ScootPH`) or with `mvn compile exec:java`.
