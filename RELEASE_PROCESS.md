# Release Process

## Development Workflow

- All development happens in the `develop` branch
- Push changes to `develop` for ongoing work
- The `develop` branch is the default branch

## Snapshot Releases (Automated)

Snapshot releases happen automatically on every push to `develop`:

1. GitHub Actions builds **native binaries** for all platforms using GraalVM:
   - `klaat-snapshot-macos-arm64-{hash}`
   - `klaat-snapshot-linux-arm64-{hash}`
   - `klaat-snapshot-linux-x64-{hash}`

2. Binaries are standalone executables (no JRE required)
3. Uploaded to Cloudflare R2 CDN at: `snapshots/{hash}/`
4. `homebrew-tap` repository is automatically updated with the latest formula (`Formula/klaat-snapshot.rb`)

Users can install snapshots on macOS or Linux via:
```bash
brew install kartikarora/tap/klaat-snapshot
```

Or download directly from CDN and run (no Java needed).

## Stable Releases (1.0.0 onwards)

Stable releases happen automatically when changes are merged to the `main` branch.

### 1. Prepare the Release

1. Make sure you are on the `develop` branch:
   ```bash
   git checkout develop
   git pull origin develop
   ```

2. Update the `klaat` version in `gradle/libs.versions.toml`:
   ```toml
   [versions]
   klaat = "1.0.0"
   ```

3. Commit and push the version bump to `develop`:
   ```bash
   git add gradle/libs.versions.toml
   git commit -m "bump: version 1.0.0"
   git push origin develop
   ```

### 2. Merge to Main

Raise a Pull Request from `develop` to `main` on GitHub, or merge directly:
```bash
git checkout main
git pull origin main
git merge develop
git push origin main  # This triggers the stable release workflow
```

### 3. GitHub Actions Handles Everything

The stable release workflow (`native-stable-release.yml`) automatically:
- Reads version from `build.gradle.kts`
- Builds **native binaries** for all platforms using GraalVM
  - macOS ARM64, macOS x64 (Currently not built), Linux ARM64, Linux x64
- Uploads binaries to Cloudflare R2 at `releases/v{version}/`
- **Creates git tag** `v{version}` and pushes it
- **Creates GitHub Release** with download URLs and release notes
- Generates SHA256 checksums
- Binaries are standalone executables (no JRE required)

**No manual tagging needed!** The workflow handles tags and releases.

### Required GitHub Secrets

For automated releases, configure these secrets in your repository:

### Cloudflare R2 (Binary Hosting)
- `R2_ACCOUNT_ID`: Your Cloudflare account ID
- `R2_ACCESS_KEY_ID`: R2 access key
- `R2_SECRET_ACCESS_KEY`: R2 secret key
- `R2_BUCKET_NAME`: R2 bucket name
- `CDN_URL`: Your CDN URL (e.g., `https://cdn.yourdomain.com`)

### GitHub Tokens
- `TAP_GITHUB_TOKEN`: Personal Access Token with `repo` scope for pushing to `kartikarora/homebrew-tap` (updating `klaat.rb` and `klaat-snapshot.rb`)

### Build Configuration
- `GOOGLE_CREDENTIALS_JSON`: Complete Google OAuth 2.0 credentials JSON (embedded in native binaries at build time)

## CDN Structure

```
/snapshots/{commit-hash}/
  - klaat-snapshot-macos-arm64-{hash}
  - klaat-snapshot-macos-x64-{hash}
  - klaat-snapshot-linux-arm64-{hash}
  - klaat-snapshot-linux-x64-{hash}
  - checksums-snapshot-{hash}.txt

/releases/{version}/
  - klaat-macos-arm64-{version}
  - klaat-macos-x64-{version}
  - klaat-linux-arm64-{version}
  - klaat-linux-x64-{version}
  - checksums-{version}.txt
```

All binaries are **native executables** built with GraalVM - no JRE required to run them!
