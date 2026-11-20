# MiniExchange: Day 1 Summary (Thu 19 Nov 2026)

**Goal of the day:** set up the environment and prove Java → JDBC → PostgreSQL works.
**Result:** ✅ Done. A row written by Java was visible in psql and DBeaver.
**Postponed:** Git + `.gitignore` + first commit + GitHub push → next session.

---

## Decisions taken (and why)

| Decision | Why |
|---|---|
| Everything runs in **WSL (Ubuntu)**, driven from the shell | Matches daily workflow; Murex asks for Linux/Bash; CI/Docker later are Linux |
| **Java 21** (LTS) | Long-term support, current standard |
| **Maven** as build tool | Manages libraries, standard build commands, standard layout (like npm for Java) |
| **JDBC** instead of an ORM | Write SQL by hand → really learn SQL (Murex requirement) |
| Postgres for MiniExchange on **port 5433** | Port 5432 is taken by ClinicPing's Docker Postgres |
| Dedicated DB user `miniexchange` (not `postgres`) | Least privilege: app can only touch its own database |
| Password never in code, passed via **environment variables** | Never reaches Git |
| Project name **MiniExchange** = "a small stock exchange" | Clear on CV, honest scope |
| Editor: shell for now (VS Code optional, IntelliJ not required) | Maven project is editor-independent |

---

## 1. Check what's installed

```bash
java -version            # Java runtime installed? (Windows + WSL: not found)
psql --version           # Postgres client installed?  (not found)
Get-Service *postgres*   # PowerShell: Postgres service on Windows? (none)
ls /usr/lib/jvm          # WSL: any JDKs installed? (none)
service postgresql status
```

## 2. Install JDK 21 + Maven

```bash
sudo apt update                           # refresh the list of available packages
sudo apt install -y openjdk-21-jdk maven  # JDK (compiler + runtime) and Maven; -y = auto-yes
java -version                             # runtime version → 21
javac -version                            # compiler version → 21 (proves JDK, not just JRE)
mvn -version                              # Maven version + which Java it uses → 21
```

- **JDK** = Java Development Kit (build + run). **JRE** = Java Runtime Environment (run only). **JVM** = the engine that runs `.class` files.

## 3. Install PostgreSQL

```bash
sudo apt install -y postgresql   # server + psql client; creates Linux user "postgres"
sudo service postgresql start    # start the server
service postgresql status        # "active (exited)" is normal: launcher only
```

- Data lives in `/var/lib/postgresql/16/main/`, **not** in the project folder.
- Projects find the DB by address: `localhost:PORT/dbname`.

## 4. Create the project database and user

```bash
openssl rand -base64 16      # generate a random password
sudo -u postgres psql        # open psql as the superuser
```
```sql
CREATE USER miniexchange WITH PASSWORD '...';     -- ⚠️ every SQL command ends with ;
CREATE DATABASE miniexchange OWNER miniexchange;
\password miniexchange   -- (re)set password safely, not stored in history
\du                      -- list users
\l                       -- list databases
\q                       -- quit psql
```

**Lesson:** prompt `postgres=#` = ready; `postgres-#` = previous command not finished (missing `;`).

## 5. Debug: "password authentication failed"

```bash
pg_lsclusters               # Ubuntu Postgres clusters and their PORT → 5433
sudo ss -ltnp | grep 543    # who listens on ports 543x → docker-proxy on 5432
docker ps                   # running containers → clinicping-db-1 on 5432
psql -h localhost -p 5433 -U miniexchange -d miniexchange -c "SELECT version();"   # ✅
```

**Cause:** `localhost:5432` is ClinicPing's Docker Postgres; ours took **5433**.
**JDBC URL:** `jdbc:postgresql://localhost:5433/miniexchange`

`psql` flags: `-h` host, `-p` port, `-U` user, `-d` database, `-c` run one command.
Pager screen with `(END)` → press `q`.

### Networking basics
- **IP** = which machine (`localhost` / `127.0.0.1` = this machine).
- **Port** = which program on the machine. Only one program per port.
- **TCP** = reliable, ordered connection between two programs (DBs always use it). UDP = faster, no guarantees.
- **Unix socket** = same-machine shortcut through a file; `sudo -u postgres psql` uses it (no port).

