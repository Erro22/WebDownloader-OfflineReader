(function() {
    const links = Array.from(document.querySelectorAll('a')).map(a => {
        const rect = a.getBoundingClientRect();
        return {
            title: a.innerText.trim() || a.getAttribute('title') || 'Без названия',
            url: a.href,
            x: Math.round(rect.left + window.scrollX),
            y: Math.round(rect.top + window.scrollY),
            width: Math.round(rect.width),
            height: Math.round(rect.height)
        };
    }).filter(link => {
        // Filter out junk
        if (!link.url || link.url.startsWith('javascript:') || link.url.startsWith('#')) return false;
        if (link.title.length < 2 && !link.url.includes(window.location.hostname)) return false;
        return true;
    });

    // Send back to Android
    if (window.AndroidInterface && window.AndroidInterface.onLinksExtracted) {
        window.AndroidInterface.onLinksExtracted(JSON.stringify(links));
    }
    return JSON.stringify(links);
})();

function highlightElement(x, y) {
    const el = document.elementFromPoint(x - window.scrollX, y - window.scrollY);
    if (el) {
        el.scrollIntoView({behavior: "smooth", block: "center"});
        
        // Add highlight effect
        const originalOutline = el.style.outline;
        const originalTransition = el.style.transition;
        
        el.style.transition = 'outline 0.3s ease';
        el.style.outline = '5px solid #6750A4';
        
        setTimeout(() => {
            el.style.outline = originalOutline;
            setTimeout(() => {
                el.style.transition = originalTransition;
            }, 300);
        }, 2000);
    }
}
