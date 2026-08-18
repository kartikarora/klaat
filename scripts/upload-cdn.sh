#!/usr/bin/env bash
#
# scripts/upload-cdn.sh
# Uploads Klaat CDN assets (CSS and JS) to Cloudflare R2 using wrangler CLI.
# Checks for differences and timestamp recency against the remote CDN before uploading.
# Purges Cloudflare cache everywhere if an API token is provided.
# Generates a GitHub Actions Step Summary when run in CI.
#
# Usage:
#   ./scripts/upload-cdn.sh [OPTIONS]
#
# Options:
#   -b, --bucket NAME     Cloudflare R2 bucket name (default: $R2_BUCKET_NAME or "distribute-cdn")
#   -t, --token TOKEN     Cloudflare API Token (optional, for cache purge)
#   -z, --zone-id ID      Cloudflare Zone ID (optional, for cache purge)
#   -m, --minify          Minify assets before checking/uploading (requires clean-css-cli and terser)
#   -f, --force           Force upload all assets even if no changes or remote is newer
#   -p, --purge           Purge Cloudflare CDN cache everywhere (requires API token or CI secret)
#   -d, --dry-run         Print the wrangler commands without executing them
#   -h, --help            Show this help message
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ASSETS_DIR="${ROOT_DIR}/src/main/resources/assets"

BUCKET="${R2_BUCKET_NAME:-distribute-cdn}"
API_TOKEN="${CLOUDFLARE_API_TOKEN:-${CF_API_TOKEN:-}}"
ZONE_ID="${CLOUDFLARE_ZONE_ID:-${CF_ZONE_ID:-}}"
DO_MINIFY=false
FORCE_UPLOAD=false
DO_PURGE=false
DRY_RUN=false
CDN_URL="${CDN_URL:-https://distribute.kartikarora.me}"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -b|--bucket)
      BUCKET="$2"
      shift 2
      ;;
    -t|--token)
      API_TOKEN="$2"
      shift 2
      ;;
    -z|--zone-id)
      ZONE_ID="$2"
      shift 2
      ;;
    -m|--minify)
      DO_MINIFY=true
      shift
      ;;
    -f|--force)
      FORCE_UPLOAD=true
      shift
      ;;
    -p|--purge)
      DO_PURGE=true
      shift
      ;;
    -d|--dry-run)
      DRY_RUN=true
      shift
      ;;
    -h|--help)
      grep '^#' "$0" | cut -c 3-
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Klaat CDN Asset Uploader (Wrangler)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Target Bucket: ${BUCKET}"
echo "  CDN Base URL:  ${CDN_URL}"
echo "  Assets Source: ${ASSETS_DIR}"
echo "  Minification:  ${DO_MINIFY}"
echo "  Force Upload:  ${FORCE_UPLOAD}"
echo "  Purge Cache:   ${DO_PURGE}"
echo "  Dry Run:       ${DRY_RUN}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Temporary directory for prepared files
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TEMP_DIR}"' EXIT

CSS_SRC="${ASSETS_DIR}/klaat-elements.css"
JS_SRC="${ASSETS_DIR}/klaat-elements.js"
BRIDGE_SRC="${ASSETS_DIR}/klaat-bridge.js"

CSS_TARGET="${TEMP_DIR}/klaat-elements.css"
JS_TARGET="${TEMP_DIR}/klaat-elements.js"
BRIDGE_TARGET="${TEMP_DIR}/klaat-bridge.js"

if [[ "${DO_MINIFY}" == true ]]; then
  echo "📦 Minifying assets..."
  if command -v cleancss >/dev/null 2>&1; then
    cleancss -o "${CSS_TARGET}" "${CSS_SRC}"
  elif npx --yes clean-css-cli -v >/dev/null 2>&1; then
    npx --yes clean-css-cli -o "${CSS_TARGET}" "${CSS_SRC}"
  else
    echo "⚠️  cleancss not found, copying CSS unminified."
    cp "${CSS_SRC}" "${CSS_TARGET}"
  fi

  if command -v terser >/dev/null 2>&1; then
    terser "${JS_SRC}" -c -m -o "${JS_TARGET}"
    terser "${BRIDGE_SRC}" -c -m -o "${BRIDGE_TARGET}"
  elif npx --yes terser -v >/dev/null 2>&1; then
    npx --yes terser "${JS_SRC}" -c -m -o "${JS_TARGET}"
    npx --yes terser "${BRIDGE_SRC}" -c -m -o "${BRIDGE_TARGET}"
  else
    echo "⚠️  terser not found, copying JS unminified."
    cp "${JS_SRC}" "${JS_TARGET}"
    cp "${BRIDGE_SRC}" "${BRIDGE_TARGET}"
  fi
