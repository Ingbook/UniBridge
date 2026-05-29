const app = document.getElementById('analysisApp');
const alumniList = document.getElementById('alumniList');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const totalScore = document.getElementById('totalScore');
const scoreMeter = document.getElementById('scoreMeter');
const scoreDescription = document.getElementById('scoreDescription');
const analysisSummary = document.getElementById('analysisSummary');
const userProfile = document.getElementById('userProfile');
const alumnusProfile = document.getElementById('alumnusProfile');
const comparisonItems = document.getElementById('comparisonItems');
const detailToggle = document.getElementById('detailToggle');
const detailAnalysis = document.getElementById('detailAnalysis');
const strengthList = document.getElementById('strengthList');
const weaknessList = document.getElementById('weaknessList');
const commentList = document.getElementById('commentList');

const companyId = Number(app?.dataset.companyId);
const targetJobRole = app?.dataset.targetJobRole || '';
let alumni = [];
let selectedAlumnusId = null;
let currentIndex = 0;

const categoryLabels = {
    GPA: '학점',
    Language: '어학성적',
    Certifications: '자격증',
    Awards: '수상경력',
    ProjectPortfolio: '프로젝트/포트폴리오'
};

function updateCarousel() {
    const totalAlumni = alumni.length;
    alumniList.style.transform = `translateX(-${currentIndex * 33.33}%)`;
    prevBtn.disabled = currentIndex === 0;
    nextBtn.disabled = totalAlumni <= 3 || currentIndex >= totalAlumni - 3;
}

function profileRows(profile) {
    return [
        ['이름', profile.name],
        ['학점', `${profile.gpa ?? '-'} / ${profile.maxGpa ?? '-'}`],
        ['어학성적', `${profile.languageType ?? '-'} ${profile.languageScore ?? '-'}`],
        ['자격증', `${profile.certificationCount ?? 0}개`],
        ['수상경력', `${profile.awardCount ?? 0}개`],
        ['프로젝트', profile.projectSummary || '-'],
        ['포트폴리오', profile.portfolioLevel || '-']
    ];
}

function renderProfile(container, profile) {
    container.innerHTML = profileRows(profile)
        .map(([label, value]) => `
            <div class="analysis-spec-row">
                <span>${label}</span>
                <strong>${value}</strong>
            </div>
        `)
        .join('');
}

function renderList(container, values) {
    container.innerHTML = (values || [])
        .map(value => `<li>${value}</li>`)
        .join('');
}

function renderAlumni() {
    alumniList.innerHTML = alumni.map(alumnus => `
        <button class="analysis-alumni-box ${alumnus.id === selectedAlumnusId ? 'is-active' : ''}"
                type="button"
                data-alumnus-id="${alumnus.id}">
            <div class="analysis-alumni-icon">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                </svg>
            </div>
            <span class="analysis-alumni-name">${alumnus.name}</span>
            <span class="analysis-alumni-score">${alumnus.representativeScore ?? '-'}점</span>
            <small>${alumnus.jobRole ?? ''}</small>
        </button>
    `).join('');

    document.querySelectorAll('.analysis-alumni-box').forEach(box => {
        box.addEventListener('click', () => {
            selectedAlumnusId = Number(box.dataset.alumnusId);
            detailAnalysis.classList.add('is-collapsed');
            detailToggle.textContent = '상세 분석 보기';
            renderAlumni();
            loadGapAnalysis();
        });
    });
    updateCarousel();
}

async function loadAlumni() {
    const response = await fetch(`/api/companies/${companyId}/alumni`);
    const body = await response.json();
    alumni = body.data || [];
    selectedAlumnusId = alumni[0]?.id ?? null;
    renderAlumni();
    if (selectedAlumnusId) {
        await loadGapAnalysis();
    }
}

async function loadGapAnalysis() {
    setLoading();
    const response = await fetch('/api/analysis/gap', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({companyId, alumnusId: selectedAlumnusId, targetJobRole})
    });
    const body = await response.json();
    if (!body.success) {
        throw new Error(body.message || '분석에 실패했습니다.');
    }
    renderAnalysis(body.data);
}

function setLoading() {
    totalScore.textContent = '-';
    scoreMeter.style.width = '0%';
    scoreDescription.textContent = 'AI가 동문 스펙과 비교하고 있습니다.';
    analysisSummary.textContent = '잠시만 기다려 주세요.';
}

function renderAnalysis(data) {
    totalScore.textContent = data.totalScore;
    scoreMeter.style.width = `${data.totalScore}%`;
    scoreDescription.textContent = data.scoreDescription;
    analysisSummary.textContent = data.summary;
    renderProfile(userProfile, data.userProfile);
    renderProfile(alumnusProfile, data.alumnusProfile);
    comparisonItems.innerHTML = data.comparisonItems.map(item => `
        <div class="analysis-comparison-item">
            <div>
                <span>${categoryLabels[item.category] || item.category}</span>
                <strong>${item.aiScore}점</strong>
            </div>
            <p>${item.userValue} → ${item.alumnusValue}</p>
            <small>${item.gapDescription}</small>
            <em>${item.status}</em>
        </div>
    `).join('');
    renderList(strengthList, data.detailAnalysis.strengths);
    renderList(weaknessList, data.detailAnalysis.weaknesses);
    renderList(commentList, data.detailAnalysis.comments);
}

prevBtn.addEventListener('click', () => {
    if (currentIndex > 0) {
        currentIndex--;
        updateCarousel();
    }
});

nextBtn.addEventListener('click', () => {
    if (currentIndex < alumni.length - 3) {
        currentIndex++;
        updateCarousel();
    }
});

detailToggle.addEventListener('click', () => {
    const collapsed = detailAnalysis.classList.toggle('is-collapsed');
    detailToggle.textContent = collapsed ? '상세 분석 보기' : '상세 분석 접기';
});

loadAlumni().catch(error => {
    scoreDescription.textContent = '분석 데이터를 불러오지 못했습니다.';
    analysisSummary.textContent = error.message;
});
