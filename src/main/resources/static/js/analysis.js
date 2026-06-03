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
const editProfileBtn = document.getElementById('editProfileBtn');

const companyId = Number(app?.dataset.companyId);
const targetJobRole = app?.dataset.targetJobRole || '';
let alumni = [];
let selectedAlumnusId = null;
let currentIndex = 0;
let myProfile = {};

const loadingMessages = [
    "AI 분석 기능은 현재 테스트 개발 단계로, 다소 시간이 소요될 수 있습니다.",
    "프로필 포트폴리오를 꼼꼼히 살펴보는 중...",
    "AI가 진지하게 고민중입니다...",
    "AI도 가끔 생각할 시간이 필요합니다.",
    "커피 한 잔 마시고 오면 끝나있을지도 모릅니다.",
    "리빙 포인트) AI도 화장실을 간답니다.",
    "AI도 재촉하면 기분 나빠합니다."
];
let messageInterval;

function toggleLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    const textElement = document.getElementById('loadingText');
    if (!overlay || !textElement) return;

    if (show) {
        overlay.classList.add('active');
        let i = 0;
        textElement.textContent = loadingMessages[0];
        messageInterval = setInterval(() => {
            i = (i + 1) % loadingMessages.length;
            textElement.textContent = loadingMessages[i];
        }, 2000);
    } else {
        overlay.classList.remove('active');
        clearInterval(messageInterval);
    }
}

const categoryLabels = {
    GPA: '학점',
    LANGUAGE: '어학성적',
    CERTIFICATION: '자격증',
    AWARD: '수상경력',
    PROJECT: '프로젝트',
    PORTFOLIO: '포트폴리오',
    PROJECT_PORTFOLIO: '프로젝트/포트폴리오'
};

function openProfileEditPopup() {
    const width = 800;
    const height = 600;
    const left = (window.innerWidth - width) / 2;
    const top = (window.innerHeight - height) / 2;
    window.open('/profile/edit', 'ProfileEdit', `width=${width},height=${height},top=${top},left=${left}`);
}

function updateCarousel() {
    const totalAlumni = alumni.length;
    alumniList.style.transform = `translateX(-${currentIndex * 33.33}%)`;
    prevBtn.disabled = currentIndex === 0;
    nextBtn.disabled = totalAlumni <= 3 || currentIndex >= totalAlumni - 3;
}

function truncateText(text, maxLength) {
    if (!text) return '-';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

function profileRows(profile) {
    if (!profile) return [];
    
    // Handle both old and new structures
    const languageType = profile.language?.type || profile.languageType;
    const languageScore = profile.language?.score || profile.languageScore;
    
    // certifications might be an object {items: [], count: 0} or flat fields
    let certNames = [];
    let certCount = 0;
    
    if (profile.certifications) {
        certNames = profile.certifications.items || [];
        certCount = profile.certifications.count || certNames.length;
    } else {
        certNames = profile.certificationNames || [];
        certCount = profile.certificationCount ?? certNames.length;
    }

    return [
        ['이름', profile.name || '-'],
        ['학점', `${profile.gpa ?? '-'} / ${profile.maxGpa ?? '-'}`],
        ['어학성적', `${languageType ?? '-'} ${languageScore ?? '-'}`],
        ['자격증', formatCertificationNames(certNames, certCount) || `총 ${certCount}개`],
        ['수상경력', `${profile.awardCount ?? 0}개`],
        ['프로젝트', truncateText(profile.projectSummary || profile.project, 20)],
        ['포트폴리오', truncateText(profile.portfolioDescription || profile.portfolio, 20) || profile.portfolioLevel || '-']
    ];
}

function formatCertificationNames(names, count) {
    if (!names || names.length === 0) return '';
    const limit = 3;
    const visible = names.slice(0, limit).join(', ');
    const remaining = Math.max(count - limit, 0);
    return remaining > 0 ? `${visible} 외 ${remaining}개` : visible;
}

function renderProfile(container, profile) {
    if (!profile) {
        container.innerHTML = '<div>프로필 데이터를 불러올 수 없습니다.</div>';
        return;
    }
    
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
            const svg = detailToggle.querySelector('svg');
            const textSpan = detailToggle.querySelector('span');
            textSpan.textContent = '상세 분석 보기';
            svg.innerHTML = '<path d="M6 9l6 6 6-6"></path>';
            renderAlumni();
            loadGapAnalysis();
        });
    });
    updateCarousel();
}

