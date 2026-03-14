/* MyOffGridAI Setup Wizard — Common Utilities */

/**
 * Makes a JSON API request with error handling.
 * @param {string} url - The API endpoint
 * @param {object} options - Fetch options (method, body, etc.)
 * @returns {Promise<object>} - The parsed JSON response
 */
async function api(url, options = {}) {
    const defaults = {
        headers: { 'Content-Type': 'application/json' },
    };
    const config = { ...defaults, ...options };
    if (config.body && typeof config.body === 'object') {
        config.body = JSON.stringify(config.body);
    }

    const response = await fetch(url, config);
    return response.json();
}

/**
 * Shows a message element with the given text and type.
 * @param {string} id - The message element ID
 * @param {string} text - The message text
 * @param {string} type - 'error' or 'success'
 */
function showMessage(id, text, type) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = text;
    el.className = 'message ' + type + ' visible';
}

/**
 * Hides a message element.
 * @param {string} id - The message element ID
 */
function hideMessage(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.className = 'message';
}

/**
 * Creates signal strength bars SVG for WiFi networks.
 * @param {number} signal - Signal strength (0-100 or negative dBm)
 * @returns {string} - HTML for signal bars
 */
function signalBars(signal) {
    // Normalize: if negative (dBm), convert to 0-100 scale
    let strength = signal;
    if (signal < 0) {
        strength = Math.min(100, Math.max(0, (signal + 100) * 2));
    }

    const bars = [
        { height: 4, threshold: 1 },
        { height: 8, threshold: 25 },
        { height: 12, threshold: 50 },
        { height: 16, threshold: 75 },
    ];

    let html = '<div class="signal-bars">';
    bars.forEach(bar => {
        const active = strength >= bar.threshold ? ' active' : '';
        html += '<div class="signal-bar' + active + '" style="height:' + bar.height + 'px"></div>';
    });
    html += '</div>';
    return html;
}

/**
 * Sets a button to loading state.
 * @param {HTMLElement} btn - The button element
 * @param {string} text - Loading text to display
 */
function setLoading(btn, text) {
    btn.disabled = true;
    btn.dataset.originalText = btn.textContent;
    btn.textContent = text;
}

/**
 * Restores a button from loading state.
 * @param {HTMLElement} btn - The button element
 */
function clearLoading(btn) {
    btn.disabled = false;
    btn.textContent = btn.dataset.originalText || btn.textContent;
}
