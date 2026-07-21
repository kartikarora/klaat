/**
 * Klaat Bridge Script
 * This script runs when a custom CSS is provided.
 * It extracts styling properties (colors, fonts, button shapes) from the external CSS,
 * dynamically loads any referenced Google Fonts, and applies them to the codelab UI.
 */
document.addEventListener('DOMContentLoaded', () => {
    // --- Font extraction and loading ---
    const GENERIC_FONTS = new Set([
        'serif', 'sans-serif', 'monospace', 'cursive', 'fantasy', 'system-ui',
        'ui-serif', 'ui-sans-serif', 'ui-monospace', 'ui-rounded',
        'inherit', 'initial', 'unset', 'revert',
        '-apple-system', 'blinkmacsystemfont', 'segoe ui', 'roboto',
        'oxygen', 'ubuntu', 'cantarell', 'fira sans', 'droid sans',
        'helvetica neue', 'helvetica', 'arial', 'verdana', 'tahoma',
        'trebuchet ms', 'georgia', 'times new roman', 'courier new',
        'lucida console', 'monaco',
    ]);

    const ICON_FONT_PATTERNS = [
        /webflow-icons/i, /^fa\s/i, /^fa\s\d/i, /fontawesome/i,
        /material[- ]icons/i, /icomoon/i, /glyphicons/i,
    ];

    function isIconOrGenericFont(name) {
        const lower = name.toLowerCase().trim();
        if (GENERIC_FONTS.has(lower)) return true;
        return ICON_FONT_PATTERNS.some(p => p.test(lower));
    }

    /**
     * Extract unique font families from all loaded stylesheets that came from
     * external CSS (not the codelab's own stylesheet).
     */
    function extractFontsFromStylesheets() {
        const fonts = new Set();
        for (const sheet of document.styleSheets) {
            // Only inspect external stylesheets (the custom CSS link)
            if (!sheet.href) continue;
            // Skip Google Fonts, Material Icons, and our own CDN assets
            if (sheet.href.includes('fonts.googleapis.com')) continue;
            if (sheet.href.includes('distribute.kartikarora.me')) continue;

            let rules;
            try {
                rules = sheet.cssRules || sheet.rules;
            } catch (e) {
                // CORS — fall back to fetching the CSS text
                fetchAndParseFonts(sheet.href, fonts);
                continue;
            }
            if (!rules) continue;
            extractFontsFromRules(rules, fonts);
        }
        return fonts;
    }

    function extractFontsFromRules(rules, fonts) {
        for (const rule of rules) {
            if (rule.style && rule.style.fontFamily) {
                parseFontFamilyValue(rule.style.fontFamily, fonts);
            }
            // Recurse into @media, @supports, etc.
            if (rule.cssRules) {
                extractFontsFromRules(rule.cssRules, fonts);
            }
        }
    }

    function parseFontFamilyValue(value, fonts) {
        // Split on commas, strip quotes and whitespace
        value.split(',').forEach(part => {
            const name = part.trim().replace(/^["']|["']$/g, '').trim();
            if (name && !isIconOrGenericFont(name)) {
                fonts.add(name);
            }
        });
    }

    /**
     * For CORS-blocked stylesheets, fetch the raw CSS and regex-parse font-family values.
     */
    function fetchAndParseFonts(url, fonts) {
        try {
            const xhr = new XMLHttpRequest();
            xhr.open('GET', url, false); // synchronous — runs before render
            xhr.send();
            if (xhr.status === 200) {
                const matches = xhr.responseText.match(/font-family\s*:\s*([^;}]+)/gi);
                if (matches) {
                    matches.forEach(m => {
                        const val = m.replace(/font-family\s*:\s*/i, '');
                        parseFontFamilyValue(val, fonts);
                    });
                }
            }
        } catch (e) {
            // Silently ignore fetch errors
        }
    }

    /**
     * Inject a Google Fonts <link> for the discovered font families.
     */
    function loadGoogleFonts(fontNames) {
        if (fontNames.length === 0) return;
        const families = fontNames
            .map(f => 'family=' + encodeURIComponent(f) + ':wght@100..900')
            .join('&');
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = `https://fonts.googleapis.com/css2?${families}&display=swap`;
        document.head.appendChild(link);
    }

    // Extract and load fonts immediately (before the style-probe timeout)
    const discoveredFonts = extractFontsFromStylesheets();
    loadGoogleFonts([...discoveredFonts]);

    // --- Style extraction via dummy elements ---
    const dummyContainer = document.createElement('div');
    dummyContainer.style.visibility = 'hidden';
    dummyContainer.style.position = 'absolute';
    dummyContainer.style.top = '-9999px';
    dummyContainer.innerHTML = `
        <div id="dummy-body" class="body"></div>
        <h1 id="dummy-h1">Heading</h1>
        <a id="dummy-a" href="#">Link</a>
        <button id="dummy-button" class="button btn w-button">Button</button>
        <div id="dummy-grid-card" class="grid-card"></div>
        <div id="dummy-success-message" class="success-message"></div>
        <div id="dummy-error-message" class="error-message"></div>
        <div id="dummy-file-upload-info" class="w-file-upload-info"></div>
    `;
    document.body.appendChild(dummyContainer);

    // Give the browser a moment to apply external CSS + loaded fonts
    setTimeout(() => {
        const bodyStyle = window.getComputedStyle(document.body);
        const h1Style = window.getComputedStyle(document.getElementById('dummy-h1'));
        const aStyle = window.getComputedStyle(document.getElementById('dummy-a'));
        const buttonStyle = window.getComputedStyle(document.getElementById('dummy-button'));
        
        const gridCard = document.getElementById('dummy-grid-card');
        const successMsg = document.getElementById('dummy-success-message');
        const errorMsg = document.getElementById('dummy-error-message');
        const infoMsg = document.getElementById('dummy-file-upload-info');

        const gridCardStyle = window.getComputedStyle(gridCard);
        const successStyle = window.getComputedStyle(successMsg);
        const errorStyle = window.getComputedStyle(errorMsg);
        const infoStyle = window.getComputedStyle(infoMsg);

        const fontFamily = bodyStyle.fontFamily;
        const h1FontFamily = h1Style.fontFamily || fontFamily;
        const textColor = bodyStyle.color;

        // Button Shapes & Fonts
        const buttonBorderRadius = buttonStyle.borderRadius;
        const buttonTextTransform = buttonStyle.textTransform;
        const buttonFontWeight = buttonStyle.fontWeight;
        const buttonBorder = buttonStyle.border;
        const buttonBoxShadow = buttonStyle.boxShadow;

        // Grid card styles or fallbacks
        const cardBg = gridCardStyle.backgroundColor !== 'rgba(0, 0, 0, 0)' ? gridCardStyle.backgroundColor : '#ffffff';
        const cardRadius = gridCardStyle.borderRadius !== '0px' && gridCardStyle.borderRadius !== '0' ? gridCardStyle.borderRadius : '8px';
        const cardShadow = gridCardStyle.boxShadow !== 'none' ? gridCardStyle.boxShadow : '0px 1px 2px 0px rgba(60,64,67,0.3)';
        const cardBorder = gridCardStyle.border !== '0px none rgb(51, 51, 51)' && gridCardStyle.border !== 'none' ? gridCardStyle.border : 'none';

        // Success styles or fallbacks
        const successBg = successStyle.backgroundColor !== 'rgba(0, 0, 0, 0)' ? successStyle.backgroundColor : '#E6F4EA';
        const successBorderColor = successStyle.borderColor !== 'rgb(0, 0, 0)' && successStyle.borderColor !== 'rgba(0, 0, 0, 0)' ? successStyle.borderColor : '#137333';
        const successTextColor = successStyle.color !== 'rgb(51, 51, 51)' ? successStyle.color : '#212124';

        // Error styles or fallbacks
        const errorBg = errorStyle.backgroundColor !== 'rgba(0, 0, 0, 0)' ? errorStyle.backgroundColor : '#FEF7E0';
        const errorBorderColor = errorStyle.borderColor !== 'rgb(0, 0, 0)' && errorStyle.borderColor !== 'rgba(0, 0, 0, 0)' ? errorStyle.borderColor : '#EA8600';
        const errorTextColor = errorStyle.color !== 'rgb(51, 51, 51)' ? errorStyle.color : '#212124';

        // Info styles or fallbacks
        const infoBg = infoStyle.backgroundColor !== 'rgba(0, 0, 0, 0)' ? infoStyle.backgroundColor : '#E8F0FE';
        const infoBorderColor = infoStyle.borderColor !== 'rgb(0, 0, 0)' && infoStyle.borderColor !== 'rgba(0, 0, 0, 0)' ? infoStyle.borderColor : '#185ABC';
        const infoTextColor = infoStyle.color !== 'rgb(51, 51, 51)' ? infoStyle.color : '#212124';

        // Find a primary color
        let primaryColor = aStyle.color;
        if (primaryColor === 'rgb(0, 0, 238)' || primaryColor === 'rgba(0, 0, 0, 0)') {
            primaryColor = buttonStyle.backgroundColor;
            if (primaryColor === 'rgba(0, 0, 0, 0)') {
                primaryColor = '#1A73E8'; // Fallback to Google blue
            }
        }

        let bgColor = bodyStyle.backgroundColor;
        if (bgColor === 'rgba(0, 0, 0, 0)') {
            bgColor = '#ffffff';
        }

        // Inject a style block to override the codelab UI
        const style = document.createElement('style');
        style.innerHTML = `
            body, google-codelab, #codelab-title, #drawer, .step__body, google-codelab #drawer ol li a {
                font-family: ${fontFamily} !important;
                color: ${textColor} !important;
                line-height: 1.6 !important;
            }
            google-codelab #codelab-title h1 {
                font-family: ${h1FontFamily} !important;
                font-weight: 700 !important;
            }
            #codelab-title {
                background: ${bgColor} !important;
                box-shadow: 0 1px 3px rgba(0,0,0,0.05) !important;
                border-bottom: 1px solid rgba(0,0,0,0.06) !important;
            }
            #drawer {
                background: ${bgColor} !important;
                border-right: 1px solid rgba(0,0,0,0.06) !important;
            }
            #drawer ol li a {
                transition: all 0.2s ease-in-out !important;
                padding: 14px 20px !important;
                border-left: 3px solid transparent !important;
            }
            #drawer ol li a:hover {
                background: rgba(0,0,0,0.02) !important;
                color: ${primaryColor} !important;
            }
            #drawer ol li[selected] a {
                border-left-color: ${primaryColor} !important;
                color: ${primaryColor} !important;
                font-weight: 600 !important;
                background: rgba(0,0,0,0.01) !important;
            }
            #next-step, #done {
                background-color: ${primaryColor} !important;
                color: #ffffff !important;
                border-radius: ${buttonBorderRadius} !important;
                text-transform: ${buttonTextTransform} !important;
                font-weight: ${buttonFontWeight} !important;
                border: ${buttonBorder} !important;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1) !important;
                transition: all 0.2s ease !important;
                padding: 10px 20px !important;
            }
            #next-step:hover, #done:hover {
                transform: translateY(-1px) !important;
                box-shadow: 0 4px 8px rgba(0,0,0,0.15) !important;
                opacity: 0.95 !important;
            }
            .step__header {
                background-color: ${primaryColor} !important;
                color: #ffffff !important;
            }
            #drawer ol li[selected] .step:before, #drawer ol li[completed] .step:before {
                background-color: ${primaryColor} !important;
            }
            google-codelab #drawer ol li a .step {
                color: ${textColor} !important;
            }
            
            /* Override aside blocks / callout cards to look extremely premium */
            google-codelab-step .instructions aside,
            google-codelab-step .instructions aside.note,
            google-codelab-step .instructions aside.notice,
            google-codelab-step .instructions aside.warning,
            google-codelab-step .instructions aside.tip,
            google-codelab-step .instructions aside.special,
            google-codelab-step aside.callout,
            google-codelab-step aside.positive,
            google-codelab-step aside.negative {
                font-family: ${fontFamily} !important;
                border-radius: 10px !important;
                box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.02) !important;
                border: 1px solid rgba(0,0,0,0.04) !important;
                border-left: 5px solid !important;
                padding: 18px 24px !important;
                margin: 24px 0 !important;
                line-height: 1.6 !important;
                position: relative !important;
            }

            google-codelab-step .instructions aside.tip,
            google-codelab-step .instructions aside.special,
            google-codelab-step aside.positive {
                background-color: ${successBg} !important;
                border-left-color: ${successBorderColor} !important;
                color: ${successTextColor} !important;
            }

            google-codelab-step .instructions aside.note,
            google-codelab-step .instructions aside.notice,
            google-codelab-step .instructions aside.warning,
            google-codelab-step aside.negative {
                background-color: ${errorBg} !important;
                border-left-color: ${errorBorderColor} !important;
                color: ${errorTextColor} !important;
            }

            google-codelab-step aside.callout {
                background-color: ${infoBg} !important;
                border-left-color: ${infoBorderColor} !important;
                color: ${infoTextColor} !important;
            }

            /* Code containers & pre blocks */
            google-codelab-step pre {
                background-color: #0f172a !important; /* Premium Slate 900 background */
                border-radius: 8px !important;
                box-shadow: 0 4px 12px rgba(0,0,0,0.08) !important;
                border: 1px solid rgba(255,255,255,0.04) !important;
                padding: 20px !important;
                margin: 20px 0 !important;
            }
            google-codelab-step code {
                font-family: 'JetBrains Mono', monospace !important;
            }
            .code-container {
                border-radius: 8px !important;
                overflow: hidden !important;
                transition: transform 0.2s ease !important;
            }
            .code-container:hover {
                transform: translateY(-1px) !important;
            }
            
            /* Offline renderer specific classes */
            .codelab__toc {
                background: ${bgColor} !important;
                color: ${textColor} !important;
                border-right: 1px solid rgba(0,0,0,0.06) !important;
            }
            .toc-item--current {
                border-left-color: ${primaryColor} !important;
                background: rgba(0,0,0,0.01) !important;
                font-weight: 600 !important;
            }
            .toc-item--complete {
                color: ${primaryColor} !important;
            }
            .step__header {
                background: ${primaryColor} !important;
                box-shadow: 0 1px 3px rgba(0,0,0,0.05) !important;
            }
        `;
        document.head.appendChild(style);

        // Cleanup dummy elements
        dummyContainer.remove();
    }, 200);
});