### Permissions (`chmod`)
r=4, w=2, x=1, for owner / group / others.
`600` secrets · `644` normal files · `700` private folders · `755` scripts.

## 6. Create the Maven project

```bash
mkdir -p ~/projects/java && cd ~/projects/java
mvn archetype:generate \
  -DgroupId=com.moharajli \
  -DartifactId=miniexchange \
  -Dpackage=com.moharajli.miniexchange \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.5 \
  -DinteractiveMode=false
cd miniexchange
find . -type f     # list all files
cat pom.xml
```

- First run failed with `Connection reset` (slow internet) → simply re-ran; Maven caches downloads in `~/.m2/`.

**Layout:**
```
pom.xml                         project description (like package.json)
src/main/java/...               real code
src/test/java/...               tests (never shipped)
target/                         build output — never edit, never commit
```

### pom.xml changes made
1. `maven.compiler.release` 17 → **21**
2. Removed placeholder `<url>` (example.com)
3. `junit-jupiter-api` + `junit-jupiter-params` → single **`junit-jupiter`** (test scope)
4. Added **`org.postgresql:postgresql:42.7.10`** with `scope=runtime` (code depends only on `java.sql` interfaces → DIP)

Applied with a heredoc: `cat > pom.xml <<'EOF' ... EOF`; verified with `grep -n -E "..." pom.xml`.

## 7. Verify the build

```bash
mvn test
cat target/surefire-reports/com.moharajli.miniexchange.AppTest.txt
# Tests run: 1, Failures: 0, Errors: 0
```

Maven lifecycle: `compile → test → package → install` (each step runs the earlier ones).

## 8. Hello Postgres over JDBC (temporary smoke test)

```bash
export DB_URL=jdbc:postgresql://localhost:5433/miniexchange
export DB_USER=miniexchange
read -s -p "DB password: " DB_PASSWORD; echo; export DB_PASSWORD   # hidden input
echo ${#DB_PASSWORD}     # prints length only (24 expected)

mvn -q compile exec:java -Dexec.mainClass=com.moharajli.miniexchange.HelloPostgres
# → 1: Hello Postgres from Java

psql -h localhost -p 5433 -U miniexchange -d miniexchange -c "SELECT * FROM hello;"
```
Then viewed in **DBeaver** (Host `localhost`, Port `5433`, DB/User `miniexchange`).

**Code review lessons:**
- `System.getenv("DB_URL")` needs quotes (a String name, not a Java variable).
- SQL inside strings is not checked by the compiler → errors only at runtime → tests matter.
- `INSERT INTO table (col) VALUES (...)`, with no `TABLE` keyword and with `VALUES` plural.
- `executeUpdate` for INSERT/UPDATE/DELETE (returns rows changed); `executeQuery` for SELECT.
- `try ( ... )` = try-with-resources → connections always closed.

**Environment variables:** live only in that terminal's memory; visible to you/root only; never on disk or in Git. Remove with `unset DB_PASSWORD`.

## 9. Cleanup (temporary items)

```bash
psql -h localhost -p 5433 -U miniexchange -d miniexchange -c "DROP TABLE hello;"
rm src/main/java/com/moharajli/miniexchange/HelloPostgres.java
unset DB_PASSWORD
find src -type f          # → only App.java and AppTest.java
echo ${#DB_PASSWORD}      # → 0
```

---

## Concepts learned today
JDK / JRE / JVM / JDBC · Maven vs npm · ORM (Prisma, Hibernate) vs JDBC · database vs server vs user · least privilege · ports, TCP, Unix sockets · environment variables & threat model · chmod · Maven layout, `target/`, BOM, scopes · try-with-resources

## Next session
1. `git init`, `.gitignore` (`target/`, IDE files, secrets), first commit, push to GitHub (`mini-exchange`)
2. Day 2: first real JUnit test + architecture sketch (Order, OrderBook, MatchingEngine, Server, ClientHandler)

**Each new terminal:** if Postgres is down → `sudo service postgresql start`.
