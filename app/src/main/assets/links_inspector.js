(function() {
    const currentHost = window.location.hostname;
    
    const links = Array.from(document.querySelectorAll('a')).map(a => {
        const rect = a.getBoundingClientRect();
        const url = a.href;
        let category = "EXTERNAL";
        
        try {
            const urlObj = new URL(url);
            if (urlObj.hostname === currentHost) {
                category = "INTERNAL";
            } else if (url.match(/\.(pdf|zip|mp4|mp3|exe|apk)$/i)) {
                category = "MEDIA";
            }
        } catch(e) {}

        return {
            title: a.innerText.trim() || a.getAttribute('title') || 'Без названия',
            url: url,
            x: Math.round(rect.left + window.scrollX),
            y: Math.round(rect.top + window.scrollY),
            width: Math.round(rect.width),
            height: Math.round(rect.height),
            category: category,
            estimatedWeight: document.body.innerText.length
        };
    }).filter(link => {
        if (!link.url || link.url.startsWith('javascript:') || link.url.startsWith('#')) return false;
        return true;
    });

    console.log("Antigravity: Extracted " + links.length + " links");
    if (window.AndroidInterface && window.AndroidInterface.onLinksExtracted) {
        window.AndroidInterface.onLinksExtracted(JSON.stringify(links));
    }
    return JSON.stringify(links);
})();

function highlightElement(targetUrl) {
    console.log("Antigravity: Highlighting URL: " + targetUrl);
    
    // Find all links with this URL
    const allLinks = Array.from(document.querySelectorAll('a'));
    const candidates = allLinks.filter(a => a.href === targetUrl);
    
    if (candidates.length === 0) {
        console.warn("Antigravity: No element found for URL: " + targetUrl);
        return;
    }

    // Prefer visible candidates
    let el = candidates.find(a => {
        const rect = a.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }) || candidates[0];

    console.log("Antigravity: Found element, scrolling...");
    
    el.scrollIntoView({behavior: "smooth", block: "center"});
    
    const styleId = 'antigravity-highlight-style';
    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            @keyframes neon-pulse-v2 {
                0% { box-shadow: 0 0 10px #ff0000, 0 0 20px #ff0000; border: 2px solid #ff0000; }
                50% { box-shadow: 0 0 30px #ff0000, 0 0 50px #ff0000; border: 2px solid #ff0000; }
                100% { box-shadow: 0 0 10px #ff0000, 0 0 20px #ff0000; border: 2px solid #ff0000; }
            }
            .neon-highlight-v2 {
                outline: 5px solid #ff0000 !important;
                outline-offset: 4px !important;
                animation: neon-pulse-v2 0.6s infinite !important;
                background-color: rgba(255, 0, 0, 0.3) !important;
                z-index: 2147483647 !important;
                position: relative !important;
                border-radius: 4px !important;
            }
        `;
        document.head.appendChild(style);
    }

    el.classList.add('neon-highlight-v2');
    
    setTimeout(() => {
        el.classList.remove('neon-highlight-v2');
        console.log("Antigravity: Highlight removed");
    }, 4000);
}
