function updateHomeScrollHint() {
    document.body.classList.toggle('is-scrolled', window.scrollY > 8);
}

updateHomeScrollHint();
window.addEventListener('scroll', updateHomeScrollHint, { passive: true });
