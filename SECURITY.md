# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 2.x     | Yes       |
| < 2.0   | No        |

## Reporting a Vulnerability

Please **do not** report security vulnerabilities through public GitHub issues.

Instead, report them privately via [GitHub Security Advisories](https://github.com/kartikarora/klaat/security/advisories/new).

Include:
- A description of the vulnerability and its potential impact.
- Steps to reproduce or a minimal proof of concept.
- The Klaat version and OS/platform you tested on.

## Credential & Token Safety

- Local Google OAuth credentials (`gcp_credentials.json`) and cached OAuth tokens (`~/.klaat/tokens/`) must never be committed to source control.
- If you believe a credential or token was accidentally exposed, revoke it immediately in Google Cloud Console and rotate the secret.
