# Phase 0 — Environment Audit and Toolchain Decisions

Project: Smart Food Delivery Management System
Stack: Java 21 + Spring Boot 3.5 + Maven (backend), React + Vite + Tailwind (frontend), MySQL 8

## 1. Purpose and exit criteria

Phase 0 establishes a working, reproducible development environment before any
application code is written. It removes the risk that later failures (build
errors, wrong bytecode level, missing build tool) are misdiagnosed as code
defects.

Exit criteria: a single repeatable `doctor` check at `scripts/doctor.ps1`
prints one line per component with an explicit
`PASS` / `FAIL` result and exits with code 0 only when every component passes.
The check must be re-runnable by any team member on any machine and must read
its values from the repository-local toolchain, not from global machine state.

## 2. Audit results

Audit performed on the student's machine, Windows 11, before any changes.

| Component | Required | Found | Status | Action taken |
|---|---|---|---|---|
| Operating system | Windows 10/11 x64 | Windows 11 Home Single Language, build 10.0.26300.0 | PASS | None |
| JDK | JDK 21 LTS | `PATH` resolves to the `javapath` shim; `java -version` gave no captured output. On disk: `jdk-26.0.2.1`, `jdk-26.0.1`; `javac -version` reports 26.0.2.1. `JAVA_HOME` empty. | WARN | Pin JDK 21 GA (Temurin, windows x64) to `.tools/jdk21/` |
| Maven | Apache Maven 3.9.x | Not installed; `where.exe mvn` and `mvn -v` returned nothing. `%USERPROFILE%\.m2` holds `repository/` and `wrapper/`. | BLOCKED | Pin Maven 3.9.16 (latest 3.9.x) to `.tools/maven/` |
| Node.js / npm | Node.js 20 LTS or newer | Node.js v24.14.0 at `C:\Program Files\nodejs\node.exe`; npm 11.9.0 | PASS | None |
| Git | Recent Git for Windows | 2.52.0.windows.1; user `Ayush Ekka`, `ayushekka915@gmail.com` | PASS | None |
| MySQL | MySQL 8 server and client | Client and server `Ver 8.0.45 for Win64`; service `MySQL80` Running; TCP 3306 accepts connections; config `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini` | WARN | Per-developer credentials; create least-privilege app user |
| Docker | Optional for Phase 0 | Not installed | PASS | Deferred (see risks) |
| Package managers | At least one | winget 1.29.380, Chocolatey 2.6.0; Scoop not installed | PASS | None |
| Ports | 5173 and 8080 free | Both free | PASS | None |
| Disk space | Sufficient for toolchain and dependencies | D: approximately 89.7 GB free | PASS | None |
| Git repository | Initialised at project root | Not yet a git repository (expected at audit time) | PASS | `git init` deferred to Phase 0 close-out |

## 3. Findings in plain language

**The `javapath` shim.** `C:\Program Files\Common Files\Oracle\Java\javapath\`
is not a Java installation. It holds small trampoline executables that forward to
whichever Oracle JRE the installer last registered. Because that directory sits
early on `PATH`, `java -version` can report one JDK while your compiler, build
tool or IDE uses another. It also explains the empty audit output: the launcher
forwarded the call and the banner was not captured. Calling the executable by
full path inside the real JDK removes the ambiguity.

**Why `JAVA_HOME` matters.** Maven does not use `PATH` to find Java. It reads
`JAVA_HOME` and runs `$JAVA_HOME\bin\java.exe`. With `JAVA_HOME` empty, Maven
either fails with "JAVA_HOME not found" or falls back to an unintended JDK. Since
the project must be built with Java 21, `JAVA_HOME` is the single switch deciding
which JDK compiles the code.

**Why JDK 26 is not the target.** The only JDK on disk is 26, newer than the JDK
21 baseline Spring Boot 3.5 is validated against. Newer JDKs remove and change
behaviour that libraries rely on, and class files compiled by 26 will not run on a
JDK 21 machine such as a college lab PC. Building with 21 keeps the artifact
portable and the reported results trustworthy.

**Why the empty root password failing is good news.** The audit tried `root` with
no password and MySQL rejected it (exit code 1), so a root password is set, which
is the safe configuration. The consequence is procedural, not a defect: the build
cannot use root, so the project must create its own application user.

## 4. Remediation — project-local toolchain (PowerShell)

Decision: pin a portable, gitignored toolchain inside the repository —
Temurin JDK 21 GA at `.tools/jdk21/` and Apache Maven 3.9.16 at `.tools/maven/`.
Rationale: the machine only has JDK 26 (newer than the validated target), Maven
is absent, a system-wide install needs administrator rights, and a
repository-local toolchain makes the college lab machine reproducible.

Run these steps in a PowerShell window opened at the repository root.

1. Record the repository path for this session:
   `$Repo = "D:\MAin project\Dsa Projects"`
2. Point `JAVA_HOME` at the pinned JDK and prepend its `bin` to `PATH`:
   `$env:JAVA_HOME = "$Repo\.tools\jdk21"`
   `$env:Path = "$env:JAVA_HOME\bin;$env:Path"`
3. Verify by absolute path, bypassing the shim:
   `& "$env:JAVA_HOME\bin\java.exe" -version`
   Expect an `openjdk version "21..."` banner. `java -version` writes to stderr,
   not stdout; that is normal. To capture it in a variable for the `doctor`
   script use `& "$env:JAVA_HOME\bin\java.exe" -version 2>&1`.
4. Point Maven at the pinned directory and add its `bin` to `PATH`:
   `$env:MAVEN_HOME = "$Repo\.tools\maven"`
   `$env:Path = "$env:MAVEN_HOME\bin;$env:Path"`
5. Verify with `mvn -v`. The real proof is the `Java version:` and `Java home:`
   lines, which must show 21 and `.tools\jdk21`. If `mvn -v` shows another Java,
   `JAVA_HOME` is not honoured and must be fixed first.
6. These settings last for the current session only. The `doctor` script sets them
   on every run, so results never depend on persisted machine state.

## 5. MySQL setup

1. Log in as an administrator account that knows the root password:
   `mysql -u root -p`
2. Create the database:

   ```sql
   CREATE DATABASE food_delivery CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
