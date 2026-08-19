<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Index of /${path} - Klaat</title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    
    <!-- Brand Identity CSS -->
    <link rel="stylesheet" href="https://distribute.kartikarora.me/css/kartikarora.1.7.0.css">
    
    <!-- Favicons -->
    <link rel="icon" type="image/png" sizes="32x32" href="https://kartikarora.me/img/icon32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="https://kartikarora.me/img/icon16.png">
    <link rel="apple-touch-icon" sizes="180x180" href="https://kartikarora.me/img/icon180.png">
    <link rel="apple-touch-icon" sizes="192x192" href="https://kartikarora.me/img/icon192.png">

    <!-- Brand CDN JS -->
    <script src="https://distribute.kartikarora.me/js/kartikarora.1.7.0.js" defer></script>

    
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: var(--font-sans);
            line-height: 1.6;
            color: var(--primary);
            background: var(--bg);
            display: flex;
            flex-direction: column;
            min-height: 100vh;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        .hero {
            padding-top: calc(70px + var(--space-3xl));
            padding-bottom: var(--space-3xl);
            text-align: center;
            border-bottom: 1px solid var(--border);
            background: var(--bg-alt);
        }

        @media (max-width: 768px) {
            .hero {
                padding-top: calc(60px + var(--space-lg));
            }
        }

        .hero h1 {
            font-size: 3rem;
            font-weight: 800;
            margin-bottom: var(--space-xs);
            letter-spacing: -1px;
            color: var(--primary);
        }

        .hero p {
            font-size: 1.25rem;
            color: var(--secondary);
        }

        .container {
            max-width: var(--max-width);
            margin: 0 auto;
            padding: 0 var(--space-md) var(--space-3xl);
        }

        .breadcrumb {
            margin: var(--space-xl) 0 var(--space-md);
            font-size: 16px;
            color: var(--secondary);
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: var(--space-xs);
        }

        .breadcrumb a {
            color: var(--accent);
            text-decoration: none;
            font-weight: 500;
            border-bottom: 1px dashed var(--accent);
            transition: all var(--transition);
        }

        .breadcrumb a:hover {
            color: var(--accent-hover);
            border-bottom-color: transparent;
        }

        .listing-card {
            background: var(--card);
            border-radius: var(--border-radius);
            border: 1px solid var(--border);
            overflow: hidden;
            box-shadow: var(--shadow);
            transition: all var(--transition);
        }

        .listing-card:hover {
            box-shadow: var(--shadow);
            border-color: var(--accent);
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            text-align: left;
            padding: var(--space-md) var(--space-lg);
            border-bottom: 1px solid var(--border);
        }

        th {
            font-size: 13px;
            font-weight: 600;
            color: var(--tertiary);
            background: var(--bg-alt);
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        tr:last-child td {
            border-bottom: none;
        }

        tr:hover {
            background-color: var(--bg-alt);
        }

        .name-col {
            display: flex;
            align-items: center;
            gap: var(--space-xs);
        }

        .icon {
            font-size: 24px;
            color: var(--accent);
        }

        .name-col a {
            color: var(--primary);
            text-decoration: none;
            font-weight: 500;
            transition: color var(--transition);
        }

        .name-col a:hover {
            color: var(--accent);
        }

        .size-col, .date-col {
            font-family: var(--font-mono);
            font-size: 13px;
            color: var(--secondary);
        }

        @media (max-width: 768px) {
            .hero h1 { font-size: 2.25rem; }
            .hero p { font-size: 1.15rem; }
            th, td { padding: var(--space-sm) var(--space-md); }
            .date-col { display: none; }
        }
    </style>
</head>
<body>
    <nav class="nav">
        <div class="nav-container">
            <a href="/" class="nav-brand">Klaat</a>
        </div>
    </nav>

    <header class="hero">
        <div class="container" style="padding-bottom: 0;">
            <h1>Index of /${path}</h1>
            <p>Generated by Klaat Codelab Server</p>
        </div>
    </header>

    <main class="container">
        <div class="breadcrumb">
            <span class="material-symbols-outlined" style="font-size: 20px; color: var(--accent);">home</span>
            <a href="/">root</a> / 
            <#assign current = "">
            <#list path?split("/") as part>
                <#if part != "">
                    <#assign current = current + part + "/">
                    <a href="/${current}">${part}</a> /
                </#if>
            </#list>
        </div>

        <div class="listing-card">
            <table>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th class="date-col">Last Modified</th>
                        <th class="size-col">Size</th>
                    </tr>
                </thead>
                <tbody>
                    <#if path != "">
                        <tr>
                            <td>
                                <div class="name-col">
                                    <span class="material-symbols-outlined icon">folder_open</span>
                                    <a href="..">.. (Parent Directory)</a>
                                </div>
                            </td>
                            <td class="date-col">-</td>
                            <td class="size-col">-</td>
                        </tr>
                    </#if>
                    <#list items as item>
                        <tr>
                            <td>
                                <div class="name-col">
                                    <span class="material-symbols-outlined icon">
                                        ${item.isDirectory?string("folder", "description")}
                                    </span>
                                    <a href="${item.name}${item.isDirectory?string("/", "")}">${item.name}</a>
                                </div>
                            </td>
                            <td class="date-col">${item.lastModified}</td>
                            <td class="size-col">${item.size}</td>
                        </tr>
                    </#list>
                </tbody>
            </table>
        </div>
    </main>

    <footer class="footer">
        <div class="container" style="padding-bottom: 0;">
            <p>
                Get in touch for help, suggestions, or to <a href="https://www.buymeacoffee.com/kartikarora" target="_blank" rel="noopener noreferrer">buy me a coffee <span class="material-symbols-outlined" style="vertical-align: middle; font-size: 1.2em;">local_cafe</span></a>
            </p>
            <p class="text-muted" style="font-size: 0.875rem; margin-top: var(--space-md);">
                Made with Kotlin and GraalVM | Hosted on Cloudflare Pages<br>
                &copy; 2026 Kartik Arora
            </p>
        </div>
    </footer>
</body>
</html>
