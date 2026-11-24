# Google Cloud (GCP) OAuth Credentials Guide

Klaat uses Google OAuth 2.0 to read Google Docs when exporting tutorials from a Google Doc ID.

## When You Need Credentials

- **Required:** Exporting from a Google Doc (`klaat export <google-doc-id>`).
- **Not required:** Exporting from local Markdown files (`klaat export tutorial.md`), `klaat serve`, `klaat demo`, `klaat completion`, and running `./gradlew test`.

> [!NOTE]
> Official pre-built binaries distributed via Homebrew or GitHub Releases already bundle an OAuth client ID at build time.
>
> **Building without a GCP project:** If no `gcp_credentials.json` is present, Gradle automatically packages [`gcp_credentials.fake.json`](gcp_credentials.fake.json) so you can build, test, and compile native binaries without setting up Google Cloud. **This is a fake placeholder credential and cannot authenticate with Google or read any Google Doc** — attempting a Google Docs export with it will fail with an error pointing back to this guide.

---

## Step-by-Step Google Cloud Console Setup

1. **Create or select a Google Cloud project**
   - Open [Google Cloud Console](https://console.cloud.google.com/).
   - Create a new project (e.g., `klaat-local`) or select an existing one.

2. **Enable the Google Docs API**
   - Go to **APIs & Services → Library**.
   - Search for **Google Docs API** and click **Enable**.

3. **Configure the OAuth consent screen**
   - Go to **APIs & Services → OAuth consent screen** (or **Google Auth Platform → Branding / Audience**).
   - Choose **External** (or **Internal** if using a Google Workspace organization).
   - Fill in the required **App name** (e.g., `Klaat`) and **User support email**.
   - Under **Scopes**, add `https://www.googleapis.com/auth/documents.readonly`.
   - Under **Test users** (while the app is in **Testing** status), add the Google email address(es) you will use to authenticate.

4. **Create an OAuth 2.0 Client ID**
   - Go to **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
   - Set **Application type** to **Desktop app**.
   - Give it a name (e.g., `Klaat Desktop`) and click **Create**.

5. **Download or populate `gcp_credentials.json`**
   - Download the JSON file from the confirmation modal (or click the download icon next to your Client ID).
   - Alternatively, copy the template and fill in `client_id`, `project_id`, and `client_secret`:
     ```bash
     cp gcp_credentials.json.template gcp_credentials.json
     ```

---

## Where to Place `gcp_credentials.json`

`GoogleDocsClient` looks for credentials in the following order:

1. **Explicit path** (`credentialsFilePath` passed programmatically or in tests).
2. **Classpath resource** at `src/main/resources/credentials/gcp_credentials.json` (embedded into the JAR / GraalVM native binary at build time).
3. **Project root** at `./gcp_credentials.json` (recommended for local development).

For local development, placing `gcp_credentials.json` in the project root is the simplest option:

```bash
cp ~/Downloads/client_secret_*.json gcp_credentials.json
```

---

## Token Storage & Renewal

- On your first Google Docs export, Klaat opens your default browser for Google sign-in and stores the OAuth token in `~/.klaat/tokens/`.
- Klaat requests `offline` access and automatically refreshes expired tokens silently.
- To sign out or force re-authentication, remove the token directory:
  ```bash
  rm -rf ~/.klaat/tokens
  ```

---

## CI/CD Builds

For automated native release builds, set the `GOOGLE_CREDENTIALS_JSON` repository secret to the full contents of your `gcp_credentials.json` file. The release workflow writes it to `src/main/resources/credentials/gcp_credentials.json` during the build. See [`.github/SECRETS.md`](.github/SECRETS.md) for details.

---

## Security & Secret Scanning

- **Never commit** `gcp_credentials.json` or `~/.klaat/tokens/` to Git.
- Both `gcp_credentials.json` and `tokens/` are listed in `.gitignore`.
- [Gitleaks](.gitleaks.toml) is configured to detect Google OAuth client secrets (`GOCSPX-...`) in commits and pull requests.

---

## Troubleshooting

- **`Error 403: access_denied` during browser login**
  Your OAuth consent screen is in **Testing** mode and your Google account is not listed under **Test users**. Add your email in Google Cloud Console → **OAuth consent screen → Test users**.
- **`Unable to start authentication service. Please try again later.`**
  `gcp_credentials.json` is missing from both `./gcp_credentials.json` and `src/main/resources/credentials/gcp_credentials.json`, or contains invalid JSON.
- **Corrupted or revoked token**
  Run `rm -rf ~/.klaat/tokens` and re-run your `klaat export` command to authenticate again.
