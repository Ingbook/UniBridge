function updateBoardHeaderState() {
    document.body.classList.toggle('is-board-scrolled', window.scrollY > 16);
}

document.addEventListener('DOMContentLoaded', updateBoardHeaderState);
window.addEventListener('scroll', updateBoardHeaderState, { passive: true });
