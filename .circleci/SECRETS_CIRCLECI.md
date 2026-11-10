# CircleCI Environment Variables Configuration

This document lists all the environment variables that need to be configured in your CircleCI project settings for the automated snapshot release and tap update to function properly.

## How to Add Environment Variables

1. Go to your project on [CircleCI Dashboard](https://app.circleci.com/).
2. Navigate to **Project Settings** → **Environment Variables**.
3. Click **Add Environment Variable** for each variable listed below.

---

## Required Environment Variables

### Cloudflare R2 (Binary Hosting)

#### `R2_ACCOUNT_ID`
- **Description**: Your Cloudflare account ID.
- **Where to find**: Cloudflare Dashboard → R2 → Account ID (right sidebar).
- **Example**: `abc123def456ghi789jkl012mno345pq`

#### `R2_ACCESS_KEY_ID`
- **Description**: R2 Access Key ID for API access.
- **Where to find**: Cloudflare Dashboard → R2 → Manage R2 API Tokens. Create a token with "Object Read & Write" permissions.
- **Example**: `1234567890abcdef1234567890abcdef`

#### `R2_SECRET_ACCESS_KEY`
- **Description**: R2 Secret Access Key (paired with Access Key ID).
- **Where to find**: Displayed once when you create the R2 API token.
- **Example**: `abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQR`

#### `R2_BUCKET_NAME`
- **Description**: Name of your R2 bucket where binaries will be stored.
- **Example**: `klaat-releases`

#### `CDN_URL`
- **Description**: Public CDN URL for binary downloads (normalized to remove trailing slashes).
- **Example**: `https://distribute.kartikarora.me`

---

### GitHub Personal Access Token

#### `TAP_GITHUB_TOKEN`
- **Description**: Personal Access Token (PAT) for updating the Homebrew tap and creating GitHub snapshot releases.
- **Required Permissions**:
    - **Classic PAT**: `repo` scope (Full control of private repositories).
    - **Fine-Grained PAT**: 
        - **Repository access**: "Only select repositories"
        - **Repositories**: `klaat` (main repo) AND `homebrew-tap` (tap repo)
        - **Permissions**: `Contents: Read & Write` (needed to push tags and update formulas).
- **Example**: `ghp_abc123def456ghi789jkl012mno345pqr678stu`

---

### Build Configuration

#### `GOOGLE_CREDENTIALS_JSON`
- **Description**: Complete Google OAuth 2.0 credentials JSON file contents.
- **Format**: 
  ```json
  {
    "installed": {
      "client_id": "...",
      "client_secret": "...",
      "redirect_uris": ["http://localhost"],
      "auth_uri": "...",
      "token_uri": "..."
    }
  }
  ```
- **Note**: This entire JSON is embedded into the native binary at build time to allow Google Docs API access.

---

## Verification Checklist

Before triggering a snapshot release, ensure:
- [ ] `TAP_GITHUB_TOKEN` has write access to BOTH the `klaat` repository and the `homebrew-tap` repository.
- [ ] R2 API Token has "Object Read & Write" permissions.
- [ ] All 7 variables are added to CircleCI Project Settings.
- [ ] `GOOGLE_CREDENTIALS_JSON` contains valid OAuth 2.0 credentials.

## Triggering the Pipeline

Since the snapshot release is manual, you must trigger it via the CircleCI UI or API with the parameter `run_snapshot` set to `true`.

### Via Web UI:
1. Go to your project on CircleCI.
2. Click **Trigger Pipeline**.
3. Add a **Pipeline Parameter**:
    - Name: `run_snapshot`
    - Type: `boolean`
    - Value: `true`
4. Click **Trigger Pipeline**.
