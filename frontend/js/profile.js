document.addEventListener("DOMContentLoaded", async () => {
    const user = SmartApp.requireAuth("USER");
    if (!user) return;

    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();

    const form = document.getElementById("profileForm");
    const resetButton = document.getElementById("resetProfileBtn");
    let originalProfile = null;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        SmartApp.hideAlert("profileAlert");

        const name = document.getElementById("profileNameInput").value.trim();
        const email = document.getElementById("profileEmailInput").value.trim().toLowerCase();
        const currentPassword = document.getElementById("currentPasswordInput").value.trim();
        const newPassword = document.getElementById("newPasswordInput").value.trim();
        const confirmPassword = document.getElementById("confirmPasswordInput").value.trim();

        if (!name || !email) {
            SmartApp.showAlert("profileAlert", "Name and email are required", "error");
            return;
        }

        if (newPassword || confirmPassword) {
            if (newPassword.length < 6) {
                SmartApp.showAlert("profileAlert", "New password must be at least 6 characters", "error");
                return;
            }
            if (newPassword !== confirmPassword) {
                SmartApp.showAlert("profileAlert", "New password and confirm password must match", "error");
                return;
            }
            if (!currentPassword) {
                SmartApp.showAlert("profileAlert", "Current password is required to change password", "error");
                return;
            }
        }

        const response = await SmartApp.apiRequest("/api/profile/me", {
            method: "PUT",
            body: JSON.stringify({
                name,
                email,
                currentPassword: currentPassword || null,
                newPassword: newPassword || null
            })
        });

        if (!response.ok) {
            SmartApp.showAlert("profileAlert", response.data?.message || "Unable to update profile", "error");
            return;
        }

        SmartApp.saveSession(response.data);
        originalProfile = {
            name: response.data.name,
            email: response.data.email,
            role: response.data.role
        };
        populateProfile(originalProfile);
        clearPasswordFields();
        SmartApp.showAlert("profileAlert", "Profile updated successfully", "success");
    });

    resetButton.addEventListener("click", () => {
        if (originalProfile) {
            populateProfile(originalProfile);
        }
        clearPasswordFields();
        SmartApp.hideAlert("profileAlert");
    });

    const response = await SmartApp.apiRequest("/api/profile/me");
    if (!response.ok) {
        SmartApp.showAlert("profileAlert", response.data?.message || "Unable to load profile", "error");
        return;
    }

    originalProfile = response.data;
    populateProfile(originalProfile);

    function populateProfile(profile) {
        document.getElementById("profileNameInput").value = profile.name || "";
        document.getElementById("profileEmailInput").value = profile.email || "";
        document.getElementById("profileRoleInput").value = profile.role || "USER";
        document.getElementById("profileSummaryName").textContent = profile.name || "-";
        document.getElementById("profileSummaryEmail").textContent = profile.email || "-";
        document.getElementById("profileSummaryRole").textContent = profile.role || "-";
    }

    function clearPasswordFields() {
        document.getElementById("currentPasswordInput").value = "";
        document.getElementById("newPasswordInput").value = "";
        document.getElementById("confirmPasswordInput").value = "";
    }
});
