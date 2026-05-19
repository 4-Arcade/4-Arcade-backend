window.addEventListener('load', () => {
    setTimeout(() => {
        const topbar = document.querySelector('.topbar-wrapper');
        if (!topbar) return;

        const link = document.createElement('a');
        link.href = '/ws-docs.html';
        link.textContent = 'WebSocket Docs';
        link.target = '_blank';
        link.style.cssText = 'color:white;margin-left:20px;font-size:14px;text-decoration:none;align-self:center;';
        link.onmouseover = () => link.style.opacity = '1';
        link.onmouseout = () => link.style.opacity = '.9';
        topbar.appendChild(link);
    }, 500);    // Swagger UI 랜더링 대기
});