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
    console.log("Antigravity: Highlighting with Native Overlay: " + targetUrl);
    
    const candidates = Array.from(document.querySelectorAll('a')).filter(a => a.href === targetUrl);
    
    if (candidates.length === 0) {
        console.error("Antigravity: Link not found: " + targetUrl);
        return;
    }

    let el = candidates.find(a => {
        const rect = a.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }) || candidates[0];

    // --- SCROLL INTO VIEW ---
    const rectBefore = el.getBoundingClientRect();
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const targetY = rectBefore.top + scrollTop - (window.innerHeight / 2) + (rectBefore.height / 2);
    
    window.scrollTo({
        top: targetY,
        behavior: 'smooth'
    });
    
    // Wait for scroll to settle, then get viewport coordinates
    setTimeout(() => {
        const rectAfter = el.getBoundingClientRect();
        
        // Final compensation check for sticky headers
        if (rectAfter.top < 80) {
            window.scrollBy({ top: -100, behavior: 'smooth' });
            setTimeout(() => {
                const finalRect = el.getBoundingClientRect();
                sendToAndroid(finalRect);
            }, 300);
        } else {
            sendToAndroid(rectAfter);
        }
    }, 600);

    function sendToAndroid(rect) {
        if (window.AndroidInterface && window.AndroidInterface.showNativeHighlight) {
            window.AndroidInterface.showNativeHighlight(
                rect.left, 
                rect.top, 
                rect.width, 
                rect.height
            );
        }
    }
}
