document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('company-search');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const filter = this.value.toLowerCase();
            const select = document.getElementById('companyId');
            const options = select.options;
            for (let i = 1; i < options.length; i++) { // Skip the first placeholder option
                const option = options[i];
                const text = option.text.toLowerCase();
                if (text.startsWith(filter) || option.value === "0") { // Always show Random
                    option.style.display = '';
                } else {
                    option.style.display = 'none';
                }
            }
        });
    }
});