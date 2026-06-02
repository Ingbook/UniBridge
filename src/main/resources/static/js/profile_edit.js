document.getElementById('profileForm').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    const certificationIds = Array.from(form.querySelectorAll('input[name="certificationIds"]:checked'))
                                  .map(cb => parseInt(cb.value, 10));

    const data = {
        gpa: parseFloat(formData.get('gpa')) || null,
        maxGpa: parseFloat(formData.get('maxGpa')) || null,
        languageScore: parseInt(formData.get('languageScore'), 10) || null,
        awardCount: parseInt(formData.get('awardCount'), 10) || null,
        projectSummary: formData.get('projectSummary'),
        portfolioDescription: formData.get('portfolioDescription'),
        certificationIds: certificationIds
    };

    fetch('/api/specifications/me', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        throw new Error('Server responded with an error.');
    })
    .then(data => {
        if (data.success) {
            alert('프로필이 성공적으로 저장되었습니다.');
            if (window.opener) {
                window.opener.postMessage('profileUpdated', '*');
            }
            window.close();
        } else {
            alert('저장 실패: ' + data.message);
        }
    })
    .catch(error => {
         alert('서버 오류가 발생했습니다: ' + error.message);
    });
});