3. Create a dedicated application user with a strong, unique password:

   ```sql
   CREATE USER 'foodapp'@'localhost' IDENTIFIED BY '<strong-password>';
   ```
4. Grant only what the application needs, on this database only:

   ```sql
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON food_delivery.* TO 'foodapp'@'localhost';
   FLUSH PRIVILEGES;
   ```
5. Test the new account: `mysql -u foodapp -p -D food_delivery`
6. Store the password outside the repository, in an environment variable or a
   gitignored `.env` file.

Using `root` for the application is bad practice: root can drop other schemas,
change server configuration and read every database, so one bad query or one
leaked config file compromises the whole server. A scoped user limits the damage.
Credentials must never be committed; a gitignored `.env` keeps history clean and
lets each developer use their own password.

## 6. Verification checklist

| Check | Command | Expected |
|---|---|---|
| Toolchain present | `& "$Repo\.tools\jdk21\bin\java.exe" -version` | `openjdk version "21...` |
| Maven on session `PATH` | `mvn -v` | `Apache Maven 3.9.16`, `Java version: 21...`, `Java home: ...\.tools\jdk21` |
| Node.js | `node -v` | `v24.14.0` |
| npm | `npm -v` | `11.9.0` |
| Git | `git --version` | `git version 2.52.0.windows.1` |
| MySQL client | `mysql --version` | `Ver 8.0.45 for Win64` |
| MySQL service | `Get-Service MySQL80` | `Running` |
| MySQL reachable | `Test-NetConnection localhost -Port 3306` | `TcpTestSucceeded : True` |
| App database | `mysql -u foodapp -p -D food_delivery` | login succeeds, `food_delivery` selected |
| Ports free | `Get-NetTCPConnection -LocalPort 5173,8080 -ErrorAction SilentlyContinue` | no output |
| `doctor` | `.\scripts\doctor.ps1` | every component `PASS`, exit code 0 |

## 7. Known risks and deferred items

- **Docker is absent.** Testcontainers-based integration tests cannot run. Fall
  back to H2 in MySQL compatibility mode, or install Docker Desktop later and
  re-enable Testcontainers. H2/MySQL behavioural differences are accepted for now.
- **JDK 26 stays installed but unused.** Not removed; the project never references
  it, and the `doctor` check enforces the pinned JDK 21.
- **MySQL credentials are per developer.** Each environment gets its own `foodapp`
  password; the shared report never contains a real credential.
- **Session-scoped variables.** A new terminal loses `JAVA_HOME` unless the steps
  are re-run; the `doctor` script is the mitigation.

## 8. What Phase 0 does NOT cover

No application code, no database schema or migrations, no frontend source, and no
dependency download or build. Phase 0 ends when the `doctor` check passes on the
target machines; everything above is environment scaffolding only.

## 9. Post-remediation verification (actual results)

Run on the target machine after the remediation steps above. These are measured
results, not expectations.

| Item | Result |
|---|---|
| JDK 21 (Temurin) downloaded to `.tools/jdk21/` | `openjdk version "21.0.13" 2026-10-20 LTS`, `Temurin-21.0.13+9`, `javac 21.0.13` |
| Maven downloaded to `.tools/maven/` | `Apache Maven 3.9.16` |
| Maven's runtime JDK | `Java version: 21.0.13`, `runtime: ...\.tools\jdk21` — `JAVA_HOME` is honoured |
| `scripts/env.ps1` dot-sourced | returns cleanly, prints both versions, does not close the shell |
| `scripts/doctor.ps1` | **8 passed, 1 warning, 0 failed**, exit code 0 |
| Only warning | `.env` missing at repo root — expected until the database is created |
| Git repository | initialised, branch `main`, two Phase 0 commits, working tree clean |
| Files committed | 37 before cleanup; the unrelated `TicketBookingSystem/` folder is now gitignored and untracked (still on disk) |

Note on the Maven version: the plan originally pinned 3.9.9, but that archive has
been removed from the Apache CDN (HTTP 404). 3.9.16 is the current 3.9.x release
and is what `.tools/maven/` now contains. The `doctor` check therefore validates
"Maven 3.9.x running on Java 21" rather than one exact patch version.

### Remaining Phase 0 action (requires the MySQL root password)

This is the only step that cannot be automated from the build side, because the
administrator password is not, and must not be, stored in the repository:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p `
  --default-character-set=utf8mb4 `
  -e "source scripts/create-database.sql"
```

Before running it, edit `scripts/create-database.sql` and replace both
`CHANGE_ME_strong_password` occurrences with a real password, then copy
`.env.example` to `.env` and set `DB_PASSWORD` to the same value. After that,
`.\scripts\doctor.ps1` reports 9 passed, 0 warnings, 0 failed.

