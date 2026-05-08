(function() {
    const currentHost = window.location.hostname;
    const blacklist = ['donate', 'login', 'action=', 'Special:', 'MediaWiki:', 'w/index.php', 'api.php'];
    
    const links = Array.from(document.querySelectorAll('a')).map(a => {
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
        if (isBlacklisted) return false;
        return true;
    });

    if (window.AndroidInterface && window.AndroidInterface.onLinksExtracted) {
        window.AndroidInterface.onLinksExtracted(JSON.stringify(links));
    }
    return JSON.stringify(links);
})();

function highlightElement(targetUrl) {
    console.log("Antigravity: Highlighting with Red Marker: " + targetUrl);
    
    // Safely escape URL for CSS selector
    const selector = 'a[href="' + targetUrl.replace(/"/g, '\\"') + '"]';
    const allLinks = Array.from(document.querySelectorAll('a'));
    const candidates = allLinks.filter(a => a.href === targetUrl);
    
    if (candidates.length === 0) {
        console.error("Antigravity: Link not found: " + targetUrl);
        return;
    }

    let el = candidates.find(a => {
        const rect = a.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }) || candidates[0];

    // --- STICKY HEADER COMPENSATION ---
    const rect = el.getBoundingClientRect();
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const targetY = rect.top + scrollTop - (window.innerHeight / 2) + (rect.height / 2);
    
    // Scroll to center but with a safety offset for sticky headers
    window.scrollTo({
        top: targetY,
        behavior: 'smooth'
    });
    
    // Additional micro-scroll if target is too high (under potential header)
    setTimeout(() => {
        const newRect = el.getBoundingClientRect();
        if (newRect.top < 100) { // If it's in the top 100px, it might be under a header
            window.scrollBy({ top: -120, behavior: 'smooth' });
        }
    }, 500);

    // --- RED MARKER STYLE ---
    const styleId = 'antigravity-red-marker-style';
    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            @keyframes red-pulse-v3 {
                0% { box-shadow: 0 0 10px rgba(255, 0, 0, 0.5); }
                50% { box-shadow: 0 0 25px rgba(255, 0, 0, 0.8), 0 0 40px rgba(255, 0, 0, 0.4); }
                100% { box-shadow: 0 0 10px rgba(255, 0, 0, 0.5); }
            }
            .antigravity-red-marker {
                background-color: rgba(255, 0, 0, 0.6) !important;
                box-shadow: 0 0 15px 5px rgba(255, 0, 0, 0.4) !important;
                border-radius: 6px !important;
                transition: all 0.3s ease !important;
                animation: red-pulse-v3 1s infinite !important;
                z-index: 2147483647 !important;
                position: relative !important;
                color: white !important;
                padding: 2px 4px !important;
                margin: -2px -4px !important;
            }
        `;
        document.head.appendChild(style);
    }

    el.classList.add('antigravity-red-marker');
    
    // Remove after 10 seconds
    setTimeout(() => {
        el.classList.remove('antigravity-red-marker');
    }, 10000);
}
