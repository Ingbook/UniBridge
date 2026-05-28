const alumniList = document.getElementById('alumniList');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const alumniBoxes = document.querySelectorAll('.analysis-alumni-box');
const totalAlumni = alumniBoxes.length;
let currentIndex = 0;

function updateCarousel() {
    alumniList.style.transform = `translateX(-${currentIndex * 33.33}%)`;

    // Update button states
    prevBtn.disabled = currentIndex === 0;
    nextBtn.disabled = currentIndex >= totalAlumni - 3;

    if (totalAlumni <= 3) {
        nextBtn.disabled = true;
    }
}

prevBtn.addEventListener('click', () => {
    if (currentIndex > 0) {
        currentIndex--;
        updateCarousel();
    }
});

nextBtn.addEventListener('click', () => {
    if (currentIndex < totalAlumni - 3) {
        currentIndex++;
        updateCarousel();
    }
});

updateCarousel();
