Release a new core version of Azertio (no plugins). All 10 steps: changelog, readme, web, VersionCommand, POMs, maven install, tag, commit+push, GitHub release, Maven Central.

**Usage:** `/bump-core-version OLD NEW`
- `OLD`: previous released version (e.g. `1.2.2`) — used for text-file substitutions
- `NEW`: new release version (e.g. `1.2.3`) — the version being published

If arguments are missing, ask for OLD and NEW before proceeding.

---

Given `$ARGUMENTS`, parse: `OLD` (first word), `NEW` (second word).

Detect the current version in root `pom.xml` (the first `<version>` element that belongs to the project, i.e. around line 15, NOT the wrapper version around line 10). Call it `POM_CURRENT`. It will typically be `OLD-SNAPSHOT` or some later SNAPSHOT.

Get today's date as `DATE` in `YYYY-MM-DD` format.

**Important:** Do ALL file-editing steps before running maven. Confirm each step with a one-line status before moving to the next.

---

## Step 1 — CHANGELOG.md

Run `git log v$OLD..HEAD --oneline` to collect commits since the last release. Group them into relevant sections (Fixed, Added, Security, Dependencies). Keep it concise; only include meaningful user-facing changes.

Insert a new section **above** the first existing `## [` line:

```
## [NEW] - DATE

### Fixed
- VersionCommand was not updated during version bumps (it now tracks the current version automatically)

<other sections derived from git log>

```

Use the Read and Edit tools to modify `CHANGELOG.md`.

---

## Step 2 — README.md

Grep README.md for any occurrences of `OLD` that refer to the azertio version (not third-party plugin examples). If any are found, replace them with `NEW` using the Edit tool. If none are found, skip with a note.

---

## Step 3 — www/index.html

Replace every occurrence of `OLD` with `NEW` in `www/index.html`. This file contains download links, version badges, and CLI output examples.

Use Bash:
```bash
sed -i 's/'"$OLD"'/'"$NEW"'/g' www/index.html
```

Verify with:
```bash
grep -n "$NEW" www/index.html | grep -v "<!--"
```

---

## Step 4 — VersionCommand.java

In `azertio-cli/src/main/java/org/azertio/cli/VersionCommand.java`, replace:
```java
public static final String VERSION = "OLD";
```
with:
```java
public static final String VERSION = "NEW";
```

Use the Edit tool.

---

## Step 5 — Core POMs (no plugins)

Use these three targeted commands. Do NOT use global sed — external deps may share the same version number.

**5a. Root pom.xml own version (first occurrence only):**
```bash
sed -i "0,|<version>$POM_CURRENT</version>|s||<version>$NEW</version>|" pom.xml
```

**5b. Child poms — only inside `<parent>` blocks:**
```bash
find . -name "pom.xml" \
  ! -path "./.git/*" \
  ! -path "*/plugins/*" \
  ! -path "*/target/*" \
  ! -path "./pom.xml" \
  ! -path "./azertio-plugin-starter/pom.xml" \
  -exec sed -i "/<parent>/,/<\/parent>/s|<version>$POM_CURRENT</version>|<version>$NEW</version>|" {} +
```

**5c. azertio-plugin-starter (no parent — own version + property versions):**
```bash
sed -i "s|<version>$POM_CURRENT</version>|<version>$NEW</version>|" azertio-plugin-starter/pom.xml
sed -i "s|<azertio-core.version>$POM_CURRENT</azertio-core.version>|<azertio-core.version>$NEW</azertio-core.version>|" azertio-plugin-starter/pom.xml
sed -i "s|<azertio-docgen.version>$POM_CURRENT</azertio-docgen.version>|<azertio-docgen.version>$NEW</azertio-docgen.version>|" azertio-plugin-starter/pom.xml
```

**5d. Audit — this grep must produce NO output:**
```bash
find . -name "pom.xml" ! -path "./.git/*" ! -path "*/target/*" | \
  xargs grep -n "$NEW" | \
  grep -v "org\.azertio\|azertio-core\.version\|azertio-docgen\.version\|azertio-plugin-starter\|<!--"
```
If any lines appear, stop and show them to the user before continuing.

---

## Step 6 — Maven install

Run from project root:
```bash
./mvnw install -DskipTests
```

Wait for the result. If the build fails, stop and show the error. Do not proceed to later steps until the build is green.

---

## Step 7 — Commit

Stage all modified files and commit:
```bash
git add \
  CHANGELOG.md \
  README.md \
  www/index.html \
  azertio-cli/src/main/java/org/azertio/cli/VersionCommand.java \
  pom.xml \
  azertio-cli/pom.xml \
  azertio-core/pom.xml \
  azertio-docgen-maven-plugin/pom.xml \
  azertio-it/pom.xml \
  azertio-jsonrpc/pom.xml \
  azertio-lsp/pom.xml \
  azertio-persistence/pom.xml \
  azertio-plugin-starter/pom.xml \
  azertio-test-support/pom.xml

git commit -m "build(release): azertio core $NEW"
```

---

## Step 8 — Tag

```bash
git tag v$NEW
```

---

## Step 9 — Push

```bash
git push && git push --tags
```

---

## Step 10 — GitHub Release

Find the distribution ZIP:
```bash
ls azertio-cli/target/azertio-cli-$NEW-dist.zip
```

Extract release notes for `[NEW]` from CHANGELOG.md (the section between `## [$NEW]` and the next `## [`).

Create the GitHub release:
```bash
gh release create v$NEW \
  azertio-cli/target/azertio-cli-$NEW-dist.zip \
  --title "v$NEW" \
  --notes "$(awk '/^## \['"$NEW"'\]/{found=1; next} found && /^## \[/{exit} found{print}' CHANGELOG.md | sed '/^$/d')"
```

---

## Step 11 — Maven Central

Ask the user: **"Please provide your GPG passphrase to deploy to Maven Central:"**

Once provided, run:
```bash
./mvnw deploy -Pmaven-central -DskipTests -Dgpg.passphrase=PASSPHRASE
```
Then:
```bash
cd azertio-plugin-starter && ../mvnw deploy -Pmaven-central -DskipTests -Dgpg.passphrase=PASSPHRASE
```

After both succeed, remind the user:
> Go to https://central.sonatype.com/publishing/deployments and click **Publish** to finalize the release.

---

## Final summary

Print a checklist of all 11 steps with ✓ or ✗ status, and remind the user to update the release history in memory if everything succeeded.