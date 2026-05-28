function updateHomeScrollHint() {
    document.body.classList.toggle('is-scrolled', window.scrollY > 8);
}

function initCompanyPagination() {
    const rows = Array.from(document.querySelectorAll('.company-list .company-row'));
    const footer = document.querySelector('.company-list-footer');
    const pageNumbers = document.getElementById('companyPageNumbers');
    const prevButton = document.getElementById('companyPagePrev');
    const nextButton = document.getElementById('companyPageNext');

    if (!footer || !pageNumbers || !prevButton || !nextButton || rows.length === 0) {
        return;
    }

    const pageSize = Number.parseInt(footer.dataset.pageSize, 10) || 10;
    const totalPages = Math.ceil(rows.length / pageSize);
    let currentPage = 1;

    function renderPageNumbers() {
        pageNumbers.innerHTML = '';

        for (let page = 1; page <= totalPages; page++) {
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = page;
            button.setAttribute('aria-label', `Company page ${page}`);
            button.classList.toggle('is-active', page === currentPage);
            button.addEventListener('click', () => {
                currentPage = page;
                updatePage();
            });
            pageNumbers.appendChild(button);
        }
    }

    function updatePage() {
        const start = (currentPage - 1) * pageSize;
        const end = start + pageSize;

        rows.forEach((row, index) => {
            row.classList.toggle('is-company-page-hidden', index < start || index >= end);
        });

        prevButton.disabled = currentPage === 1;
        nextButton.disabled = currentPage === totalPages;
        renderPageNumbers();
    }

    prevButton.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            updatePage();
        }
    });

    nextButton.addEventListener('click', () => {
        if (currentPage < totalPages) {
            currentPage++;
            updatePage();
        }
    });

    footer.hidden = totalPages <= 1;
    updatePage();
}

document.addEventListener('DOMContentLoaded', () => {
    updateHomeScrollHint();
    initCompanyPagination();
});
window.addEventListener('scroll', updateHomeScrollHint, { passive: true });