async function loadMyProfile() {
    try {
        const response = await fetch('/api/specifications/me');
        if (!response.ok) {
            return; // Do nothing, let the initial message show
        }
        const data = await response.json();
        if (data.success && data.data) {
            myProfile = data.data;
            const formattedProfile = {
                name: "나",
                gpa: myProfile.gpa,
                maxGpa: myProfile.maxGpa,
                languageType: myProfile.languageType,
                languageScore: myProfile.languageScore,
                awardCount: myProfile.awardCount,
                projectSummary: myProfile.projectSummary,
                portfolioDescription: myProfile.portfolioDescription,
                certificationNames: myProfile.certificationNames,
                certificationCount: myProfile.certificationCount
            };
            renderProfile(userProfile, formattedProfile);
        }
    } catch (error) {
        console.error("Failed to load profile:", error);
    }
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
    toggleLoading(true);
    try {
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
    } catch (error) {
         scoreDescription.textContent = '분석에 실패했습니다.';
         analysisSummary.textContent = error.message;
    } finally {
        toggleLoading(false);
    }
}

function setLoading() {
    totalScore.textContent = '-';
    scoreMeter.style.width = '0%';
    scoreDescription.textContent = 'AI가 동문 스펙과 비교하고 있습니다.';
    analysisSummary.textContent = '잠시만 기다려 주세요.';
}

function renderAnalysis(data) {
    const score = data.overallScore ?? data.totalScore ?? 0;
    const summary = data.summarized ?? data.summary ?? '';
    
    // FIX: Look for selectedAlumnus first (the correct name), fallback to the shitty names if needed
    const selectedProfile = data.selectedAlumnus ?? data.selectedAlumnusProfile ?? data.alumnusProfile;
    const myAnalyzedProfile = data.currentUser ?? data.userProfile;
    
    const items = data.gapItems ?? data.comparisonItems ?? [];
    
    totalScore.textContent = score;
    scoreMeter.style.width = `${score}%`;
    scoreDescription.textContent = data.scoreDescription;
    analysisSummary.textContent = summary;
    
    if(myAnalyzedProfile) {
        renderProfile(userProfile, myAnalyzedProfile);
    }

    renderProfile(alumnusProfile, selectedProfile);

    comparisonItems.innerHTML = items.map(item => `
        <div class="analysis-comparison-item">
            <div>
                <span>${item.displayName || categoryLabels[item.category] || item.category || item.label}</span>
                <strong>${item.score ?? item.aiScore}점</strong>
            </div>
            <p>${truncateText(item.userValue || item.currentValue, 20)} → ${truncateText(item.alumnusValue, 20)}</p>
            <small>${item.comment ?? item.gapDescription ?? item.message}</small>
            <em>${item.status}</em>
        </div>
    `).join('');
    
    // FIX: Detail Analysis vs Overall Comment
    const strengths = data.overallComment?.strengths || data.detailAnalysis?.strengths;
    const weaknesses = data.overallComment?.weaknesses || data.detailAnalysis?.weaknesses;
    const comments = data.overallComment?.aiComment ? [data.overallComment.aiComment] : data.detailAnalysis?.comments;
    
    renderList(strengthList, strengths);
    renderList(weaknessList, weaknesses);
    renderList(commentList, comments);
}

if (editProfileBtn) {
    editProfileBtn.addEventListener('click', openProfileEditPopup);
}

if (prevBtn) {
    prevBtn.addEventListener('click', () => {
        if (currentIndex > 0) {
            currentIndex--;
            updateCarousel();
        }
    });
}

if (nextBtn) {
    nextBtn.addEventListener('click', () => {
        if (currentIndex < alumni.length - 3) {
            currentIndex++;
            updateCarousel();
        }
    });
}

if (detailToggle) {
    detailToggle.addEventListener('click', () => {
        const collapsed = detailAnalysis.classList.toggle('is-collapsed');
        const svg = detailToggle.querySelector('svg');
        const textSpan = detailToggle.querySelector('span');

        if (collapsed) {
            textSpan.textContent = '상세 분석 보기';
            svg.innerHTML = '<path d="M6 9l6 6 6-6"></path>';
        } else {
            textSpan.textContent = '상세 분석 접기';
            svg.innerHTML = '<path d="M18 15l-6-6-6 6"></path>';
        }
    });
}

// Only run these if we are on the analysis page
if (app) {
    loadMyProfile().then(() => {
        loadAlumni().catch(error => {
            scoreDescription.textContent = '분석 데이터를 불러오지 못했습니다.';
            analysisSummary.textContent = error.message;
        });
    });
}

window.addEventListener('message', (event) => {
    if (event.data === 'profileUpdated') {
        window.location.reload();
    }
});