else
  cp "${CSS_SRC}" "${CSS_TARGET}"
  cp "${JS_SRC}" "${JS_TARGET}"
  cp "${BRIDGE_SRC}" "${BRIDGE_TARGET}"
fi

calc_hash() {
  local file="$1"
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "${file}" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${file}" | awk '{print $1}'
  else
    cksum "${file}" | awk '{print $1}'
  fi
}

calc_stream_hash() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum | awk '{print $1}'
  else
    cksum | awk '{print $1}'
  fi
}

get_local_timestamp() {
  local file="$1"
  local git_time=0
  local mtime=0

  if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git_time=$(git log -1 --format="%ct" -- "${file}" 2>/dev/null || echo 0)
  fi

  if command -v node >/dev/null 2>&1; then
    mtime=$(node -e 'const fs = require("fs"); console.log(Math.floor(fs.statSync(process.argv[1]).mtimeMs / 1000))' "${file}" 2>/dev/null || echo 0)
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    mtime=$(stat -f "%m" "${file}" 2>/dev/null || echo 0)
  else
    mtime=$(stat -c "%Y" "${file}" 2>/dev/null || echo 0)
  fi

  if (( git_time > mtime )); then
    echo "$git_time"
  else
    echo "$mtime"
  fi
}

parse_http_date() {
  local date_str="$1"
  if command -v node >/dev/null 2>&1; then
    node -e 'const d = new Date(process.argv[1]); console.log(isNaN(d) ? 0 : Math.floor(d.getTime() / 1000))' "${date_str}"
  elif command -v python3 >/dev/null 2>&1; then
    python3 -c 'import email.utils, sys; print(int(email.utils.parsedate_to_datetime(sys.argv[1]).timestamp()))' "${date_str}" 2>/dev/null || echo 0
  else
    echo 0
  fi
}

UPLOADED_FILES=()
SUMMARY_ROWS=()

upload_if_changed() {
  local remote_path="$1"
  local local_target_file="$2"
  local local_source_file="$3"
  local content_type="$4"

  local local_hash
  local_hash="$(calc_hash "${local_target_file}")"

  local remote_url="${CDN_URL%/}/${remote_path}"
  local remote_hash=""
  local remote_last_modified=""
  local remote_timestamp=0
  local local_timestamp
  local_timestamp="$(get_local_timestamp "${local_source_file}")"

  local should_upload=true
  local status_label="✅ Uploaded"
  local skip_reason=""

  if [[ "${FORCE_UPLOAD}" == false ]]; then
    # Fetch remote headers and content
    local http_headers
    http_headers="$(curl -s -I -L --max-time 10 "${remote_url}" 2>/dev/null || true)"

    if [[ -n "${http_headers}" ]]; then
      remote_last_modified="$(echo "${http_headers}" | grep -i '^last-modified:' | head -n1 | cut -d: -f2- | sed 's/^[[:space:]]*//' | tr -d '\r')"
      if [[ -n "${remote_last_modified}" ]]; then
        remote_timestamp="$(parse_http_date "${remote_last_modified}")"
      fi

      local remote_content
      if remote_content="$(curl -s -L -f --max-time 10 "${remote_url}" 2>/dev/null)"; then
        remote_hash="$(printf "%s" "${remote_content}" | calc_stream_hash)"

        if [[ -n "${remote_hash}" && "${local_hash}" == "${remote_hash}" ]]; then
          should_upload=false
          status_label="⏭️ Skipped"
          skip_reason="SHA-256 match (identical)"
        elif (( remote_timestamp > local_timestamp )); then
          should_upload=false
          status_label="⏭️ Skipped"
          skip_reason="CDN is newer (${remote_last_modified})"
        fi
      fi
    fi
  else
    skip_reason="Forced upload"
  fi

  if [[ "${should_upload}" == false ]]; then
    echo "  ⏭️  ${remote_path} skipped: ${skip_reason}."
    SUMMARY_ROWS+=("| \`${remote_path}\` | ${status_label} | \`${content_type}\` | ${skip_reason} |")
  else
    local upload_reason="Modified & newer"
    [[ "${FORCE_UPLOAD}" == true ]] && upload_reason="Forced upload"
    echo "  ⬆️  ${remote_path} (${upload_reason}). Uploading (${content_type})..."
    UPLOADED_FILES+=("${remote_url}")
    SUMMARY_ROWS+=("| \`${remote_path}\` | ${status_label} | \`${content_type}\` | ${upload_reason} |")

    if [[ "${DRY_RUN}" == true ]]; then
      echo "     [DRY RUN] wrangler r2 object put \"${BUCKET}/${remote_path}\" --file \"${local_target_file}\" --content-type \"${content_type}\" --remote"
    else
      wrangler r2 object put "${BUCKET}/${remote_path}" --file "${local_target_file}" --content-type "${content_type}" --remote
    fi
  fi
}

