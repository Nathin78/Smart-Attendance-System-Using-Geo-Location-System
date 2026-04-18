const SmartApp = (() => {
    const TOKEN_KEY = "sa_token";
    const USER_KEY = "sa_user";
    const THEME_KEY = "sa_theme";
    const THEMES = [
        { id: "teal", label: "Teal Ocean" },
        { id: "sunset", label: "Sunset Orange" },
        { id: "forest", label: "Forest Green" }
    ];
    const DEFAULT_API_BASE = "http://localhost:8081";
    const API_BASE_CANDIDATES = (
        window.SMART_ATTENDANCE_CONFIG?.apiBaseCandidates
        || [window.SMART_ATTENDANCE_CONFIG?.apiBase]
        || [DEFAULT_API_BASE]
    ).filter(Boolean);

    function saveSession(authResponse) {
        localStorage.setItem(TOKEN_KEY, authResponse.token);
        localStorage.setItem(USER_KEY, JSON.stringify({
            name: authResponse.name,
            email: authResponse.email,
            role: authResponse.role
        }));
    }

    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    function applyTheme(theme) {
        const selectedTheme = theme || localStorage.getItem(THEME_KEY) || "teal";
        document.body.setAttribute("data-theme", selectedTheme);
        localStorage.setItem(THEME_KEY, selectedTheme);
        return selectedTheme;
    }

    function getThemeLabel(themeId) {
        return THEMES.find(theme => theme.id === themeId)?.label || "Teal Ocean";
    }

    function initThemeControl(buttonId = "themeToggleBtn") {
        const button = document.getElementById(buttonId);
        const current = applyTheme();
        if (!button) {
            return;
        }

        button.textContent = `Theme: ${getThemeLabel(current)}`;
        button.addEventListener("click", () => {
            const activeTheme = localStorage.getItem(THEME_KEY) || "teal";
            const activeIndex = THEMES.findIndex(theme => theme.id === activeTheme);
            const nextTheme = THEMES[(activeIndex + 1) % THEMES.length].id;
            applyTheme(nextTheme);
            button.textContent = `Theme: ${getThemeLabel(nextTheme)}`;
        });
    }

    function initProfileMenu(user, options = {}) {
        const trigger = document.getElementById(options.triggerId || "profileBtn");
        const dropdown = document.getElementById(options.dropdownId || "profileDropdown");
        const nameNode = document.getElementById(options.nameId || "profileName");
        const emailNode = document.getElementById(options.emailId || "profileEmail");
        const roleNode = document.getElementById(options.roleId || "profileRole");
        const shortNode = document.getElementById(options.shortNameId || "profileNameShort");
        const avatarNode = document.getElementById(options.avatarId || "profileAvatar");
        const editProfileNode = document.getElementById(options.editProfileId || "profileEditBtn");
        const logoutNode = document.getElementById(options.logoutId || "profileLogoutBtn");

        if (nameNode) nameNode.textContent = user.name || "-";
        if (emailNode) emailNode.textContent = user.email || "-";
        if (roleNode) roleNode.textContent = user.role || "-";
        if (shortNode) shortNode.textContent = user.name || "User";
        if (avatarNode) avatarNode.textContent = (user.name || "U").trim().charAt(0).toUpperCase();

        if (editProfileNode) {
            if (user.role === "USER") {
                editProfileNode.hidden = false;
                editProfileNode.addEventListener("click", () => {
                    window.location.href = options.profileHref || "profile.html";
                });
            } else {
                editProfileNode.hidden = true;
            }
        }

        if (logoutNode) {
            logoutNode.addEventListener("click", logout);
        }

        if (!trigger || !dropdown) return;

        trigger.addEventListener("click", () => {
            dropdown.classList.toggle("open");
        });

        document.addEventListener("click", (event) => {
            if (!dropdown.classList.contains("open")) return;
            if (trigger.contains(event.target) || dropdown.contains(event.target)) return;
            dropdown.classList.remove("open");
        });
    }

    function initHeaderClock(elementId = "headerDateTime") {
        const node = document.getElementById(elementId);
        if (!node) return;

        const formatDateTime = () => {
            const now = new Date();
            return now.toLocaleString("en-IN", {
                weekday: "short",
                day: "2-digit",
                month: "short",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
                hour12: true
            });
        };

        node.textContent = formatDateTime();
        setInterval(() => {
            node.textContent = formatDateTime();
        }, 1000);
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function getUser() {
        const raw = localStorage.getItem(USER_KEY);
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (err) {
            clearSession();
            return null;
        }
    }

    function redirectByRole(user) {
        if (!user) {
            window.location.href = "login.html";
            return;
        }
        if (user.role === "ADMIN") {
            window.location.href = "admin-dashboard.html";
            return;
        }
        window.location.href = "user-dashboard.html";
    }

    function logout() {
        clearSession();
        window.location.href = "login.html";
    }

    function requireAuth(requiredRole) {
        const token = getToken();
        const user = getUser();
        if (!token || !user) {
            window.location.href = "login.html";
            return null;
        }
        if (requiredRole && user.role !== requiredRole) {
            redirectByRole(user);
            return null;
        }
        return user;
    }

    function showAlert(targetId, message, type = "info") {
        const node = document.getElementById(targetId);
        if (!node) return;
        node.textContent = message;
        node.className = `alert ${type} show`;
    }

    function hideAlert(targetId) {
        const node = document.getElementById(targetId);
        if (!node) return;
        node.className = "alert";
        node.textContent = "";
    }

    function shouldRetryCandidate(response, contentType) {
        return (response.status === 404 || response.status === 405) && !contentType.includes("application/json");
    }

    async function apiRequest(path, options = {}, withAuth = true) {
        const headers = { ...(options.headers || {}) };
        if (options.body && !headers["Content-Type"] && !headers["content-type"]) {
            headers["Content-Type"] = "application/json";
        }
        if (withAuth && getToken()) {
            headers.Authorization = `Bearer ${getToken()}`;
        }

        const normalizedPath = path.startsWith("/") ? path : `/${path}`;
        let finalResult = null;
        let lastNetworkError = null;

        for (const apiBase of API_BASE_CANDIDATES) {
            let response;
            try {
                response = await fetch(`${apiBase}${normalizedPath}`, { ...options, headers });
            } catch (err) {
                lastNetworkError = err;
                continue;
            }

            let data = null;
            const contentType = response.headers.get("content-type") || "";
            if (contentType.includes("application/json")) {
                try {
                    data = await response.json();
                } catch (err) {
                    data = null;
                }
            } else {
                try {
                    const text = await response.text();
                    if (text?.trim()) {
                        data = { message: text.trim() };
                    }
                } catch (err) {
                    data = null;
                }
            }

            finalResult = {
                ok: response.ok,
                status: response.status,
                data
            };

            // Retry other candidates when a static frontend server responds instead of the API.
            if (shouldRetryCandidate(response, contentType)) {
                continue;
            }

            break;
        }

        if (!finalResult) {
            return {
                ok: false,
                status: 0,
                data: {
                    message: "Unable to connect to server. Please verify backend service is running."
                },
                error: lastNetworkError ? String(lastNetworkError) : null
            };
        }

        if (finalResult.status === 401) {
            clearSession();
            if (!normalizedPath.startsWith("/api/auth/")) {
                window.location.href = "login.html";
            }
        }

        if (!finalResult.ok && !finalResult.data) {
            finalResult.data = { message: `Request failed with status ${finalResult.status}` };
        }

        return finalResult;
    }

    async function downloadRequest(path, options = {}, withAuth = true) {
        const headers = { ...(options.headers || {}) };
        if (withAuth && getToken()) {
            headers.Authorization = `Bearer ${getToken()}`;
        }

        const normalizedPath = path.startsWith("/") ? path : `/${path}`;
        let finalResult = null;
        let lastNetworkError = null;

        for (const apiBase of API_BASE_CANDIDATES) {
            let response;
            try {
                response = await fetch(`${apiBase}${normalizedPath}`, { ...options, headers });
            } catch (err) {
                lastNetworkError = err;
                continue;
            }

            const contentType = response.headers.get("content-type") || "";
            if (shouldRetryCandidate(response, contentType)) {
                continue;
            }

            finalResult = response;
            break;
        }

        if (finalResult) {
            return finalResult;
        }

        throw new Error(lastNetworkError ? String(lastNetworkError) : "Unable to connect to server");
    }

    function formatTime(rawTime) {
        if (!rawTime) return "-";
        return rawTime.slice(0, 8);
    }

    return {
        applyTheme,
        apiRequest,
        clearSession,
        downloadRequest,
        formatTime,
        getToken,
        getUser,
        hideAlert,
        initHeaderClock,
        initProfileMenu,
        initThemeControl,
        logout,
        redirectByRole,
        requireAuth,
        saveSession,
        showAlert
    };
})();
