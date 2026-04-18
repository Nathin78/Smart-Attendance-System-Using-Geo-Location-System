document.addEventListener("DOMContentLoaded", () => {
    SmartApp.applyTheme();
    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();

    const existingUser = SmartApp.getUser();
    if (existingUser && SmartApp.getToken()) {
        SmartApp.redirectByRole(existingUser);
        return;
    }

    const form = document.getElementById("registerForm");
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        SmartApp.hideAlert("registerAlert");

        const name = document.getElementById("name").value.trim();
        const email = document.getElementById("email").value.trim().toLowerCase();
        const password = document.getElementById("password").value.trim();
        const confirmPassword = document.getElementById("confirmPassword").value.trim();

        if (!name || !email || !password || !confirmPassword) {
            SmartApp.showAlert("registerAlert", "All fields are required", "error");
            return;
        }
        if (password.length < 6) {
            SmartApp.showAlert("registerAlert", "Password must be at least 6 characters", "error");
            return;
        }
        if (password !== confirmPassword) {
            SmartApp.showAlert("registerAlert", "Passwords do not match", "error");
            return;
        }

        const response = await SmartApp.apiRequest("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({ name, email, password })
        }, false);

        if (!response.ok) {
            SmartApp.showAlert("registerAlert", response.data?.message || "Registration failed", "error");
            return;
        }

        SmartApp.saveSession(response.data);
        SmartApp.showAlert("registerAlert", "Registration successful. Redirecting...", "success");
        setTimeout(() => window.location.href = "user-dashboard.html", 600);
    });
});
