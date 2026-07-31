export interface Env {
  KLAAT_SESSIONS: KVNamespace;
  GOOGLE_CLIENT_ID: string;
  GOOGLE_CLIENT_SECRET: string;
  REDIRECT_URI: string;
}

interface JsonRpcRequest {
  jsonrpc: string;
  id?: string | number;
  method: string;
  params?: any;
}

// Map to store active SSE controllers for writing events
const sseSessions = new Map<string, ReadableStreamDefaultController>();

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    // Enable CORS
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // 1. Google OAuth Authentication flow
    if (url.pathname === "/login") {
      const session = url.searchParams.get("session");
      if (!session) {
        return new Response("Missing session parameter", { status: 400 });
      }

      // Redirect user to Google OAuth Consent Screen
      const oauthUrl = `https://accounts.google.com/o/oauth2/v2/auth?` + 
        `client_id=${encodeURIComponent(env.GOOGLE_CLIENT_ID)}` +
        `&redirect_uri=${encodeURIComponent(env.REDIRECT_URI)}` +
        `&response_type=code` +
        `&scope=${encodeURIComponent("https://www.googleapis.com/auth/documents.readonly")}` +
        `&state=${encodeURIComponent(session)}` +
        `&prompt=consent` +
        `&access_type=offline`;

      return Response.redirect(oauthUrl, 302);
    }

    if (url.pathname === "/oauth/callback") {
      const code = url.searchParams.get("code");
      const session = url.searchParams.get("state"); // state holds our session ID

      if (!code || !session) {
        return new Response("OAuth callback missing code or state session", { status: 400 });
      }

      try {
        // Exchange authorization code for tokens
        const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({
            code,
            client_id: env.GOOGLE_CLIENT_ID,
            client_secret: env.GOOGLE_CLIENT_SECRET,
            redirect_uri: env.REDIRECT_URI,
            grant_type: "authorization_code",
          }),
        });

        if (!tokenResponse.ok) {
          const errText = await tokenResponse.text();
          throw new Error(`Token exchange failed: ${errText}`);
        }

        const tokenData: any = await tokenResponse.json();
        
        // Save token in KV database linked to session
        await env.KLAAT_SESSIONS.put(`token:${session}`, JSON.stringify({
          access_token: tokenData.access_token,
          refresh_token: tokenData.refresh_token,
          expires_at: Date.now() + (tokenData.expires_in * 1000)
        }), { expirationTtl: 3600 * 24 * 7 }); // Expiry 7 days

        // Return a beautiful success page to the user
        const successHtml = `
          <!DOCTYPE html>
          <html>
          <head>
            <title>Klaat Authorization Successful</title>
            <link href="https://distribute.kartikarora.me/css/kartikarora.css" rel="stylesheet">
            <link href="https://fonts.googleapis.com/css2?family=Readex+Pro:wght@400;600&family=Material+Symbols+Outlined" rel="stylesheet">
            <style>
              body { display: flex; align-items: center; justify-content: center; height: 100vh; background: var(--bg); color: var(--primary); font-family: var(--font-sans); }
              .card { padding: var(--space-xl); background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); text-align: center; max-width: 500px; box-shadow: 0 10px 30px var(--shadow); }
              .icon { font-size: 4rem; color: #4caf50; margin-bottom: var(--space-md); }
            </style>
          </head>
          <body>
            <div class="card">
              <span class="material-symbols-outlined icon">check_circle</span>
              <h1>Authentication Successful</h1>
              <p style="margin-top: var(--space-sm); color: var(--secondary);">Your Google Account has been successfully linked to Klaat. You can close this window and continue asking the AI to parse your Google Document.</p>
            </div>
          </body>
          </html>
        `;
        return new Response(successHtml, {
          headers: { "Content-Type": "text/html", ...corsHeaders }
        });
      } catch (err: any) {
        return new Response(`Authentication Error: ${err.message}`, { status: 500 });
      }
    }

    // 2. Model Context Protocol (MCP) SSE Transport Flow
    if (url.pathname === "/sse") {
      const sessionId = crypto.randomUUID();

      const stream = new ReadableStream({
        start(controller) {
          sseSessions.set(sessionId, controller);
          
          // Send the initial SSE handshake event with the POST message route
          const messageEndpoint = `/message?session=${sessionId}`;
          const initEvent = `event: endpoint\ndata: ${messageEndpoint}\n\n`;
          controller.enqueue(new TextEncoder().encode(initEvent));
        },
        cancel() {
          sseSessions.delete(sessionId);
        }
      });

      return new Response(stream, {
        headers: {
          "Cache-Control": "no-cache",
          "Connection": "keep-alive",
          "Content-Type": "text/event-stream",
          ...corsHeaders
        }
      });
    }

    if (url.pathname === "/message") {
      const session = url.searchParams.get("session");
      if (!session) {
        return new Response("Missing session query parameter", { status: 400, headers: corsHeaders });
      }

      const requestBody: JsonRpcRequest = await request.json();
      const response = await handleMcpRequest(requestBody, session, env);

      // If there's an active SSE session open, we write the response back to the event stream
      const controller = sseSessions.get(session);
      if (controller && response) {
        const sseEvent = `event: message\ndata: ${JSON.stringify(response)}\n\n`;
        controller.enqueue(new TextEncoder().encode(sseEvent));
      }

      return new Response(JSON.stringify({ status: "accepted" }), {
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });
    }

    return new Response("Klaat MCP Server. Connect via SSE at /sse", { status: 404, headers: corsHeaders });
  }
};

