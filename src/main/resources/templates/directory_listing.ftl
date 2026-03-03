<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Index of /${path} - Klaat</title>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Readex+Pro:wght@300;400;500;700&family=Roboto+Mono&display=swap">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" />
    <style>
        :root {
            --primary: #303F9F;
            --primary-dark: #1a237e;
            --primary-light: #5c6bc0;
            --accent: #FFC107;
            --secondary: #212121;
            --background: #ffffff;
            --surface: #f8f9fa;
            --text-primary: #212121;
            --text-secondary: #666666;
            --text-light: #dedede;
            --border: #e0e0e0;
            --shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            --shadow-hover: 0 4px 16px rgba(0, 0, 0, 0.15);
            --code-bg: #f5f5f5;
        }

        @media (prefers-color-scheme: dark) {
            body:not(.light-mode) {
                --background: #121212;
                --surface: #1e1e1e;
                --text-primary: #e0e0e0;
                --text-secondary: #aaaaaa;
                --border: #333333;
                --code-bg: #2d2d2d;
                --shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
                --shadow-hover: 0 4px 16px rgba(0, 0, 0, 0.5);
            }
        }

        body.dark-mode {
            --background: #121212;
            --surface: #1e1e1e;
            --text-primary: #e0e0e0;
            --text-secondary: #aaaaaa;
            --border: #333333;
            --code-bg: #2d2d2d;
            --shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
            --shadow-hover: 0 4px 16px rgba(0, 0, 0, 0.5);
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Readex Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: var(--text-primary);
            background: var(--background);
            transition: background-color 0.3s, color 0.3s;
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }

        .main-content {
            flex: 1;
            background: var(--primary);
            color: white;
        }

        .nav {
            background: var(--primary);
            position: sticky;
            top: 0;
            z-index: 1000;
        }

        .nav-content {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .logo {
            font-size: 24px;
            font-weight: 700;
            color: #fff;
            text-transform: uppercase;
            letter-spacing: 1px;
            text-decoration: none;
        }

        .theme-toggle {
            background: transparent;
            border: 1px solid rgba(255, 255, 255, 0.3);
            color: white;
            cursor: pointer;
            padding: 8px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s;
        }

        .theme-toggle:hover {
            background: rgba(255, 255, 255, 0.1);
            border-color: var(--accent);
        }

        .hero {
            background: var(--primary);
            color: white;
            padding: 60px 0;
            text-align: center;
        }

        .hero h1 {
            font-size: 48px;
            font-weight: 800;
            margin-bottom: 8px;
            letter-spacing: -1px;
        }

        .hero p {
            font-size: 18px;
            opacity: 0.9;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 0 24px 80px;
        }

        .breadcrumb {
            margin-bottom: 24px;
            font-size: 16px;
            color: rgba(255, 255, 255, 0.7);
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 4px;
        }

        .breadcrumb a {
            color: #fff;
            text-decoration: none;
            font-weight: 500;
            border-bottom: 1px dashed rgba(255, 255, 255, 0.5);
            transition: all 0.2s;
        }

        .breadcrumb a:hover {
            color: var(--accent);
            border-bottom-color: var(--accent);
        }

        .listing-card {
            background: var(--surface);
            border-radius: 12px;
            border: 1px solid var(--border);
            overflow: hidden;
            box-shadow: var(--shadow);
            transition: box-shadow 0.3s;
            color: var(--text-primary);
        }

        .listing-card:hover {
            box-shadow: var(--shadow-hover);
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            text-align: left;
            padding: 16px 24px;
            border-bottom: 1px solid var(--border);
        }

        th {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-secondary);
            background: rgba(0, 0, 0, 0.05);
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        body.dark-mode th {
            background: rgba(255, 255, 255, 0.05);
        }

        tr:last-child td {
            border-bottom: none;
        }

        tr:hover {
            background-color: rgba(0, 0, 0, 0.02);
        }

        body.dark-mode tr:hover {
            background-color: rgba(255, 255, 255, 0.05);
        }

        .name-col {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .icon {
            font-size: 24px;
            color: var(--text-secondary);
        }

        .icon.folder {
            color: var(--primary-light);
        }

        body.dark-mode .icon.folder {
            color: var(--accent);
        }

        .name-col a {
            color: var(--text-primary);
            text-decoration: none;
            font-weight: 500;
            transition: color 0.2s;
        }

        .name-col a:hover {
            color: var(--accent);
        }

        body.dark-mode .name-col a:hover {
            color: var(--accent);
        }

        .size-col, .date-col {
            font-family: 'Roboto Mono', monospace;
            font-size: 13px;
            color: var(--text-secondary);
        }

        .footer {
            background: var(--primary);
            padding: 48px 0;
            text-align: center;
            color: #fff;
        }

        .footer p {
            margin-bottom: 8px;
            font-size: 14px;
        }

        .material-symbols-outlined {
            vertical-align: middle;
        }

        @media (max-width: 768px) {
            .hero h1 {
                font-size: 32px;
            }
            .hero p {
                font-size: 16px;
            }
            th, td {
                padding: 12px 16px;
            }
            .date-col {
                display: none;
            }
        }
    </style>
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const themeToggle = document.getElementById('theme-toggle');
            const themeIcon = themeToggle.querySelector('.material-symbols-outlined');
            const body = document.body;

            const getStoredTheme = () => localStorage.getItem('theme');
            const setStoredTheme = (theme) => localStorage.setItem('theme', theme);

            const applyTheme = (theme) => {
                const isDark = theme === 'dark' || (theme === null && window.matchMedia('(prefers-color-scheme: dark)').matches);
                if (isDark) {
                    body.classList.add('dark-mode');
                    body.classList.remove('light-mode');
                    themeIcon.textContent = 'light_mode';
                } else {
                    body.classList.remove('dark-mode');
                    body.classList.add('light-mode');
                    themeIcon.textContent = 'dark_mode';
                }
            };

            themeToggle.addEventListener('click', () => {
                const currentTheme = body.classList.contains('dark-mode') ? 'dark' : 'light';
                const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
                applyTheme(newTheme);
                setStoredTheme(newTheme);
            });

            applyTheme(getStoredTheme());
        });
    </script>
</head>
<body>
    <nav class="nav">
        <div class="nav-content">
            <a href="/" class="logo">Klaat</a>
            <button id="theme-toggle" class="theme-toggle" aria-label="Toggle theme">
                <span class="material-symbols-outlined">dark_mode</span>
            </button>
        </div>
    </nav>

    <div class="main-content">
        <header class="hero">
            <div class="container">
                <h1>Index of /${path}</h1>
                <p>Generated by Klaat Codelab Server</p>
            </div>
        </header>

        <main class="container">
            <div class="breadcrumb">
                <span class="material-symbols-outlined" style="font-size: 20px;">home</span>
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
                                        <span class="material-symbols-outlined icon folder">folder_open</span>
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
                                        <span class="material-symbols-outlined icon ${item.isDirectory?string('folder', '')}">
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
    </div>

    <footer class="footer">
        <div class="container">
            <p>&copy; 2025 Kartik Arora. Built with ❤️ using Kotlin and GraalVM.</p>
            <p>Klaat is inspired by Google's <a href="https://github.com/googlecodelabs/tools" target="_blank" style="color: var(--accent); text-decoration: none;">claat</a> tool.</p>
        </div>
    </footer>
</body>
</html>
