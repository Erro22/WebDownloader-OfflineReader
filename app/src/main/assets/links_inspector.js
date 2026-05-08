(function() {
    const currentHost = window.location.hostname;
    const blacklist = ['donate', 'login', 'action=', 'Special:', 'MediaWiki:', 'w/index.php', 'api.php'];
    
    const links = Array.from(document.querySelectorAll('a')).map(a => {
        const rect = a.getBoundingClientRect();
        const rawUrl = a.href;
        let url = rawUrl;
        let title = a.innerText.trim() || a.getAttribute('title') || 'Без названия';
        
        // Decode URL and Title for readability (e.g. Cyrillic)
        try {
            url = decodeURIComponent(rawUrl);
            title = decodeURIComponent(title);
        } catch(e) {}

        let category = "PROCHEЕ"; // Default: Other
        
        try {
            const urlObj = new URL(rawUrl);
            if (urlObj.hostname === currentHost) {
                if (rawUrl.includes('/wiki/')) {
                    category = "СТАТЬИ";
                } else {
                    category = "ВНУТРЕННИЕ";
                }
            } else if (rawUrl.match(/\.(pdf|zip|mp4|mp3|exe|apk)$/i)) {
                category = "МЕДИА";
            } else {
                category = "ВНЕШНИЕ";
            }
        } catch(e) {}

        return {
            title: title,
            url: rawUrl, // Keep raw URL for actual downloading
            displayUrl: url,
            x: Math.round(rect.left + window.scrollX),
            y: Math.round(rect.top + window.scrollY),
            width: Math.round(rect.width),
            height: Math.round(rect.height),
            category: category
        };
    }).filter(link => {
        // Validation
        if (!link.url || link.url.startsWith('javascript:') || link.url.startsWith('#')) return false;
        
        // Blacklist check
        const isBlacklisted = blacklist.some(word => link.url.toLowerCase().includes(word.toLowerCase()));
        if (isBlacklisted) return false;
        
        return true;
    });

    console.log("Antigravity: Extracted " + links.length + " clean links");
    if (window.AndroidInterface && window.AndroidInterface.onLinksExtracted) {
        window.AndroidInterface.onLinksExtracted(JSON.stringify(links));
    }
    return JSON.stringify(links);
})();

function highlightElement(targetUrl) {
    console.log("Antigravity: Highlighting URL: " + targetUrl);
    const allLinks = Array.from(document.querySelectorAll('a'));
    const candidates = allLinks.filter(a => a.href === targetUrl);
    
    if (candidates.length === 0) return;

    let el = candidates.find(a => {
        const rect = a.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }) || candidates[0];

    el.scrollIntoView({behavior: "smooth", block: "center"});
    
    const styleId = 'antigravity-highlight-style';
    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            @keyframes neon-pulse-v3 {
                0% { box-shadow: 0 0 10px #00ffcc, 0 0 20px #00ffcc; border: 2px solid #00ffcc; }
                50% { box-shadow: 0 0 30px #00ffcc, 0 0 50px #00ffcc; border: 2px solid #00ffcc; }
                100% { box-shadow: 0 0 10px #00ffcc, 0 0 20px #00ffcc; border: 2px solid #00ffcc; }
            }
            .neon-highlight-v3 {
                outline: 6px solid #00ffcc !important;
                outline-offset: 4px !important;
                animation: neon-pulse-v3 0.6s infinite !important;
                background-color: rgba(0, 255, 204, 0.2) !important;
                z-index: 2147483647 !important;
                position: relative !important;
                border-radius: 8px !important;
            }
        `;
        document.head.appendChild(style);
    }

    el.classList.add('neon-highlight-v3');
    setTimeout(() => el.classList.remove('neon-highlight-v3'), 3000);
}
