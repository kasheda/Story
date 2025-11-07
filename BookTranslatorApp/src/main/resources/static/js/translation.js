document.addEventListener('DOMContentLoaded', () => {
    const audio = document.querySelector('audio');
    if (!audio) {
        return;
    }
    const status = document.createElement('p');
    status.classList.add('helper');
    audio.parentElement.appendChild(status);

    audio.addEventListener('play', () => {
        status.textContent = 'Playing narration…';
    });

    audio.addEventListener('pause', () => {
        status.textContent = 'Playback paused.';
    });

    audio.addEventListener('ended', () => {
        status.textContent = 'Playback finished.';
    });
});
