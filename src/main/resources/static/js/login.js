function switchTab(tabId, trigger) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    trigger.classList.add('active');
    document.getElementById(`tab-${tabId}`).classList.add('active');
}

function toggleHelp(event) {
    if (event) {
        event.preventDefault();
    }

    document.getElementById('helpBox').classList.toggle('is-open');
}