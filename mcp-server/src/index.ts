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
          "Content-Type": "text/stream-stream", // Fallback stream
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
          version: "1.0.0"
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
        const hasH1 = lines.some(l => l.startsWith("# "));
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
