# GitHub Secrets Configuration

This document lists all the secrets that need to be configured in the GitHub repository settings for the automated release workflows to function properly.

## How to Add Secrets

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret listed below

---

## Required Secrets

### Cloudflare R2 (Binary Hosting)

#### `R2_ACCOUNT_ID`
- **Description**: Your Cloudflare account ID
- **How to get**:
  - Log in to Cloudflare Dashboard
  - Copy Account ID from the right sidebar
- **Example**: `abc123def456ghi789jkl012mno345pq`

#### `R2_ACCESS_KEY_ID`
- **Description**: R2 Access Key ID for API access
- **How to get**:
  - Cloudflare Dashboard → R2 → Manage R2 API Tokens
  - Create API token with "Object Read & Write" permissions
  - Copy the Access Key ID
- **Example**: `1234567890abcdef1234567890abcdef`

#### `R2_SECRET_ACCESS_KEY`
- **Description**: R2 Secret Access Key (paired with Access Key ID)
- **How to get**: Displayed when you create the R2 API token (save it immediately, can't retrieve later)
- **Example**: `abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQR`

#### `R2_BUCKET_NAME`
- **Description**: Name of your R2 bucket where binaries will be stored
- **How to get**:
  - Cloudflare Dashboard → R2 → Create bucket or use existing
  - Copy the bucket name
- **Example**: `klaat-releases`

#### `CDN_URL`
- **Description**: Public CDN URL where binaries and web assets can be downloaded (R2 bucket with custom domain)
- **How to get**:
  - Set up custom domain for your R2 bucket in Cloudflare
  - Or use R2.dev subdomain
- **Example**: `https://distribute.kartikarora.me`

#### `CLOUDFLARE_ZONE_ID` *(Optional, for cache purge)*
- **Description**: Cloudflare Zone ID for the domain hosting CDN assets
- **How to get**: Cloudflare Dashboard → Select Domain (`kartikarora.me`) → Overview (bottom right sidebar)

#### `CLOUDFLARE_API_TOKEN` *(Optional, for cache purge)*
- **Description**: Cloudflare API token with `Zone:Cache Purge:Purge` permission
- **How to get**: Cloudflare Dashboard → My Profile → API Tokens → Create Token → Custom token with Zone > Cache Purge > Purge permissions

---

### GitHub Personal Access Tokens

#### `TAP_GITHUB_TOKEN`
- **Description**: Personal Access Token for pushing to `homebrew-tap` repository
- **Required Scopes**: `repo` (Full control of private repositories)
- **How to get**:
  1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
  2. Generate new token (classic)
  3. Name: `Klaat Tap Token`
  4. Select scope: `repo`
  5. Generate and copy the token
- **Example**: `<your-github-personal-access-token>`
- **Note**: This token needs write access to `kartikarora/homebrew-tap`

#### `HOMEBREW_GITHUB_TOKEN`
- **Description**: Personal Access Token for creating PRs to `Homebrew/homebrew-core`
- **Required Scopes**:
  - `repo` (Full control of private repositories)
  - `workflow` (Update GitHub Action workflows)
- **How to get**:
  1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
  2. Generate new token (classic)
  3. Name: `Klaat Homebrew Core Token`
  4. Select scopes: `repo` and `workflow`
  5. Generate and copy the token
- **Example**: `<your-github-personal-access-token>`
- **Note**: This token will be used to fork `homebrew-core` and create PRs

---

### Build Configuration

#### `GOOGLE_CREDENTIALS_JSON`
- **Description**: Complete Google OAuth 2.0 credentials JSON for Google Docs API access
- **How to get**:
  1. Go to [Google Cloud Console](https://console.cloud.google.com)
  2. Create a project or select existing
  3. Enable Google Docs API
  4. Go to APIs & Services → Credentials
  5. Create OAuth 2.0 Client ID (Application type: **Desktop app**)
  6. Download the JSON file (click download icon next to your client ID)
  7. Copy the **entire contents** of the JSON file
- **Format**: The JSON should look like:
  ```json
  {
    "installed": {
      "client_id": "123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com",
      "project_id": "your-project-id",
      "auth_uri": "https://accounts.google.com/o/oauth2/auth",
      "token_uri": "https://oauth2.googleapis.com/token",
      "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
      "client_secret": "YOUR_CLIENT_SECRET",
      "redirect_uris": ["http://localhost"]
    }
  }
  ```
- **Note**: This entire JSON is embedded as a resource in the native binary at build time
- **Security**: Since this is a public desktop app, these credentials identify your app but don't grant access without user consent. Each user must authenticate with their own Google account.

#### `GEMINI_API_KEY` *(Optional, for stable-release AI skills benchmark)*
- **Description**: Gemini Developer API key used by the `eval-skills` job in `native-stable-release.yml` to run live AI skills evaluations and update `docs/docs/eval-report.json`.
- **How to get**: Create a key in [Google AI Studio](https://aistudio.google.com/apikey) on a billing-enabled project.
- **Note**: If omitted, the stable release workflow safely skips the `eval-skills` step.

---

---

### Cloudflare Pages (Documentation Hosting)

#### `CLOUDFLARE_API_TOKEN`
- **Description**: API token for deploying to Cloudflare Pages
- **How to get**:
  1. Cloudflare Dashboard → My Profile → API Tokens
  2. Create Token → Use "Edit Cloudflare Workers" template or create custom
  3. Permissions needed: `Account.Cloudflare Pages:Edit`
  4. Copy the token
- **Example**: `abcdefghijklmnopqrstuvwxyz123456789`

#### `CLOUDFLARE_ACCOUNT_ID`
- **Description**: Your Cloudflare account ID (same as R2 account ID)
- **How to get**: Cloudflare Dashboard → Copy Account ID from right sidebar
- **Example**: `abc123def456ghi789jkl012mno345pq`

---

## Verification Checklist

After adding all secrets, verify:

- [ ] All 10 secrets are added to repository settings
  - `R2_ACCOUNT_ID`
  - `R2_ACCESS_KEY_ID`
  - `R2_SECRET_ACCESS_KEY`
  - `R2_BUCKET_NAME`
  - `CDN_URL`
  - `TAP_GITHUB_TOKEN`
  - `HOMEBREW_GITHUB_TOKEN`
  - `GOOGLE_CREDENTIALS_JSON`
  - `CLOUDFLARE_API_TOKEN`
  - `CLOUDFLARE_ACCOUNT_ID`
- [ ] `TAP_GITHUB_TOKEN` has write access to `homebrew-tap` repo
- [ ] `HOMEBREW_GITHUB_TOKEN` has correct scopes (`repo` + `workflow`)
- [ ] R2 bucket is created and API token has proper permissions
- [ ] CDN URL is configured and publicly accessible
- [ ] Google OAuth credentials JSON is valid and Google Docs API is enabled

---

## Testing Secrets

To test if secrets are properly configured:

1. **Test Snapshot Build**: Push to `develop` branch
   - Should build native binaries
   - Upload to R2
   - Update `homebrew-tap` tap (`Formula/klaat-snapshot.rb`)

2. **Test Stable Release**:
   - Update version in `build.gradle.kts`
   - Merge to `main`
   - Should build, upload to R2, update `homebrew-tap` tap (`Formula/klaat.rb`), create GitHub release, and submit PR to homebrew-core

---

## Security Notes

1. **Never commit secrets to the repository**
2. **Rotate tokens periodically** (every 90 days recommended)
3. **Use minimal scope tokens** - only grant permissions needed
4. **Monitor token usage** in GitHub settings
5. **Revoke tokens immediately** if compromised
6. **Use dedicated OAuth app** - Create a separate Google Cloud project for the CLI tool, don't share credentials with production services

---

## Troubleshooting

### Workflow fails with authentication error
- Check that token has correct scopes
- Verify token hasn't expired
- Ensure token has access to target repository

### R2 upload fails
- Verify R2 bucket exists
- Check API token permissions
- Confirm bucket name matches `R2_BUCKET_NAME`

### Homebrew tap update fails
- Ensure `TAP_GITHUB_TOKEN` has write access
- Verify `homebrew-tap` repository exists
- Check repository name matches workflow configuration

### Homebrew Core PR fails
- Verify `HOMEBREW_GITHUB_TOKEN` has both `repo` and `workflow` scopes
- Check that you haven't exceeded GitHub API rate limits
- Ensure formula follows all Homebrew guidelines
