document.addEventListener('DOMContentLoaded', () => {
    // Fetch latest versions from the version API
    async function fetchVersions() {
        try {
            const response = await fetch('https://klaat-version.kartikarora.me/');
            if (!response.ok) return;
            const data = await response.json();

            const releaseVersion = data.release || 'latest';
            const snapshotVersion = data.snapshot || 'latest';

            document.querySelectorAll('.release-version').forEach(el => {
                el.textContent = releaseVersion;
            });

            document.querySelectorAll('.snapshot-version').forEach(el => {
                el.textContent = snapshotVersion;
            });

            // Update URLs after versions are loaded
            updateDownloadURLs();
        } catch (e) {
            console.error('Failed to fetch versions', e);
        }
    }

    // Platform Selection Logic
    const platformSelectSnapshot = document.getElementById('platform-select');
    const platformSelectRelease = document.getElementById('platform-select-release');

    function detectPlatform() {
        const userAgent = window.navigator.userAgent.toLowerCase();
        const platform = window.navigator.platform.toLowerCase();

        let detected = 'macos-arm64'; // Default

        if (platform.includes('mac')) {
            detected = 'macos-arm64';
        } else if (platform.includes('linux')) {
            if (userAgent.includes('arm') || userAgent.includes('aarch64')) {
                detected = 'linux-arm64';
            } else {
                detected = 'linux-x64';
            }
        }

        return detected;
    }

    function updateDownloadURLs(updateUrl = false) {
        const platform = platformSelectSnapshot ? platformSelectSnapshot.value : detectPlatform();
        const releaseEl = Array.from(document.querySelectorAll('.release-version')).find(el => !el.closest('#release-url'));
        const snapshotEl = Array.from(document.querySelectorAll('.snapshot-version')).find(el => !el.closest('#snapshot-url'));

        if (!releaseEl || !snapshotEl) return;

        const releaseVersion = releaseEl.textContent;
        const snapshotVersion = snapshotEl.textContent;

        const platformNames = {
            'macos-arm64': 'macOS ARM64 (Apple Silicon)',
            'linux-arm64': 'Linux ARM64',
            'linux-x64': 'Linux x64'
        };

        const platformName = platformNames[platform];

        // Update Snapshot UI
        const selectedPlatformName = document.getElementById('selected-platform-name');
        if (selectedPlatformName) selectedPlatformName.textContent = platformName;
        
        const snapshotBinary = `klaat-snapshot-${platform}-${snapshotVersion}`;
        const snapshotURL = `https://distribute.kartikarora.me/bin/klaat/snapshot/${snapshotVersion}/${snapshotBinary}`;
        
        const snapshotUrlEl = document.getElementById('snapshot-url');
        if (snapshotUrlEl) snapshotUrlEl.textContent = snapshotURL;
        
        const snapshotCurlEl = document.getElementById('snapshot-curl');
        if (snapshotCurlEl) snapshotCurlEl.textContent = `curl -L -O ${snapshotURL}`;
        
        document.querySelectorAll('.snapshot-binary-filename').forEach(el => el.textContent = snapshotBinary);

        // Update Release UI
        const selectedPlatformNameRelease = document.getElementById('selected-platform-name-release');
        if (selectedPlatformNameRelease) selectedPlatformNameRelease.textContent = platformName;
        
        const releaseBinary = `klaat-${platform}-${releaseVersion}`;
        const releaseURL = `https://distribute.kartikarora.me/bin/klaat/release/${releaseVersion}/${releaseBinary}`;
        
        const releaseUrlEl = document.getElementById('release-url');
        if (releaseUrlEl) releaseUrlEl.textContent = releaseURL;
        
        const releaseCurlEl = document.getElementById('release-curl');
        if (releaseCurlEl) releaseCurlEl.textContent = `curl -L -O ${releaseURL}`;
        
        document.querySelectorAll('.release-binary-filename').forEach(el => el.textContent = releaseBinary);

        if (updateUrl) {
            const url = new URL(window.location);
            url.searchParams.set('platform', platform);
            window.history.replaceState({}, '', url);
        }
    }

    // Sync selectors
    if (platformSelectSnapshot) {
        platformSelectSnapshot.addEventListener('change', () => {
            if (platformSelectRelease) platformSelectRelease.value = platformSelectSnapshot.value;
            updateDownloadURLs(true);
        });
    }

    if (platformSelectRelease) {
        platformSelectRelease.addEventListener('change', () => {
            if (platformSelectSnapshot) platformSelectSnapshot.value = platformSelectRelease.value;
            updateDownloadURLs(true);
        });
    }

    // Set initial platform
    const urlParams = new URLSearchParams(window.location.search);
    const platformParam = urlParams.get('platform');
    const detectedPlatform = platformParam && ['macos-arm64', 'linux-arm64', 'linux-x64'].includes(platformParam)
        ? platformParam
        : detectPlatform();

    if (platformSelectSnapshot) platformSelectSnapshot.value = detectedPlatform;
    if (platformSelectRelease) platformSelectRelease.value = detectedPlatform;
    updateDownloadURLs();

    // Initialize versions
    fetchVersions();

    // Tab Switching Logic
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.getAttribute('data-tab');

            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            tabContents.forEach(content => {
                if (content.id.includes(tabId)) {
                    content.classList.add('active');
                } else {
                    content.classList.remove('active');
                }
            });
        });
    });

    // Mobile Navigation
    const navToggle = document.getElementById('nav-toggle');
    const navLinks = document.getElementById('nav-links');

    if (navToggle && navLinks) {
        navToggle.addEventListener('click', () => {
            navToggle.classList.toggle('active');
            navLinks.classList.toggle('active');
            
            // Toggle icon between menu and close
            const icon = navToggle.querySelector('.material-symbols-outlined');
            if (icon) {
                icon.textContent = navToggle.classList.contains('active') ? 'close' : 'menu';
            }
        });

        // Close menu when clicking a link
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                navToggle.classList.remove('active');
                navLinks.classList.remove('active');
                const icon = navToggle.querySelector('.material-symbols-outlined');
                if (icon) icon.textContent = 'menu';
            });
        });
    }
});
