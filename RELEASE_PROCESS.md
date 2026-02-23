# Release Process

## Development Workflow

- All development happens in the `develop` branch
- Push changes to `develop` for ongoing work
- The `develop` branch is the default branch

## Snapshot Releases (Automated)

Snapshot releases happen automatically on every push to `develop`:

1. GitHub Actions builds **native binaries** for all platforms using GraalVM:
   - `klaat-macos-arm64-snapshot-{hash}`
   - `klaat-macos-x64-snapshot-{hash}`
   - `klaat-linux-arm64-snapshot-{hash}`
   - `klaat-linux-x64-snapshot-{hash}`

2. Binaries are standalone executables (no JRE required)
3. Uploaded to Cloudflare R2 CDN at: `snapshots/{hash}/`
4. `homebrew-klaat-snapshots` tap is automatically updated

Users can install snapshots via:
```bash
brew tap kartikarora/klaat-snapshots
brew install klaat-snapshot
```

Or download directly from CDN and run (no Java needed).

## Stable Releases (Automated)

Stable releases happen automatically when you push to `main`:

### 1. Prepare the Release

```bash
# Ensure you're on develop with latest changes
git checkout develop
git pull origin develop

# Create a release branch
git checkout -b release/v1.0.0
```

### 2. Update Version in build.gradle.kts

```kotlin
version = "1.0.0"  // Update this line
```

Also update version references in `README.md` if needed.

### 3. Merge to Main

```bash
# Merge to main
git checkout main
git merge release/v1.0.0

# Push to main (this triggers the stable release workflow)
git push origin main
```

### 4. GitHub Actions Handles Everything

The stable release workflow (`native-stable-release.yml`) automatically:
- Reads version from `build.gradle.kts`
- Builds **native binaries** for all platforms using GraalVM
  - macOS ARM64, macOS x64, Linux ARM64, Linux x64
- Uploads binaries to Cloudflare R2 at `releases/v{version}/`
- **Creates git tag** `v{version}` and pushes it
- **Creates GitHub Release** with download URLs and release notes
- Generates SHA256 checksums
- Binaries are standalone executables (no JRE required)

**No manual tagging needed!** The workflow handles tags and releases.

### 5. Merge Back to Develop

```bash
git checkout develop
git merge main
git push origin develop
```

### 6. Clean Up

```bash
git branch -d release/v1.0.0
```

### 7. Homebrew Core Submission (Automatic)

The workflow **automatically submits a PR to Homebrew Core** that:
- Forks `Homebrew/homebrew-core`
- Creates a properly formatted formula in `Formula/k/klaat.rb`
- Follows all Homebrew guidelines (commit style, formula structure, etc.)
- Runs `brew audit --strict --online`
- Tests the formula with `brew install` and `brew test`
- Creates a PR with proper checklist and description

You'll need to monitor the PR for:
- CI test results
- Maintainer feedback
- Any requested changes

Once approved and merged, users can install with: `brew install klaat`

## Required GitHub Secrets

For automated releases, configure these secrets in your repository:

### Cloudflare R2 (Binary Hosting)
- `R2_ACCOUNT_ID`: Your Cloudflare account ID
- `R2_ACCESS_KEY_ID`: R2 access key
- `R2_SECRET_ACCESS_KEY`: R2 secret key
- `R2_BUCKET_NAME`: R2 bucket name
- `CDN_URL`: Your CDN URL (e.g., `https://cdn.yourdomain.com`)

### GitHub Tokens
- `TAP_GITHUB_TOKEN`: Personal Access Token with `repo` scope for pushing to `homebrew-klaat-snapshots`
- `HOMEBREW_GITHUB_TOKEN`: Personal Access Token with `repo` and `workflow` scope for creating PRs to `homebrew-core`

### Build Configuration
- `GOOGLE_OAUTH_CLIENTID`: Google OAuth Client ID (embedded in native binaries at build time)
- `GOOGLE_OAUTH_CLIENTSECRET`: Google OAuth Client Secret (embedded in native binaries at build time)

## CDN Structure

```
/snapshots/{commit-hash}/
  - klaat-macos-arm64-snapshot-{hash}
  - klaat-macos-x64-snapshot-{hash}
  - klaat-linux-arm64-snapshot-{hash}
  - klaat-linux-x64-snapshot-{hash}
  - checksums-snapshot-{hash}.txt

/releases/{version}/
  - klaat-macos-arm64-{version}
  - klaat-macos-x64-{version}
  - klaat-linux-arm64-{version}
  - klaat-linux-x64-{version}
  - checksums-{version}.txt
```

All binaries are **native executables** built with GraalVM - no JRE required to run them!
