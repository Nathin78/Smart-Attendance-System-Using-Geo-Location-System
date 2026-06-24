document.addEventListener("DOMContentLoaded", async () => {
    const user = SmartApp.requireAuth("USER");
    if (!user) return;

    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();
    SmartApp.initProfileMenu(user);

    const form = document.getElementById("profileForm");
    const resetButton = document.getElementById("resetProfileBtn");
    const avatarInput = document.getElementById("avatarInput");
    const avatarPreview = document.getElementById("profilePhotoPreview");
    const dirtyStateNode = document.getElementById("profileDirtyState");
    const lastSavedNode = document.getElementById("profileLastSaved");
    let originalProfile = null;

    wirePasswordToggle("toggleCurrentPasswordBtn", "currentPasswordInput");
    wirePasswordToggle("toggleNewPasswordBtn", "newPasswordInput");
    wirePasswordToggle("toggleConfirmPasswordBtn", "confirmPasswordInput");

    avatarInput?.addEventListener("change", () => {
        const file = avatarInput.files?.[0];
        if (!file || !avatarPreview) {
            refreshDirtyState();
            return;
        }
        avatarPreview.src = URL.createObjectURL(file);
        refreshDirtyState();
    });

    form.querySelectorAll("input, textarea").forEach(element => {
        element.addEventListener("input", refreshDirtyState);
        element.addEventListener("change", refreshDirtyState);
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        SmartApp.hideAlert("profileAlert");

        const name = document.getElementById("profileNameInput").value.trim();
        const email = document.getElementById("profileEmailInput").value.trim().toLowerCase();
        const currentPassword = document.getElementById("currentPasswordInput").value.trim();
        const newPassword = document.getElementById("newPasswordInput").value.trim();
        const confirmPassword = document.getElementById("confirmPasswordInput").value.trim();
        const avatarFile = avatarInput?.files?.[0] || null;

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

        let latestSession = null;
        if (avatarFile) {
            const uploadResponse = await uploadAvatar(avatarFile);
            if (!uploadResponse.ok) {
                SmartApp.showAlert("profileAlert", uploadResponse.data?.message || "Unable to upload profile photo", "error");
                return;
            }
            latestSession = uploadResponse.data;
            SmartApp.saveSession(uploadResponse.data);
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

        latestSession = response.data;
        SmartApp.saveSession(response.data);
        originalProfile = {
            name: response.data.name,
            email: response.data.email,
            role: response.data.role,
            avatarUrl: response.data.avatarUrl || latestSession?.avatarUrl || null
        };
        populateProfile(originalProfile);
        clearPasswordFields();
        refreshDirtyState();
        const savedAt = new Date().toLocaleTimeString("en-IN", {
            hour: "2-digit",
            minute: "2-digit"
        });
        if (lastSavedNode) {
            lastSavedNode.textContent = `Saved at ${savedAt}`;
        }
        SmartApp.showAlert("profileAlert", "Profile updated successfully", "success");
    });

    resetButton.addEventListener("click", () => {
        if (originalProfile) {
            populateProfile(originalProfile);
        }
        clearPasswordFields();
        refreshDirtyState();
        SmartApp.hideAlert("profileAlert");
    });

    const response = await SmartApp.apiRequest("/api/profile/me");
    if (!response.ok) {
        SmartApp.showAlert("profileAlert", response.data?.message || "Unable to load profile", "error");
        return;
    }

    originalProfile = response.data;
    populateProfile(originalProfile);
    refreshDirtyState();

    function populateProfile(profile) {
        const avatar = profile.avatarUrl || "favicon.svg";
        document.getElementById("profileNameInput").value = profile.name || "";
        document.getElementById("profileEmailInput").value = profile.email || "";
        document.getElementById("profileRoleInput").value = profile.role || "USER";
        document.getElementById("profileSummaryName").textContent = profile.name || "-";
        document.getElementById("profileSummaryEmail").textContent = profile.email || "-";
        document.getElementById("profileSummaryRole").textContent = profile.role || "-";
        document.getElementById("profileHeroName").textContent = profile.name || "User";
        document.getElementById("profileHeroEmail").textContent = profile.email || "-";
        document.getElementById("profileRoleBadge").textContent = profile.role || "USER";
        document.getElementById("profileStateText").textContent = profile.avatarUrl ? "Custom avatar enabled" : "Using default avatar";
        document.getElementById("profileHeroAvatar").textContent = "";
        document.getElementById("profileHeroAvatar").style.backgroundImage = `url(${avatar})`;
        document.getElementById("profileHeroAvatar").style.backgroundSize = "cover";
        document.getElementById("profileHeroAvatar").style.backgroundPosition = "center";
        if (avatarPreview) avatarPreview.src = avatar;
    }

    function clearPasswordFields() {
        document.getElementById("currentPasswordInput").value = "";
        document.getElementById("newPasswordInput").value = "";
        document.getElementById("confirmPasswordInput").value = "";
        if (avatarInput) avatarInput.value = "";
    }

    function refreshDirtyState() {
        if (!originalProfile) return;
        const dirty = isDirty();
        if (dirtyStateNode) {
            dirtyStateNode.textContent = dirty ? "Unsaved changes" : "No unsaved changes";
        }
        if (lastSavedNode) {
            lastSavedNode.textContent = dirty ? "Changes pending save" : "Saved data is up to date";
        }
    }

    function isDirty() {
        if (!originalProfile) return false;

        const currentName = document.getElementById("profileNameInput").value.trim();
        const currentEmail = document.getElementById("profileEmailInput").value.trim().toLowerCase();
        const currentPassword = document.getElementById("currentPasswordInput").value.trim();
        const newPassword = document.getElementById("newPasswordInput").value.trim();
        const confirmPassword = document.getElementById("confirmPasswordInput").value.trim();
        const avatarChanged = Boolean(avatarInput?.files?.length);

        return currentName !== (originalProfile.name || "")
            || currentEmail !== (originalProfile.email || "")
            || avatarChanged
            || Boolean(currentPassword || newPassword || confirmPassword);
    }

    async function uploadAvatar(file) {
        const formData = new FormData();
        formData.append("avatar", file);
        return SmartApp.apiRequest("/api/profile/me/avatar", {
            method: "POST",
            body: formData
        });
    }

    function wirePasswordToggle(buttonId, inputId) {
        const button = document.getElementById(buttonId);
        const input = document.getElementById(inputId);
        if (!button || !input) return;
        button.addEventListener("click", () => {
            input.type = input.type === "password" ? "text" : "password";
            button.textContent = input.type === "password" ? "Show" : "Hide";
        });
    }
});
