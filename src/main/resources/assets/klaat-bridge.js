/**
 * Klaat Bridge Script
 * This script runs when a custom CSS is provided.
 * It extracts styling properties (colors, fonts) from the external CSS
 * by checking computed styles on dummy standard elements,
 * and then dynamically applies them to the codelab's UI elements
 * (which normally use hardcoded Google colors).
 */
document.addEventListener('DOMContentLoaded', () => {
    // Create dummy elements to read styles from the external CSS.
    const dummyContainer = document.createElement('div');
    dummyContainer.style.visibility = 'hidden';
    dummyContainer.style.position = 'absolute';
    dummyContainer.style.top = '-9999px';
    dummyContainer.innerHTML = `
        <div id="dummy-body" class="body"></div>
        <h1 id="dummy-h1">Heading</h1>
        <a id="dummy-a" href="#">Link</a>
        <button id="dummy-button" class="button btn w-button">Button</button>
    `;
    document.body.appendChild(dummyContainer);

    // Give the browser a moment to apply external CSS
    setTimeout(() => {
        const bodyStyle = window.getComputedStyle(document.body);
        const h1Style = window.getComputedStyle(document.getElementById('dummy-h1'));
        const aStyle = window.getComputedStyle(document.getElementById('dummy-a'));
        const buttonStyle = window.getComputedStyle(document.getElementById('dummy-button'));

        // Fallbacks are checked so we don't accidentally pick up default unstyled browser styles
        const fontFamily = bodyStyle.fontFamily;
        const h1FontFamily = h1Style.fontFamily || fontFamily;
        const textColor = bodyStyle.color;
        
        // Button Shapes & Fonts
        const buttonBorderRadius = buttonStyle.borderRadius;
        const buttonTextTransform = buttonStyle.textTransform;
        const buttonFontWeight = buttonStyle.fontWeight;
        const buttonBorder = buttonStyle.border;
        const buttonBoxShadow = buttonStyle.boxShadow;
        
        // Find a primary color: try a link color if it's not the default blue, otherwise try a button background
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
            }
            google-codelab #codelab-title h1 {
                font-family: ${h1FontFamily} !important;
            }
            #codelab-title {
                background: ${bgColor} !important;
            }
            #drawer {
                background: ${bgColor} !important;
            }
            #next-step, #done {
                background-color: ${primaryColor} !important;
                color: #ffffff !important;
                border-radius: ${buttonBorderRadius} !important;
                text-transform: ${buttonTextTransform} !important;
                font-weight: ${buttonFontWeight} !important;
                border: ${buttonBorder} !important;
                box-shadow: ${buttonBoxShadow} !important;
            }
            .step__header {
                background-color: ${primaryColor} !important;
                color: #ffffff !important;
            }
            #drawer ol li[selected] .step:before, #drawer ol li[completed] .step:before {
                background-color: ${primaryColor} !important;
            }
            #drawer ol li[selected] a {
                border-color: ${primaryColor} !important;
                color: ${primaryColor} !important;
            }
            google-codelab #drawer ol li a .step {
                color: ${textColor} !important;
            }
            google-codelab #drawer ol li a {
                background: ${bgColor} !important;
            }
            google-codelab #drawer ol li[selected] a {
                background: ${bgColor} !important;
            }
            
            /* Offline renderer specific classes */
            .codelab__toc {
                background: ${bgColor} !important;
                color: ${textColor} !important;
            }
            .toc-item--current {
                border-left-color: ${primaryColor} !important;
            }
            .toc-item--complete {
                color: ${primaryColor} !important;
            }
            .step__header {
                background: ${primaryColor} !important;
            }
        `;
        document.head.appendChild(style);
        
        // Cleanup dummy elements
        dummyContainer.remove();
    }, 150);
});
