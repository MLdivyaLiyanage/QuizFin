# QuizFin
QuizFin is a Java Swing desktop game with a Fish Memory Challenge and an optional
general knowledge quiz followed by matching fish rounds.

The Java Swing desktop interface starts at `com.perisic.fish.peripherals.LoginGUI`.
The dashboard explains both routes: Quiz + Fish combines a knowledge quiz with
matching fish rounds; Fish Challenge starts a standalone memory challenge.
Forms show inline feedback and run sign-in and registration in the background.
Use Tab to move between controls and Enter to sign in. Resize the main window;
pages scroll when needed to keep controls reachable. Leaving an unfinished
challenge through navigation or logout asks before discarding progress.

To compile and launch from the repository root in PowerShell:

```powershell
$sources = Get-ChildItem QuizFin/src -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -cp 'lib/*' -d target/classes $sources
java -cp 'target/classes;QuizFin/src;lib/*' com.perisic.fish.peripherals.LoginGUI
```

Passwords are stored in `users.password_hash` using jBCrypt 0.4 with a random
salt and cost 12. The IntelliJ module includes `lib/jbcrypt-0.4.jar`; command-line
builds and runs must include `lib/*` on the classpath.

Existing PBKDF2 accounts are upgraded after a successful password login.
Legacy passwords longer than 72 UTF-8 bytes remain readable as PBKDF2; they need
a password change to a value within bcrypt's limit before they can be upgraded.
New registrations reject passwords above that limit. The existing TEXT database
column needs no schema change.

To compile and check password hashing from PowerShell:

```powershell
$sources = Get-ChildItem QuizFin/src -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -cp 'lib/*' -d target/bcrypt-test $sources QuizFin/tests/com/perisic/fish/auth/PasswordHasherTest.java
java -cp 'target/bcrypt-test;lib/*' com.perisic.fish.auth.PasswordHasherTest
```

Assessment review and meeting preparation: see [the lecturer guide](LECTURER_MEETING_GUIDE.md)
and [source acknowledgements](SOURCES.md). These are study notes, not an assessed video.
New accounts must be registered through the UI; startup no longer creates an account
with a hardcoded password. Existing accounts remain available.

To compile and run the regression checks from the repository root:

```powershell
$sources = Get-ChildItem QuizFin/src,QuizFin/tests -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -cp 'lib/*' -d target/assessment-check $sources
java -cp 'target/assessment-check;lib/*' com.perisic.fish.engine.GameFlowTest
java -cp 'target/assessment-check;lib/*' com.perisic.fish.auth.PasswordHasherTest
java -cp 'target/assessment-check;lib/*' com.perisic.fish.auth.SessionManagerTest
java '-Djava.awt.headless=true' -cp 'target/assessment-check;lib/*' com.perisic.fish.peripherals.QuizButtonUiTest
```

Optional checks require local PostgreSQL or internet access, respectively:

```powershell
java -cp 'target/assessment-check;lib/*' com.perisic.fish.auth.AuthServiceIntegrationTest
java -cp 'target/assessment-check;lib/*' com.perisic.fish.engine.LiveApiSmokeTest
```

The database test reads `db.properties` and environment overrides, creates an
isolated random schema, and removes it afterward. Its database role needs permission
to create schemas. It uses temporary session files under `target/`, not the player's
normal session directory. If `QUIZFIN_DB_URL` is set, its leaderboard checks are
skipped to avoid the production loader overriding the isolated test configuration.