// Handles JSON-RPC requests conforming to the Model Context Protocol
async function handleMcpRequest(req: JsonRpcRequest, session: string, env: Env): Promise<any | null> {
  const method = req.method;

  if (method === "initialize") {
    return {
      jsonrpc: "2.0",
      id: req.id,
      result: {
        protocolVersion: "2024-11-05",
        capabilities: {
          tools: {}
        },
        serverInfo: {
          name: "klaat-mcp-server",
          version: "1.1.0"
        }
      }
    };
  }

  if (method === "tools/list") {
    return {
      jsonrpc: "2.0",
      id: req.id,
      result: {
        tools: [
          {
            name: "google_sign_in",
            description: "Generates a Google OAuth authorization URL for the user to sign in to their Google account to read documents.",
            inputSchema: {
              type: "object",
              properties: {}
            }
          },
          {
            name: "check_google_auth_status",
            description: "Checks if the user has authenticated and connected their Google Account with this session.",
            inputSchema: {
              type: "object",
              properties: {}
            }
          },
          {
            name: "parse_google_doc",
            description: "Fetches a Google Document by ID and parses it into Klaat-compatible Markdown text.",
            inputSchema: {
              type: "object",
              properties: {
                documentId: {
                  type: "string",
                  description: "The Google Document ID (found in the URL: docs.google.com/document/d/DOCUMENT_ID)"
                }
              },
              required: ["documentId"]
            }
          },
          {
            name: "generate_markdown_codelab",
            description: "Generates a perfectly structured and validated Klaat Markdown codelab from metadata and step content components.",
            inputSchema: {
              type: "object",
              properties: {
                id: { type: "string", description: "URL-safe slug identifier." },
                title: { type: "string", description: "The main H1 title of the codelab." },
                summary: { type: "string", description: "Required short summary of the codelab." },
                categories: { type: "array", items: { type: "string" }, description: "List of categories." },
                status: { type: "string", description: "Codelab status (e.g. Published, Draft)." },
                authors: { type: "array", items: { type: "string" }, description: "Authors list." },
                steps: {
                  type: "array",
                  description: "Step-by-step contents of the tutorial.",
                  items: {
                    type: "object",
                    properties: {
                      title: { type: "string", description: "Heading 2 step title." },
                      duration: { type: "integer", description: "Step duration in minutes." },
                      content: { type: "string", description: "Markdown text content for the step." }
                    },
                    required: ["title", "duration", "content"]
                  }
                }
              },
              required: ["title", "summary", "steps"]
            }
          },
          {
            name: "validate_markdown_codelab",
            description: "Validates a raw Markdown string against Klaat's schema and structure rules (checks frontmatter, titles, steps, and duration annotations).",
            inputSchema: {
              type: "object",
              properties: {
                markdownContent: {
                  type: "string",
                  description: "The raw markdown content string to validate."
                }
              },
              required: ["markdownContent"]
            }
          },
          {
            name: "get_klaat_prompts",
            description: "Returns highly optimized system prompts and templates for writing, reviewing, or migrating documents into Klaat codelabs.",
            inputSchema: {
              type: "object",
              properties: {
                type: {
                  type: "string",
                  enum: ["authoring", "docs_mapping", "migration"],
                  description: "Filter prompts by usage type."
                }
              }
            }
          },
          {
            name: "get_klaat_skills",
            description: "Returns the complete file definitions (filenames, relative paths, and contents) for KLAAT AI guidelines and formatting skills so they can be written to the local workspace.",
            inputSchema: {
              type: "object",
              properties: {}
            }
          }
        ]
      }
    };
  }

  if (method === "tools/call") {
    const toolName = req.params?.name;
    const args = req.params?.arguments;

    try {
      if (toolName === "google_sign_in") {
        const loginUrl = `https://mcp.klaat.kartikarora.me/login?session=${session}`;
        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: `To authenticate, please visit the Google Sign-In URL in your browser:\n\n${loginUrl}\n\nOnce completed, notify the AI that you have successfully authenticated.`
              }
            ]
          }
        };
      }

      if (toolName === "check_google_auth_status") {
        const tokenString = await env.KLAAT_SESSIONS.get(`token:${session}`);
        const authenticated = tokenString !== null;

        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: authenticated 
                  ? "Status: Authenticated. You can run parse_google_doc." 
                  : "Status: Not authenticated. Call google_sign_in to authenticate."
              }
            ]
          }
        };
      }

      if (toolName === "parse_google_doc") {
        const docId = args?.documentId;
        if (!docId) {
          throw new Error("Missing documentId parameter");
        }

        const tokenString = await env.KLAAT_SESSIONS.get(`token:${session}`);
        if (!tokenString) {
          return {
            jsonrpc: "2.0",
            id: req.id,
            result: {
              isError: true,
              content: [
                {
                  type: "text",
                  text: "Authentication Required. Please run google_sign_in tool first to log in with your Google account."
                }
              ]
            }
          };
        }

        const tokenData = JSON.parse(tokenString);
        let accessToken = tokenData.access_token;

        if (tokenData.expires_at && tokenData.expires_at < Date.now()) {
          if (tokenData.refresh_token) {
            const refreshResponse = await fetch("https://oauth2.googleapis.com/token", {
              method: "POST",
              headers: { "Content-Type": "application/x-www-form-urlencoded" },
              body: new URLSearchParams({
                client_id: env.GOOGLE_CLIENT_ID,
                client_secret: env.GOOGLE_CLIENT_SECRET,
                refresh_token: tokenData.refresh_token,
                grant_type: "refresh_token",
              }),
            });

            if (refreshResponse.ok) {
              const refreshData: any = await refreshResponse.json();
              accessToken = refreshData.access_token;
              await env.KLAAT_SESSIONS.put(`token:${session}`, JSON.stringify({
                access_token: accessToken,
                refresh_token: tokenData.refresh_token,
                expires_at: Date.now() + (refreshData.expires_in * 1000)
              }));
            } else {
              throw new Error("Google access token has expired and refresh failed. Please log in again using google_sign_in.");
            }
          } else {
            throw new Error("Google access token has expired. Please log in again using google_sign_in.");
          }
        }

        const docResponse = await fetch(`https://docs.googleapis.com/v1/documents/${docId}`, {
          headers: {
            "Authorization": `Bearer ${accessToken}`,
            "Content-Type": "application/json"
          }
        });

        if (!docResponse.ok) {
          const errText = await docResponse.text();
          throw new Error(`Failed to fetch Google Document: ${errText}`);
        }

        const docData: any = await docResponse.json();
        const markdown = parseGdocToMarkdown(docData);

        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: markdown
              }
            ]
          }
        };
      }

      if (toolName === "generate_markdown_codelab") {
        const id = args.id || args.summary.toLowerCase().replace(/[^a-z0-9]+/g, "-");
        const title = args.title;
        const summary = args.summary;
        const categories = args.categories ? args.categories.join(", ") : "";
        const status = args.status || "Published";
        const authors = args.authors ? args.authors.join(", ") : "";
        const steps = args.steps || [];

        let md = "---\n";
        md += `id: ${id}\n`;
        md += `summary: ${summary}\n`;
        if (categories) md += `categories: ${categories}\n`;
        md += `status: ${status}\n`;
        if (authors) md += `authors: ${authors}\n`;
        md += "---\n\n";

        md += `# ${title}\n\n`;

        steps.forEach((step: any) => {
          md += `## ${step.title}\n`;
          md += `Duration: ${step.duration}\n\n`;
          md += `${step.content.trim()}\n\n`;
        });

        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: md.trim() + "\n"
              }
            ]
          }
        };
      }

      if (toolName === "validate_markdown_codelab") {
        const md = args.markdownContent || "";
        const lines = md.split("\n");
        const errors: string[] = [];
        const warnings: string[] = [];

        // 1. Check Frontmatter
        if (lines[0] !== "---") {
          errors.push("Missing frontmatter block starting tag '---' at line 1.");
        } else {
          let fmEnd = -1;
          for (let i = 1; i < lines.length; i++) {
            if (lines[i] === "---") {
              fmEnd = i;
              break;
            }
          }

          if (fmEnd === -1) {
            errors.push("Missing closing frontmatter block tag '---'.");
          } else {
            const fmContent = lines.slice(1, fmEnd).join("\n");
            if (!fmContent.includes("summary:")) {
              errors.push("Frontmatter metadata is missing the required 'summary' key.");
            }
            if (!fmContent.includes("id:")) {
              warnings.push("Frontmatter metadata has no 'id' key. Klaat will sluggify the summary.");
            }
          }
        }

        // 2. Check Title
        const hasH1 = lines.some((l: string) => l.startsWith("# "));
        if (!hasH1) {
          errors.push("Missing document H1 Title (e.g. '# Tutorial Title').");
        }

        // 3. Check Steps and Durations
        let stepCount = 0;
        for (let i = 0; i < lines.length; i++) {
          if (lines[i].startsWith("## ")) {
            stepCount++;
            const nextLine = lines[i + 1] || "";
            const nextNextLine = lines[i + 2] || "";

            const hasDuration = nextLine.toLowerCase().startsWith("duration:") || 
                                nextNextLine.toLowerCase().startsWith("duration:");

            if (!hasDuration) {
              errors.push(`Step '${lines[i].substring(3)}' is missing the 'Duration: MM' annotation immediately below it.`);
            }
          }
        }

        if (stepCount === 0) {
          errors.push("No Heading 2 (##) steps found. A codelab requires at least one step.");
        }

        const isSuccess = errors.length === 0;

        let report = `Validation Status: ${isSuccess ? "✔ VALID" : "✖ INVALID"}\n\n`;
        if (errors.length > 0) {
          report += `Errors (${errors.length}):\n` + errors.map(e => `- ${e}`).join("\n") + "\n\n";
        }
        if (warnings.length > 0) {
          report += `Warnings (${warnings.length}):\n` + warnings.map(w => `- ${w}`).join("\n") + "\n\n";
        }
        if (isSuccess) {
          report += "All metadata checks, H1 headers, step nodes, and durations conform to Klaat's schema guidelines.";
        }

        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: report
              }
            ]
          }
        };
      }

      if (toolName === "get_klaat_prompts") {
        const type = args.type;
        const authoringPrompt = `You are a Senior Kotlin & Technical Writer AI assistant. Your goal is to write a highly informative, valid Klaat-compatible Markdown tutorial. Follow these strict rules:
1. Begin with a valid YAML frontmatter containing required 'summary' and optional 'id', 'categories', 'status', 'authors', and 'feedback_link' fields.
2. Direct after the frontmatter, write the main H1 Codelab Title: '# Title'
3. Write steps using Heading 2s: '## Step Title'
4. Immediately write the step duration directly under the Heading 2: 'Duration: X' (where X is in minutes)
5. Set up rich elements:
   - Positive boxes: blockquotes starting with '> **Note:**' or '> **Pro Tip:**'
   - Negative boxes: blockquotes starting with '> **Warning:**' or '> **Caution:**'
   - Code blocks with filenames: Write '**filename.ext**' on the line immediately preceding the code block.
   - Checklist checklist elements under a bold survey question.`;

        const docsMappingPrompt = `You are helping the user map their Google Doc contents to a Klaat-compatible layout. Share these clear formatting rules:
1. Place a 2-column metadata table at the very beginning of the Google Document (keys in left column, values in right column).
2. Set the Codelab Title using Google Docs 'Title' style.
3. Mark each step with 'Heading 1' style, and add 'Duration: X' (minutes) immediately below it in plain text.
4. For Positive Alerts (Tips): create a single-cell table, set background to light green, and start with 'Note:' or 'Pro Tip:'.
5. For Negative Alerts (Warnings): create a single-cell table, set background to light red, and start with 'Warning:' or 'Caution:'.
6. For Code Blocks: create a single-cell table and set the font to 'Courier New'.`;

        const migrationPrompt = `You are migrating an existing article/documentation page into a step-by-step Klaat codelab.
Analyze the headings, paragraphs, and elements of the source text, split the content logically into steps of 2 to 10 minutes duration each, write correct YAML frontmatter metadata, and output a valid Markdown document with H2 headers, step durations, warning panels, and syntax highlighted code blocks with filenames.`;

        let resultText = "";
        if (type === "authoring") {
          resultText = authoringPrompt;
        } else if (type === "docs_mapping") {
          resultText = docsMappingPrompt;
        } else if (type === "migration") {
          resultText = migrationPrompt;
        } else {
          resultText = `=== 1. AUTHORING PROMPT ===\n${authoringPrompt}\n\n=== 2. GOOGLE DOCS MAPPING PROMPT ===\n${docsMappingPrompt}\n\n=== 3. MIGRATION PROMPT ===\n${migrationPrompt}`;
        }

        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: resultText
              }
            ]
          }
        };
      }

      if (toolName === "get_klaat_skills") {
        const skillsData = [
          {
            path: "klaat-markdown-author/SKILL.md",
            content: `---
name: klaat-markdown-author
description: Instructs the AI on how to author valid Klaat-compatible Markdown documents following the official format guide. Covers YAML frontmatter validation, steps hierarchy, step durations, and rich element syntax (info boxes, surveys, code filenames).
---

# Klaat Markdown Authoring Guide

Use this skill when the user requests to create, format, or review a Markdown document for export with Klaat.

## Mandatory Structure

All Klaat Markdown files must adhere to the following layout:

\`\`\`markdown
---
id: unique-url-slug
summary: Short single-sentence description of the tutorial.
categories: Category1, Category2
environments: Web
status: Published
authors: Your Name
feedback_link: https://github.com/your/repo/issues
---

# Codelab Title

## Step 1 Title
Duration: 5

Step content...

## Step 2 Title
Duration: 10

Step content...
\`\`\`

## Frontmatter Schema

Verify that the YAML block matches these validation rules:

| Field Name | Type | Requirement | Fallback / Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| \`id\` | String | Optional | Sluggified \`summary\` | URL-safe name for the output folder (lowercase, hyphens, alphanumeric). |
| \`summary\` | String | **Required** | None | Short description used for cards. Falls back to \`untitled-codelab\` if completely missing. |
| \`categories\`| List | Optional | Empty list | Comma-separated categories or YAML block list. |
| \`environments\`| List | Optional | \`["Web"]\` | Comma-separated target environments. |
| \`status\` | List | Optional | \`["Published"]\` | Release state: \`Published\`, \`Draft\`, \`Deprecated\`. |
| \`authors\` | List | Optional | Empty list | Comma-separated author name(s). |
| \`feedback_link\`| String| Optional | Empty string | URL for reporting bugs/issues. |
| \`analytics_account\`| String| Optional| Empty string | Google Analytics property ID. |
| \`tags\` | List | Optional | Empty list | Comma-separated keyword tags. |
| \`theme\` | String | Optional | Empty string | Styling theme class hook. |
| \`duration\` | Int | Optional | Sum of steps | Override for total tutorial duration in minutes. |
| \`image\` | String | Optional | First page image | Cover image path/URL. |

## Content Element Syntax

Ensure the correct Markdown markup is used for rich elements:

### 1. Steps & Durations
* Each step must start with a **Heading 2** (\`## Step Title\`).
* Directly beneath the step title, specify the step duration in minutes: \`Duration: X\` (e.g., \`Duration: 5\`). Do not add extra text on this line.

### 2. Info Boxes (Alerts)
* **Positive (Tips/Info):** Start blockquote with \`**Note:**\` or \`**Pro Tip:**\`
  \`\`\`markdown
  > **Note:** Best practice tips go here.
  \`\`\`
* **Warning (Warnings/Caution):** Start blockquote with \`**Warning:**\` or \`**Caution:**\`
  \`\`\`markdown
  > **Warning:** Critical alerts or safety warnings go here.
  \`\`\`

### 3. Code Blocks
* Always specify the programming language (e.g., \`\`\`kotlin, \`\`\`bash).
* **Code block with filename:** Add bold file header immediately above the code block:
  \`\`\`markdown
  **Main.kt**
  \`\`\`kotlin
  fun main() = println("Hello!")
  \`\`\`
  \`\`\`

### 4. Interactive Surveys
Use checklist bullet blocks immediately following a bold question:
\`\`\`markdown
**How would you rate your Kotlin experience?**
- [ ] Beginner
- [ ] Intermediate
- [ ] Expert
\`\`\`

### 5. Frequently Asked Questions (FAQs)
Format Q&A sections using bold \`Q:\` and plain \`A:\` pairs:
\`\`\`markdown
**Q: What is the default port?**

A: The default port is 9090.
\`\`\`

### 6. Action / Download Buttons
Write a single link alone on its own line:
\`\`\`markdown
[Download Starter Code](https://example.com/starter.zip)
\`\`\``
          },
          {
            path: "klaat-markdown-author/references/template.md",
            content: `---
id: sample-klaat-tutorial
summary: A sample tutorial demonstrating the correct frontmatter and structure.
categories: Kotlin, Tutorial
environments: Web, Kiosk
status: Published
authors: Klaat Author
feedback_link: https://github.com/yourusername/klaat/issues
tags: getting-started, guide, kotlin
---

# Sample Klaat Tutorial Title

## Step 1: Configuration
Duration: 5

Welcome to the tutorial! Here is a tip to help you:

> **Note:** Always keep your step durations reasonable (between 2 and 15 minutes is recommended).

### Environment Setup

Add the following dependency to your build file:

\`\`\`kotlin
implementation("me.kartikarora.klaat:core:1.1.0")
\`\`\`

## Step 2: Running the Command
Duration: 3

Now we are ready to run the export:

\`\`\`bash
klaat export hello.md -o output/
\`\`\`

**Verify the files:**
- [ ] Export directory \`output\` exists.
- [ ] The generated HTML file loads.
- [ ] Styles and assets load from CDN.

**Q: Can I customize the output location?**

A: Yes! Use the \`-o\` or \`--out\` flag to specify any path.`
          },
          {
            path: "klaat-gdocs-author/SKILL.md",
            content: `---
name: klaat-gdocs-author
description: Instructs the AI on how to help users author valid Google Docs tutorials compatible with Klaat. Explains how to structure metadata tables, steps, durations, code tables, and warning cells. Guides the AI on handling Google OAuth sign-in prompts.
---

# Klaat Google Docs Authoring Guide

Use this skill when a user wants to structure or prepare a Google Document to export as a codelab using Klaat.

## ⚠️ Important: Drive & Sign-In Requirements

Since the AI does not have direct write access to the user's private Google Drive account, you cannot create the Google Document for them. Instead, you must instruct the user on how to set it up:

1. **Creation**: The user must create a new document in their own Google Drive account.
2. **Authentication**: Explain to the user that when they run \`klaat export [DOC_ID]\` for the first time, Klaat will open their default browser to authenticate read-access to Google Docs. The user must complete this OAuth sign-in.
3. **Local Storage**: Klaat will save the login credentials securely on the user's system at \`~/.klaat/tokens/StoredCredential\` for subsequent requests.

## Google Doc Layout Schema

Instruct the user to structure their Google Document with these styles:

### 1. Metadata Block (Document Start)
The document must begin with a **two-column table** listing the configuration properties:

| Left Column (Keys) | Right Column (Values) |
| :--- | :--- |
| \`id\` | \`my-first-gdoc-codelab\` |
| \`summary\` | \`A brief description of this tutorial.\` |
| \`categories\` | \`Android, Kotlin\` |
| \`status\` | \`Published\` |
| \`authors\` | \`Your Name\` |
| \`feedback_link\` | \`https://github.com/org/repo/issues\` |

### 2. Title & Steps
* **Codelab Title**: Use the standard **Title** paragraph style or a large Header.
* **Step Headers**: Mark the beginning of each step using **Heading 1** (e.g., \`1. Introduction\`).
* **Step Duration**: Write \`Duration: X\` (where X is in minutes) as plain text directly below the Heading 1 step header.

### 3. Visual Element Formatting Maps
To output code blocks and alerts correctly, the user must apply these table and color mappings:

* **Positive Info Box (Tips)**:
  * Create a **single-cell table**.
  * Set the cell background color to **light green** (hex \`#E8F5E9\` or similar).
  * Start text with **Note:** or **Pro Tip:** in bold.
* **Warning Info Box (Warnings)**:
  * Create a **single-cell table**.
  * Set the cell background color to **light red / orange** (hex \`#FFEBEE\` or \`#FFF3E0\` or similar).
  * Start text with **Warning:** or **Caution:** in bold.
* **Code Blocks**:
  * Create a **single-cell table**.
  * Set the font of all code contents to **Courier New**.
  * For terminal commands, set the cell background to **black** and text to **light grey / white** in Courier New.

## Exporting the Codelab

Guide the user to perform the following steps to export:

1. Extract the **Document ID** from the Google Docs URL in their browser:
   \`https://docs.google.com/document/d/YOUR_DOCUMENT_ID_HERE/edit\`
2. Run the export command in their terminal:
   \`\`\`bash
   klaat export YOUR_DOCUMENT_ID_HERE -o output/
   \`\`\`
3. Authenticate on the browser window that opens.`
          },
          {
            path: "klaat-gdocs-author/references/gdoc-mapping-reference.md",
            content: `# Google Docs Visual Formatting Mapping Reference

When structuring a Google Doc for Klaat, apply the following visual layouts to map elements into codelab nodes correctly:

---

## 1. Metadata Block
A **two-column table** placed at the very top of the document. Do not put any text before this table.

| Key (Left Column) | Value (Right Column) |
| :--- | :--- |
| \`id\` | \`gdoc-tutorial-sample\` |
| \`summary\` | \`A sample showing Google Doc mapping formats.\` |
| \`categories\` | \`Android, Kotlin\` |
| \`status\` | \`Published\` |
| \`authors\` | \`Klaat Exporter\` |

---

## 2. Steps & Durations
* **Step Title**: Formatted as a **Heading 1** paragraph.
* **Step Duration**: Formatted as plain text immediately below the Heading 1 step header. Format: \`Duration: X\` (e.g., \`Duration: 5\`).

---

## 3. Info Boxes (Alerts)
Info boxes must be created using **single-cell tables** with specific background shading:

| Element | Background Shading | Prefix Content |
| :--- | :--- | :--- |
| **Positive Box (Info/Tip)** | Light Green (hex \`#E8F5E9\` or similar) | Start with **Note:** or **Pro Tip:** |
| **Warning Box (Warning)** | Light Red (hex \`#FFEBEE\` or similar) | Start with **Warning:** or **Caution:** |

---

## 4. Code & Commands
Code blocks must be created using **single-cell tables** using the \`Courier New\` font:

* **Standard Code Block**:
  * Single-cell table.
  * Font set to \`Courier New\`.
  * No custom background color.
* **Terminal Command Block**:
  * Single-cell table.
  * Cell background set to **Black**.
  * Font set to \`Courier New\` (White or Light Grey text color).`
          },
          {
            path: "klaat-markdown-author/references/format-guide.md",
            content: `# Klaat Codelab Formatting Guide

This guide documents the official syntax, structure, and formatting requirements for generating codelabs with Klaat.

---

## 1. Metadata Frontmatter (YAML Block)
Every codelab begins with a YAML block that defines the tutorial's configuration.

\`\`\`yaml
---
id: sample-codelab-id
summary: A brief description of what this tutorial covers.
categories: Kotlin, Setup
environments: Web
status: Published
authors: Jane Doe
feedback_link: https://github.com/username/repo/issues
---
\`\`\`

### Metadata Fields Reference

| YAML Field | Requirement | Data Type | Default / Fallback | Description |
| :--- | :--- | :--- | :--- | :--- |
| \`id\` | Optional | String | Sluggified \`summary\` | URL-safe name for the folder. Alphanumeric and dashes only. |
| \`summary\` | **Required** | String | None | Short description used for lists and preview cards. |
| \`categories\` | Optional | List | Empty list | Categories for directory catalog listing and filtering. |
| \`environments\`| Optional | List | \`["Web"]\` | Environments/platforms where the codelab runs. |
| \`status\` | Optional | List | \`["Published"]\` | Release state (e.g. \`Published\`, \`Draft\`, \`Deprecated\`). |
| \`authors\` | Optional | List | Empty list | Codelab author name(s). |
| \`feedback_link\`| Optional | String | Empty string | URL for feedback/bug reports (renders as footer link). |
| \`analytics_account\`| Optional| String | Empty string | Google Analytics property ID. |
| \`theme\` | Optional | String | Empty string | CSS hook class for styling customization. |
| \`duration\` | Optional | Integer | Sum of step times| Manual override for total tutorial duration in minutes. |
| \`image\` | Optional | String | First content image| Cover image URL for card listing catalogs. |

*Note: Custom frontmatter variables are stripped unless the \`-pass_metadata\` comma-separated flag is specified during export.*

---

## 2. Document Hierarchy & Steps

Klaat splits your tutorial into separate pages (called **Steps**) based on Heading structures.

### Markdown Structure
* **Codelab Title**: Set as a single **Heading 1** (\`# Title\`).
* **Step Headers**: Mark each step with a **Heading 2** (\`## Step Title\`).
* **Step Duration**: Specify step length in minutes using \`Duration: X\` directly on the line immediately below the H2.

*Example:*
\`\`\`markdown
# Building a Kotlin App

## Step 1: Getting Started
Duration: 5

Step content goes here...
\`\`\`

### Google Docs Structure
* **Codelab Title**: Styled using the Google Docs **Title** style.
* **Step Headers**: Styled using the Google Docs **Heading 1** style.
* **Step Duration**: Write \`Duration: X\` in plain text directly under the Heading 1 step header.

---

## 3. Inline & Block Formatting Elements

Use these syntax conventions to format various rich widgets:

### Info Boxes (Alerts)
Info boxes must be created using blockquotes with a specific prefix. In Google Docs, create a single-cell table with a background color:

* **Positive Box (Tips/Info)**:
  * *Markdown*: Blockquote starting with \`**Note:**\` or \`**Pro Tip:**\`
  * *Google Docs*: Single-cell table with a **light green** background.
  * *Example*:
    \`\`\`markdown
    > **Note:** This is a tip/recommendation.
    \`\`\`
* **Negative Box (Warnings/Alerts)**:
  * *Markdown*: Blockquote starting with \`**Warning:**\` or \`**Caution:**\`
  * *Google Docs*: Single-cell table with a **light red/orange** background.
  * *Example*:
    \`\`\`markdown
    > **Warning:** This is a warning or critical notice.
    \`\`\`

### Code & Commands
* **Inline Code**: Surround with single backticks: \`val name = "Klaat"\`.
* **Multi-line Code Blocks**: Use triple backticks with syntax language:
  \`\`\`kotlin
  fun main() {
      println("Hello World")
  }
  \`\`\`
* **Code Block with Filename**: Put file names in bold immediately above the code block:
  \`\`\`markdown
  **Main.kt**
  \`\`\`kotlin
  fun main() = println("Hello!")
  \`\`\`
  \`\`\`
* **Google Docs Code Block**: Place code in a single-cell table, set font to **Courier New**. For terminal commands, set the table background to **black** and font to white.

### Interactive Surveys & Checklists
Render checkboxes by putting a list immediately below a bold question:
\`\`\`markdown
**Which editor are you using?**
- [ ] IntelliJ IDEA
- [ ] Android Studio
- [ ] VS Code
\`\`\`

### Frequently Asked Questions (FAQs)
Format FAQ lists using bold \`Q:\` and plain \`A:\` tags:
\`\`\`markdown
**Q: Can I build native binaries with Klaat?**

A: Yes! Use \`./gradlew nativeCompile\` to build a native binary using GraalVM.
\`\`\`

### Action / Download Buttons
Write a single link alone on its own line:
\`\`\`markdown
[Download Starter Project](https://example.com/starter-code.zip)
\`\`\`

### Media & Embeds
* **Images**: Render images with standard syntax and an optional caption underneath:
  \`\`\`markdown
  ![Klaat Architecture Overview](images/architecture.png)
  \`\`\`
* **YouTube Videos**: Embed a YouTube video by putting its link alone on its own line:
  \`\`\`markdown
  https://www.youtube.com/watch?v=dQw4w9WgXcQ
  \`\`\``
          },
          {
            path: "klaat-gdocs-author/references/format-guide.md",
            content: ""
          }
        ];

        skillsData[5].content = skillsData[4].content;

        return {
          jsonrpc: "2.0",
          id: req.id,
          result: {
            content: [
              {
                type: "text",
                text: JSON.stringify(skillsData, null, 2)
              }
            ]
          }
        };
      }
    } catch (err: any) {
      return {
        jsonrpc: "2.0",
        id: req.id,
        result: {
          isError: true,
          content: [
            {
              type: "text",
              text: `Tool Error: ${err.message}`
            }
          ]
        }
      };
    }
  }

  return null;
}

