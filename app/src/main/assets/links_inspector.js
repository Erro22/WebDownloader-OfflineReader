(function() {
    const currentHost = window.location.hostname;
    const blacklist = ['donate', 'login', 'action=', 'Special:', 'MediaWiki:', 'w/index.php', 'api.php'];
    
    // --- STYLES FOR SELECTIVE HIGHLIGHTING ---
    const styleId = 'antigravity-inspector-styles';
    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            .antigravity-link-highlight {
                background-color: rgba(255, 0, 0, 0.2) !important;
                outline: 1px solid rgba(255, 0, 0, 0.4) !important;
                transition: all 0.2s ease !important;
                cursor: pointer !important;
                border-radius: 2px !important;
            }
            .antigravity-link-highlight.selected {
                background-color: rgba(0, 255, 0, 0.4) !important;
                outline: 2px solid rgba(0, 255, 0, 0.8) !important;
                box-shadow: 0 0 10px rgba(0, 255, 0, 0.5) !important;
            }
        `;
        document.head.appendChild(style);
    }

    const allLinks = Array.from(document.querySelectorAll('a'));
    const extractedLinks = allLinks.map(a => {
        const rect = a.getBoundingClientRect();
        const rawUrl = a.href;
        let url = rawUrl;
        let title = a.innerText.trim() || a.getAttribute('title') || 'Без названия';
        
        try {
            url = decodeURIComponent(rawUrl);
            title = decodeURIComponent(title);
        } catch(e) {}

        let category = "PROCHEЕ";
        try {
            const urlObj = new URL(rawUrl);
            if (urlObj.hostname === currentHost) {
                if (rawUrl.includes('/wiki/')) category = "СТАТЬИ";
                else category = "ВНУТРЕННИЕ";
            } else if (rawUrl.match(/\.(pdf|zip|mp4|mp3|exe|apk)$/i)) {
                category = "МЕДИА";
            } else {
                category = "ВНЕШНИЕ";
            }
        } catch(e) {}

        // --- APPLY INITIAL HIGHLIGHT ---
        if (!a.classList.contains('antigravity-link-highlight')) {
            a.classList.add('antigravity-link-highlight');
            a.onclick = function(e) {
                e.preventDefault();
                e.stopPropagation();
                const isSelected = a.classList.toggle('selected');
                if (window.AndroidInterface && window.AndroidInterface.onLinkToggledOnPage) {
                    window.AndroidInterface.onLinkToggledOnPage(rawUrl, isSelected);
                }
                return false;
            };
        }

        return {
            title: title,
            url: rawUrl,
            displayUrl: url,
            x: Math.round(rect.left + window.scrollX),
            y: Math.round(rect.top + window.scrollY),
            width: Math.round(rect.width),
            height: Math.round(rect.height),
            category: category
        };
    }).filter(link => {
        if (!link.url || link.url.startsWith('javascript:') || link.url.startsWith('#')) return false;
        const isBlacklisted = blacklist.some(word => link.url.toLowerCase().includes(word.toLowerCase()));
        return !isBlacklisted;
    });

    if (window.AndroidInterface && window.AndroidInterface.onLinksExtracted) {
        window.AndroidInterface.onLinksExtracted(JSON.stringify(extractedLinks));
    }
    return JSON.stringify(extractedLinks);
})();

// --- GLOBAL FUNCTIONS FOR ANDROID ---
function updateLinkSelection(targetUrl, isSelected) {
    const links = Array.from(document.querySelectorAll('a')).filter(a => a.href === targetUrl);
    links.forEach(a => {
        if (isSelected) {
            a.classList.add('selected');
        } else {
            a.classList.remove('selected');
        }
    });
}

function highlightElement(targetUrl) {
    // Keep this for the 'Magnifying Glass' feature
    const candidates = Array.from(document.querySelectorAll('a')).filter(a => a.href === targetUrl);
    if (candidates.length === 0) return;
    let el = candidates[0];
    const rect = el.getBoundingClientRect();
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const targetY = rect.top + scrollTop - (window.innerHeight / 2) + (rect.height / 2);
    window.scrollTo({ top: targetY, behavior: 'smooth' });
}
