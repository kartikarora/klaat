<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Index of /${path} - Klaat</title>

    <!-- Brand CSS (v2.0.0) -->
    <link href="https://distribute.kartikarora.me/css/kartikarora.2.0.0.css" rel="stylesheet">

    <!-- Google Fonts: Chivo, JetBrains Mono & Material Symbols -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Chivo:ital,wght@0,300..900;1,400&family=JetBrains+Mono:wght@400;500;600;700;800&family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">

    <!-- Favicons -->
    <link rel="icon" type="image/png" sizes="32x32" href="https://kartikarora.me/img/icon32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="https://kartikarora.me/img/icon16.png">
    <link rel="apple-touch-icon" sizes="180x180" href="https://kartikarora.me/img/icon180.png">
    <link rel="apple-touch-icon" sizes="192x192" href="https://kartikarora.me/img/icon192.png">

    <!-- Brand JS (v2.0.0) -->
    <script src="https://distribute.kartikarora.me/js/kartikarora.2.0.0.js" defer></script>

    <style>
        :root {
            --border-strong: var(--color-rule-strong);
        }

        html, body {
            overflow-x: clip;
        }

        body.studio-body {
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }

        main.studio-stage {
            flex: 1 0 auto;
            padding: var(--space-2xl) 0 var(--space-3xl);
        }

        .breadcrumb {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: var(--space-lg);
            font-family: var(--font-mono);
            font-size: var(--text-xs);
            color: var(--color-ink-muted);
        }

        .breadcrumb a {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 4px 10px;
            border-radius: var(--radius-sm);
            border: 2px solid var(--color-rule-strong);
            background: var(--color-card);
            color: var(--color-ink);
            text-decoration: none;
            font-weight: 700;
            box-shadow: 2px 2px 0 var(--color-rule-strong);
            transition: transform var(--dur-micro, 120ms) var(--ease-standard), box-shadow var(--dur-micro, 120ms) var(--ease-standard), border-color var(--dur-micro, 120ms) var(--ease-standard), color var(--dur-micro, 120ms) var(--ease-standard);
        }

        .breadcrumb a:hover {
            border-color: var(--color-signal);
            color: var(--color-signal);
            transform: translate(-1px, -1px);
            box-shadow: 3px 3px 0 var(--color-signal-secondary);
        }

        .listing-card {
            padding: 0 !important;
            overflow: hidden;
            border: 2px solid var(--color-rule-strong);
            border-radius: var(--radius-sm);
            box-shadow: 4px 4px 0 var(--color-rule-strong);
        }

        .listing-card.tokyo-card:hover {
            transform: none;
            border-color: var(--color-rule-strong);
            box-shadow: 4px 4px 0 var(--color-rule-strong);
        }

        .listing-card table {
            width: 100%;
            border-collapse: collapse;
        }

        .listing-card th,
        .listing-card td {
            text-align: left;
            padding: var(--space-md) var(--space-lg);
            border-bottom: 1px solid var(--color-rule);
        }

        .listing-card th {
            font-family: var(--font-mono);
            font-size: var(--text-xs);
            font-weight: 700;
            color: var(--color-ink);
            background: var(--color-paper-alt);
            text-transform: uppercase;
            letter-spacing: 0.08em;
            border-bottom: 2px solid var(--color-rule-strong);
        }

        .listing-card tr:last-child td {
            border-bottom: none;
        }

        .listing-card tbody tr {
            transition: background-color 0.2s var(--ease-standard);
        }

        .listing-card tbody tr:hover {
            background-color: var(--color-paper-alt);
        }

        .name-col {
            display: flex;
            align-items: center;
            gap: var(--space-sm);
        }

        .icon {
            font-size: 22px;
            color: var(--color-signal);
        }

        .name-col a {
            color: var(--color-ink);
            text-decoration: none;
            font-weight: 700;
            transition: color 0.2s var(--ease-standard);
        }

        .name-col a:hover {
            color: var(--color-signal);
            text-decoration: underline;
            text-underline-offset: 3px;
        }

        .size-col, .date-col {
            font-family: var(--font-mono);
            font-size: var(--text-xs);
            color: var(--color-ink-muted);
            font-variant-numeric: tabular-nums;
        }

        .breadcrumb a:focus-visible,
        .name-col a:focus-visible {
            outline: 2px solid var(--color-focus);
            outline-offset: 2px;
            transition: none;
        }

        @media (max-width: 768px) {
            .listing-card th, .listing-card td { padding: var(--space-sm) var(--space-md); }
            .date-col { display: none; }
        }

        @media (prefers-reduced-motion: reduce) {
            *, *::before, *::after {
                animation-duration: 0.01ms !important;
                transition-duration: 0.01ms !important;
            }
        }
    </style>
</head>
<body class="studio-body">
    <!-- Studio Header (matching kartikarora.me/brand-demo) -->
    <header class="studio-nav nav">
        <div class="studio-container">
            <div class="studio-nav-inner">
                <a href="/" class="studio-wordmark">Klaat</a>
                <div class="studio-nav-center">Local Preview Server</div>
                <nav class="studio-nav-links">
                    <a href="https://klaat.kartikarora.me" target="_blank" rel="noopener noreferrer" class="studio-link">Docs</a>
                    <a href="https://kartikarora.me" target="_blank" rel="noopener noreferrer" class="studio-link">kartikarora.me</a>
                </nav>
            </div>
        </div>
    </header>

    <main class="studio-stage">
        <div class="studio-container">
            <!-- Studio Hero (matching kartikarora.me/brand-demo) -->
            <header class="studio-hero hero" style="margin-bottom: var(--space-xl);">
                <div class="studio-pill-row">
                    <span class="badge badge-secondary">Local Preview</span>
                    <span class="studio-location">Local Development Server</span>
                </div>
                <h1 class="studio-headline">
                    Index of <span class="tokyo-highlight">/${path}</span>
                </h1>
                <p class="studio-bio" style="margin-bottom: 0;">
                    Generated by Klaat Codelab Server. Select a codelab directory below to launch the interactive tutorial.
                </p>
            </header>

            <div class="breadcrumb">
                <a href="/">
                    <span class="material-symbols-outlined" style="font-size: 16px; color: var(--color-signal);">home</span>
                    <span>root</span>
                </a>
                <#if path?has_content>
                    <#assign parts = path?split("/")>
                    <#assign currentPath = "">
                    <#list parts as part>
                        <#if part?has_content>
                            <#assign currentPath = currentPath + "/" + part>
                            <span>/</span>
                            <a href="${currentPath}/">${part}</a>
                        </#if>
                    </#list>
                </#if>
            </div>

            <div class="tokyo-card listing-card">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th class="date-col">Last Modified</th>
                            <th class="size-col">Size</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if path?has_content>
                            <tr>
                                <td>
                                    <div class="name-col">
                                        <span class="material-symbols-outlined icon">arrow_upward</span>
                                        <a href="../">..</a>
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
        </div>
    </main>

    <!-- Studio Footer (matching kartikarora.me/brand-demo) -->
    <footer class="studio-footer">
        <div class="studio-container">
            <div class="studio-footer-inner">
                <span>&copy; 2026 Kartik Arora</span>
                <span>Made with Kotlin &amp; GraalVM &middot; <a href="https://www.buymeacoffee.com/kartikarora" target="_blank" rel="noopener noreferrer" class="studio-link">Buy me a coffee</a></span>
            </div>
        </div>
    </footer>
</body>
</html>
