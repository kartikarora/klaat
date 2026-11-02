# Privacy Policy for Klaat

**Last Updated:** 2 November 2026

This Privacy Policy sets out how **Klaat** ("we", "us", "our", or "the Application") collects, handles, stores, and protects personal information and user data when you use the Klaat command-line interface (CLI) and related services at **https://klaat.kartikarora.me**.

We are committed to protecting your privacy in accordance with the **Privacy Act 1988 (Cth)**, the **Australian Privacy Principles (APPs)**, and applicable Google API policies.

Klaat is an independent developer utility designed to export technical documentation and tutorials authored in Google Docs into interactive, structured Codelabs (HTML and Markdown).

---

## 1. Information We Access and Collect

Klaat interacts with Google APIs strictly when you explicitly invoke commands requiring Google Docs data (such as `klaat export <document-id>`).

### A. Google User Data
When you authorise Klaat using your Google account, the application requests access to the following sensitive scope:
- `https://www.googleapis.com/auth/documents.readonly`

Through this scope, Klaat accesses:
- **Document Metadata & Content**: Document titles, structural body text, headings, formatting styles, tables, and embedded media elements solely for the specific document IDs you nominate in your command.
- **OAuth Authentication Credentials**: Short-lived OAuth access tokens and refresh tokens issued by Google Identity Services to authorise local requests on your behalf.

### B. Information We Do Not Collect
- We **do not** collect personal identifiers such as your full name, phone number, residential address, or billing details.
- We **do not** access, browse, or read any documents or files within your Google Drive or Google Docs that you have not explicitly specified.
- We **do not** track your browsing behaviour across external websites, compile behavioral profiles, or log telemetry to remote tracking services.

---

## 2. How We Use Google User Data

Klaat processes Google Docs data solely to perform real-time, user-directed actions initiated from your local command line:
- Reading the specified Google Document structure, text, and formatting.
- Parsing and transforming that structure into local HTML, Markdown, or offline codelab packages on your workstation.

**Klaat does not use your data for:**
- Serving targeted advertisements or marketing materials.
- User profiling, market research, or data aggregation.
- Training artificial intelligence (AI) or machine learning (ML) models.

---

## 3. Storage, Security, and Cross-Border Disclosure

Klaat operates as a client-side command-line utility:
- **Local Storage Only**: OAuth credentials, access tokens, and refresh tokens are stored exclusively on your local workstation in your home user directory (`~/.klaat/tokens/`).
- **No Remote Transmission**: The text, structure, and media retrieved from Google Docs are parsed entirely in memory and written as output files directly to your local file system. Neither your document content nor your authentication tokens are ever transmitted to, hosted on, or processed by any external or remote Klaat servers.
- **Cross-Border Disclosure**: Because Klaat does not transmit your personal data to remote servers or third-party cloud infrastructure operated by us, no cross-border disclosure of personal information occurs on our part under APP 8.
- **Token Revocation & Deletion**: You can permanently remove your locally cached credentials at any time by deleting the `~/.klaat/tokens/` directory on your machine or by revoking access directly via your [Google Account Permissions](https://myaccount.google.com/permissions).

---

## 4. Disclosure to Third Parties

We **do not** sell, rent, commercialise, trade, or otherwise disclose your Google user data or personal information to third parties. All communication authenticated via Google APIs occurs directly and securely between your local device and Google's infrastructure.

---

## 5. Google API Services User Data Policy Compliance

Klaat's use and transfer to any other app of information received from Google APIs will adhere to the **[Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy)**, including the **Limited Use** requirements.

---

## 6. Access, Correction, and Enquiries under Australian Privacy Law

Under the Australian Privacy Principles, you have rights to seek access to and correction of any personal information held about you. Because Klaat operates client-side and retains no centrally stored personal records or document contents, your data remains directly within your local custody and your Google account.

If you have any questions, concerns, or complaints regarding this Privacy Policy or our compliance with the Australian Privacy Principles, please contact us at:

- **Entity / Maintainer**: Kartik Arora
- **Website**: [https://klaat.kartikarora.me](https://klaat.kartikarora.me)
- **Email**: contact@kartikarora.me
- **Jurisdiction**: Victoria, Australia (or your specific Australian state/territory)

If you are unsatisfied with our response to a privacy complaint, you may refer the matter to the **Office of the Australian Information Commissioner (OAIC)** at [www.oaic.gov.au](https://www.oaic.gov.au).

---

## 7. Variations to This Policy

We may update this Privacy Policy periodically to reflect changes in our practices, technology, or legal requirements. Any updated version will be published at this location with a revised "Last Updated" date.