// Converts Google Doc structural JSON nodes (paragraphs, tables, lists) to KLAAT Markdown
function parseGdocToMarkdown(doc: any): string {
  let markdown = "";
  
  const title = doc.title || "Untitled Document";
  const body = doc.body?.content || [];
  
  // Try to find the metadata block (e.g., first table)
  let foundMetadataTable = false;
  let metadataYaml = "---\n";

  // Scan document elements
  for (const element of body) {
    if (element.table) {
      // If it is the first table, it's highly likely the Metadata table
      if (!foundMetadataTable) {
        foundMetadataTable = true;
        const rows = element.table.tableRows || [];
        for (const row of rows) {
          const cells = row.tableCells || [];
          if (cells.length === 2) {
            const key = getTextFromCell(cells[0]).trim().replace(/:$/, "").toLowerCase();
            const value = getTextFromCell(cells[1]).trim();
            metadataYaml += `${key}: ${value}\n`;
          }
        }
        metadataYaml += "---\n\n";
        markdown += metadataYaml;
        markdown += `# ${title}\n\n`;
        continue;
      }
      
      // Standard Table parsing
      const rows = element.table.tableRows || [];
      markdown += "\n";
      for (let r = 0; r < rows.length; r++) {
        const cells = rows[r].tableCells || [];
        let rowText = "|";
        let sepText = "|";
        for (const cell of cells) {
          rowText += ` ${getTextFromCell(cell).trim().replace(/\n/g, "<br>")} |`;
          sepText += "---|";
        }
        markdown += rowText + "\n";
        if (r === 0) {
          markdown += sepText + "\n";
        }
      }
      markdown += "\n";
      continue;
    }

    if (element.paragraph) {
      const paragraph = element.paragraph;
      const style = paragraph.paragraphStyle?.namedStyleType;
      const text = getTextFromParagraph(paragraph);

      if (!text.trim()) continue;

      if (style === "TITLE") {
        if (!foundMetadataTable) {
          markdown += `# ${text.trim()}\n\n`;
        }
      } else if (style === "HEADING_1") {
        markdown += `## ${text.trim()}\n`;
      } else if (style === "HEADING_2") {
        markdown += `### ${text.trim()}\n`;
      } else if (style === "HEADING_3") {
        markdown += `#### ${text.trim()}\n`;
      } else {
        // Standard text, handle duration formatting
        if (text.toLowerCase().startsWith("duration:")) {
          markdown += `${text.trim()}\n\n`;
        } else {
          markdown += `${text}\n`;
        }
      }
    }
  }

  return markdown;
}

function getTextFromParagraph(paragraph: any): string {
  let text = "";
  const elements = paragraph.elements || [];
  for (const el of elements) {
    if (el.textRun) {
      let content = el.textRun.content;
      const style = el.textRun.textStyle || {};
      
      if (style.bold && style.italic) {
        content = `***${content.trim()}*** `;
      } else if (style.bold) {
        content = `**${content.trim()}** `;
      } else if (style.italic) {
        content = `*${content.trim()}* `;
      }
      
      if (style.link?.url) {
        content = `[${content.trim()}](${style.link.url})`;
      }
      
      text += content;
    }
  }
  return text;
}

function getTextFromCell(cell: any): string {
  let text = "";
  const content = cell.content || [];
  for (const item of content) {
    if (item.paragraph) {
      text += getTextFromParagraph(item.paragraph) + " ";
    }
  }
  return text;
}
