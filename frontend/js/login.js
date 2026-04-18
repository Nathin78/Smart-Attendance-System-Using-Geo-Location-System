document.addEventListener("DOMContentLoaded", () => {
    SmartApp.applyTheme();
    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();

    const requiredRole = document.body.dataset.loginRole || "";
    const roleRedirect = requiredRole === "ADMIN" ? "admin-dashboard.html" : "user-dashboard.html";
    const roleLabel = requiredRole === "ADMIN" ? "Admin" : "User";
    const fixedAdminEmail = "admin@smartattendance.com";
    const isAdminLogin = requiredRole === "ADMIN";
    const rememberedEmailKey = "sa_remembered_user_email";
    const legacyRememberedEmailKey = "sa_remembered_email";

    const existingUser = SmartApp.getUser();
    if (existingUser && SmartApp.getToken()) {
        SmartApp.redirectByRole(existingUser);
        return;
    }

    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const rememberEmail = document.getElementById("rememberEmail");
    const togglePasswordBtn = document.getElementById("togglePasswordBtn");
    const resetLoginBtn = document.getElementById("resetLoginBtn");
    const rememberEmailField = rememberEmail?.closest("label") || null;
    const rememberedEmail = localStorage.getItem(rememberedEmailKey) || localStorage.getItem(legacyRememberedEmailKey);

    if (isAdminLogin) {
        emailInput.value = fixedAdminEmail;
        emailInput.readOnly = true;
        if (rememberEmailField) {
            rememberEmailField.hidden = true;
        }
    } else if (rememberedEmail) {
        emailInput.value = rememberedEmail;
        rememberEmail.checked = true;
        localStorage.setItem(rememberedEmailKey, rememberedEmail);
        localStorage.removeItem(legacyRememberedEmailKey);
    }

    if (!isAdminLogin && rememberEmail) {
        rememberEmail.addEventListener("change", () => {
            if (rememberEmail.checked) {
                const currentEmail = emailInput.value.trim().toLowerCase();
                if (currentEmail) {
                    localStorage.setItem(rememberedEmailKey, currentEmail);
                }
                return;
            }
            localStorage.removeItem(rememberedEmailKey);
            localStorage.removeItem(legacyRememberedEmailKey);
        });
    }

    togglePasswordBtn.addEventListener("click", () => {
        const isPassword = passwordInput.type === "password";
        passwordInput.type = isPassword ? "text" : "password";
        togglePasswordBtn.textContent = isPassword ? "Hide" : "Show";
    });

    const form = document.getElementById("loginForm");
    resetLoginBtn.addEventListener("click", () => {
        form.reset();
        emailInput.value = isAdminLogin ? fixedAdminEmail : "";
        passwordInput.value = "";
        if (rememberEmail) {
            rememberEmail.checked = false;
        }
        if (!isAdminLogin) {
            localStorage.removeItem(rememberedEmailKey);
            localStorage.removeItem(legacyRememberedEmailKey);
        }
        SmartApp.hideAlert("loginAlert");
        passwordInput.type = "password";
        togglePasswordBtn.textContent = "Show";
        emailInput.focus();
    });
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        SmartApp.hideAlert("loginAlert");

        const email = emailInput.value.trim().toLowerCase();
        const password = passwordInput.value.trim();

        if (!email || !password) {
            SmartApp.showAlert("loginAlert", "Email and password are required", "error");
            return;
        }

        const response = await SmartApp.apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        }, false);

        if (!response.ok) {
            SmartApp.showAlert("loginAlert", response.data?.message || "Login failed", "error");
            return;
        }

        if (requiredRole && response.data?.role !== requiredRole) {
            SmartApp.showAlert(
                "loginAlert",
                `This page is for ${roleLabel.toLowerCase()} accounts. Please use the ${roleLabel} Login page.`,
                "error"
            );
            return;
        }

        if (!isAdminLogin && rememberEmail.checked) {
            localStorage.setItem(rememberedEmailKey, email);
            localStorage.removeItem(legacyRememberedEmailKey);
        } else if (!isAdminLogin) {
            localStorage.removeItem(rememberedEmailKey);
            localStorage.removeItem(legacyRememberedEmailKey);
        }

        SmartApp.saveSession(response.data);
        SmartApp.showAlert("loginAlert", "Login successful. Redirecting...", "success");
        setTimeout(() => {
            if (requiredRole) {
                window.location.href = roleRedirect;
                return;
            }
            SmartApp.redirectByRole(response.data);
        }, 500);
    });
});