echo "🔍 Checking for changes & timestamps against CDN (${BUCKET})..."

upload_if_changed "css/klaat-elements.css" "${CSS_TARGET}" "${CSS_SRC}" "text/css"
upload_if_changed "js/klaat-elements.js" "${JS_TARGET}" "${JS_SRC}" "application/javascript"
upload_if_changed "js/klaat-bridge.js" "${BRIDGE_TARGET}" "${BRIDGE_SRC}" "application/javascript"

if [[ ${#UPLOADED_FILES[@]} -eq 0 ]]; then
  echo "✨ All assets on CDN are up-to-date or newer. No files uploaded."
else
  echo "✅ Upload complete! (${#UPLOADED_FILES[@]} file(s) updated)"
fi

PURGE_STATUS="Skipped"

# Optional Purge: Trigger when --purge is requested and token is available
if [[ "${DO_PURGE}" == true ]]; then
  if [[ -n "${API_TOKEN}" ]]; then
    if [[ -z "${ZONE_ID}" ]]; then
      HOST_NAME=$(echo "${CDN_URL}" | sed -e 's|^[^/]*//||' -e 's|/.*$||')
      APEX_DOMAIN=$(echo "${HOST_NAME}" | awk -F. '{if (NF>2) print $(NF-1)"."$NF; else print $0}')

      echo "🔍 Looking up Zone ID for ${APEX_DOMAIN}..."
      ZONE_RESP=$(curl -s -X GET "https://api.cloudflare.com/client/v4/zones?name=${APEX_DOMAIN}" \
        -H "Authorization: Bearer ${API_TOKEN}" \
        -H "Content-Type: application/json")

      ZONE_ID=$(echo "${ZONE_RESP}" | jq -r '.result[0].id // empty' 2>/dev/null || true)
    fi

    if [[ -n "${ZONE_ID}" ]]; then
      echo "🧹 Purging Cloudflare Cache for Zone ${ZONE_ID} (purge_everything)..."

      if [[ "${DRY_RUN}" == true ]]; then
        echo "     [DRY RUN] curl -X POST https://api.cloudflare.com/client/v4/zones/${ZONE_ID}/purge_cache"
        PURGE_STATUS="[DRY RUN] Would purge everywhere"
      else
        PURGE_RESP="$(curl -s -X POST "https://api.cloudflare.com/client/v4/zones/${ZONE_ID}/purge_cache" \
          -H "Authorization: Bearer ${API_TOKEN}" \
          -H "Content-Type: application/json" \
          --data '{"purge_everything":true}')"

        if echo "${PURGE_RESP}" | grep -q '"success":true'; then
          echo "✅ Cache purged everywhere successfully."
          PURGE_STATUS="✅ Cache purged everywhere"
        else
          echo "❌ Cache purge error response from Cloudflare:"
          echo "${PURGE_RESP}" | jq . 2>/dev/null || echo "${PURGE_RESP}"
          PURGE_STATUS="❌ Cache purge failed"
        fi
      fi
    fi
  else
    echo "ℹ️  Cache purge skipped (runs automatically in GitHub Actions CI with secrets)."
    PURGE_STATUS="ℹ️ Skipped locally"
  fi
fi

# Write GitHub Step Summary if running in GitHub Actions
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### 📦 Klaat CDN Asset Deployment Summary"
    echo ""
    echo "| Asset Path | Status | Content Type | Details |"
    echo "| :--- | :--- | :--- | :--- |"
    for row in "${SUMMARY_ROWS[@]}"; do
      echo "${row}"
    done
    echo ""
    echo "**Deployment Metadata:**"
    echo "- **Bucket:** \`${BUCKET}\`"
    echo "- **CDN Base URL:** ${CDN_URL}"
    echo "- **Minified:** \`${DO_MINIFY}\`"
    echo "- **Cache Purge:** ${PURGE_STATUS}"
    echo "- **Files Updated:** ${#UPLOADED_FILES[@]} / 3"
  } >> "${GITHUB_STEP_SUMMARY}"
fi
