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
            estimatedWeight: document.body.innerText.length // Very rough estimate
        };
    }).filter(link => {
        if (!link.url || link.url.startsWith('javascript:') || link.url.startsWith('#')) return false;
        return true;
    });

    if (window.AndroidInterface && window.AndroidInterface.onLinksExtracted) {
        window.AndroidInterface.onLinksExtracted(JSON.stringify(links));
    }
    return JSON.stringify(links);
})();

function highlightElement(x, y) {
    const el = document.elementFromPoint(x - window.scrollX, y - window.scrollY);
    if (el) {
        el.scrollIntoView({behavior: "smooth", block: "center"});
        
        // Neon Red Highlight
        const styleId = 'antigravity-highlight-style';
        if (!document.getElementById(styleId)) {
            const style = document.createElement('style');
            style.id = styleId;
            style.innerHTML = `
                @keyframes neon-pulse {
                    0% { box-shadow: 0 0 5px #ff0000, 0 0 10px #ff0000; }
                    50% { box-shadow: 0 0 20px #ff0000, 0 0 30px #ff0000; }
                    100% { box-shadow: 0 0 5px #ff0000, 0 0 10px #ff0000; }
                }
                .neon-highlight {
                    outline: 3px solid #ff0000 !important;
                    animation: neon-pulse 1s infinite !important;
                    background-color: rgba(255, 0, 0, 0.1) !important;
                    transition: all 0.3s ease !important;
                    z-index: 9999 !important;
                }
            `;
            document.head.appendChild(style);
        }

        el.classList.add('neon-highlight');
        
        setTimeout(() => {
            el.classList.remove('neon-highlight');
        }, 3000);
    }
}
