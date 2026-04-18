document.addEventListener("DOMContentLoaded", async () => {
    const user = SmartApp.requireAuth("ADMIN");
    if (!user) return;

    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();
    SmartApp.initProfileMenu(user);

    const adminNameNode = document.getElementById("adminName");
    if (adminNameNode) adminNameNode.textContent = user.name;

    const reportDateInput = document.getElementById("reportDate");
    reportDateInput.value = new Date().toISOString().split("T")[0];
    const autoRefreshToggle = document.getElementById("autoRefreshToggle");
    let refreshTimer = null;

    document.getElementById("loadReportBtn").addEventListener("click", () => loadReport(reportDateInput.value));
    document.getElementById("geofenceForm").addEventListener("submit", updateGeofence);
    document.getElementById("dailyExportBtn").addEventListener("click", () => exportReport("daily", reportDateInput.value));
    document.getElementById("weeklyExportBtn").addEventListener("click", () => exportReport("weekly", reportDateInput.value));
    document.getElementById("monthlyExportBtn").addEventListener("click", () => exportReport("monthly", reportDateInput.value));

    await Promise.all([loadUsers(), loadReport(reportDateInput.value), loadGeofence(), loadLeaveRequests()]);

    function startAutoRefresh() {
        if (refreshTimer) return;
        refreshTimer = setInterval(() => {
            loadUsers();
            loadReport(reportDateInput.value);
            loadLeaveRequests();
        }, 30000);
    }

    function stopAutoRefresh() {
        if (!refreshTimer) return;
        clearInterval(refreshTimer);
        refreshTimer = null;
    }

    autoRefreshToggle.addEventListener("change", () => {
        if (autoRefreshToggle.checked) {
            startAutoRefresh();
            SmartApp.showAlert("adminAlert", "Auto refresh enabled (every 30 seconds)", "info");
            return;
        }
        stopAutoRefresh();
        SmartApp.showAlert("adminAlert", "Auto refresh paused", "warning");
    });

    startAutoRefresh();
});

async function loadUsers() {
    const response = await SmartApp.apiRequest("/api/admin/users");
    const tbody = document.getElementById("userRows");

    if (!response.ok) {
        const message = getAdminErrorMessage(response, "Failed to load users");
        SmartApp.showAlert("adminAlert", message, "error");
        if (response.status === 404) {
            tbody.innerHTML = "<tr><td colspan='4' class='muted'>Resource not found.</td></tr>";
        }
        return;
    }

    tbody.innerHTML = response.data.map(user => `
        <tr>
            <td>${user.id}</td>
            <td>${user.name}</td>
            <td>${user.email}</td>
            <td>${user.role}</td>
        </tr>
    `).join("");
}

async function loadReport(date) {
    const query = date ? `?date=${encodeURIComponent(date)}` : "";
    const response = await SmartApp.apiRequest(`/api/admin/reports${query}`);
    const tbody = document.getElementById("reportRows");

    if (!response.ok) {
        const message = getAdminErrorMessage(response, "Failed to load report");
        SmartApp.showAlert("adminAlert", message, "error");
        if (response.status === 404) {
            tbody.innerHTML = "<tr><td colspan='9' class='muted'>Resource not found.</td></tr>";
        }
        return;
    }

    const report = response.data;
    document.getElementById("totalUsers").textContent = report.totalUsers;
    document.getElementById("totalMarked").textContent = report.totalMarked;
    document.getElementById("presentCount").textContent = report.presentCount;
    document.getElementById("lateCount").textContent = report.lateCount;
    document.getElementById("onLeaveCount").textContent = report.onLeaveCount;
    document.getElementById("absentCount").textContent = report.absentCount;

    if (!report.records.length) {
        tbody.innerHTML = "<tr><td colspan='9' class='muted'>No attendance records for selected date.</td></tr>";
        return;
    }

    tbody.innerHTML = report.records.map(item => `
        <tr>
            <td>${escapeHtml(item.userName || "-")}<br><span class="muted">${escapeHtml(item.userEmail || "")}</span></td>
            <td>${escapeHtml(item.date || "-")}</td>
            <td>${SmartApp.formatTime(item.time)}</td>
            <td>${escapeHtml(item.status || "-")}</td>
            <td>${formatCoordinate(item.latitude)}, ${formatCoordinate(item.longitude)}</td>
            <td>${item.distanceMeters ?? "-"} m</td>
            <td>${item.accuracyMeters ? `${item.accuracyMeters} m` : "-"}</td>
            <td>${escapeHtml(item.note || "-")}</td>
            <td>${escapeHtml(item.message ?? "-")}</td>
        </tr>
    `).join("");
}

async function loadLeaveRequests() {
    const response = await SmartApp.apiRequest("/api/admin/leave-requests");
    const tbody = document.getElementById("leaveRequestRows");

    if (!response.ok) {
        const message = getAdminErrorMessage(response, "Failed to load leave requests");
        SmartApp.showAlert("adminAlert", message, "error");
        if (response.status === 404) {
            tbody.innerHTML = "<tr><td colspan='6' class='muted'>Resource not found.</td></tr>";
        }
        return;
    }

    const requests = Array.isArray(response.data) ? response.data : [];
    if (!requests.length) {
        tbody.innerHTML = "<tr><td colspan='6' class='muted'>No leave requests found.</td></tr>";
        return;
    }

    tbody.innerHTML = requests.map(item => `
        <tr>
            <td>${escapeHtml(item.userName || "-")}<br><span class="muted">${escapeHtml(item.userEmail || "")}</span></td>
            <td>${formatLeaveRange(item.startDate, item.endDate)}</td>
            <td><span class="status-pill ${getLeaveStatusClass(item.status)}">${escapeHtml(item.status || "-")}</span></td>
            <td>${escapeHtml(item.reason || "-")}</td>
            <td>${escapeHtml(item.adminComment || "-")}</td>
            <td>
                ${item.status === "PENDING" ? `
                    <div class="table-actions">
                        <button class="btn-small btn-secondary" type="button" data-leave-action="APPROVED" data-leave-id="${item.id}">Approve</button>
                        <button class="btn-small btn-danger" type="button" data-leave-action="REJECTED" data-leave-id="${item.id}">Reject</button>
                    </div>
                ` : "<span class='muted'>Reviewed</span>"}
            </td>
        </tr>
    `).join("");

    tbody.querySelectorAll("[data-leave-action]").forEach(button => {
        button.addEventListener("click", () => reviewLeaveRequest(button.dataset.leaveId, button.dataset.leaveAction));
    });
}

async function reviewLeaveRequest(id, status) {
    const verb = status === "APPROVED" ? "approving" : "rejecting";
    const adminComment = window.prompt(`Add an optional comment for ${verb} this leave request:`, "");
    const response = await SmartApp.apiRequest(`/api/admin/leave-requests/${id}`, {
        method: "PUT",
        body: JSON.stringify({
            status,
            adminComment: adminComment || null
        })
    });

    if (!response.ok) {
        SmartApp.showAlert("adminAlert", getAdminErrorMessage(response, "Failed to update leave request"), "error");
        return;
    }

    SmartApp.showAlert("adminAlert", `Leave request ${status === "APPROVED" ? "approved" : "rejected"} successfully`, "success");
    await loadLeaveRequests();
    await loadReport(document.getElementById("reportDate").value);
}

async function loadGeofence() {
    const response = await SmartApp.apiRequest("/api/admin/geofence");
    if (!response.ok) {
        SmartApp.showAlert("adminAlert", getAdminErrorMessage(response, "Unable to load geofence"), "error");
        return;
    }

    document.getElementById("latitude").value = response.data.latitude;
    document.getElementById("longitude").value = response.data.longitude;
    document.getElementById("radius").value = response.data.radius;
}

async function updateGeofence(event) {
    event.preventDefault();
    SmartApp.hideAlert("adminAlert");

    const payload = {
        latitude: Number(document.getElementById("latitude").value),
        longitude: Number(document.getElementById("longitude").value),
        radius: Number(document.getElementById("radius").value)
    };

    const response = await SmartApp.apiRequest("/api/admin/geofence", {
        method: "PUT",
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        SmartApp.showAlert("adminAlert", getAdminErrorMessage(response, "Failed to update geofence"), "error");
        return;
    }

    SmartApp.showAlert("adminAlert", "Geofence updated successfully", "success");
}

async function exportReport(type, date) {
    SmartApp.hideAlert("adminAlert");
    const queryParts = [`type=${encodeURIComponent(type)}`];
    if (date) {
        queryParts.push(`date=${encodeURIComponent(date)}`);
    }
    const query = queryParts.join("&");

    try {
        const response = await SmartApp.downloadRequest(`/api/admin/reports/export?${query}`, {
            method: "GET"
        });

        if (!response.ok) {
            const text = await response.text();
            let message = "Failed to export report";
            try {
                const parsed = JSON.parse(text);
                message = parsed.message || message;
            } catch (err) {
                message = message;
            }
            if (response.status === 404) {
                message = "Resource not found.";
            }
            SmartApp.showAlert("adminAlert", message, "error");
            return;
        }

        const blob = await response.blob();
        const fileName = getFileNameFromHeader(response.headers.get("content-disposition"))
            || `attendance-${type}.xlsx`;
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
        SmartApp.showAlert("adminAlert", `${capitalize(type)} Excel downloaded`, "success");
    } catch (error) {
        SmartApp.showAlert("adminAlert", "Unable to download report. Please try again.", "error");
    }
}

function getFileNameFromHeader(dispositionHeader) {
    if (!dispositionHeader) return null;
    const match = dispositionHeader.match(/filename=\"?([^\";]+)\"?/i);
    return match ? match[1] : null;
}

function capitalize(value) {
    return value.charAt(0).toUpperCase() + value.slice(1);
}

function formatCoordinate(value) {
    return typeof value === "number" ? value.toFixed(6) : "-";
}

function formatLeaveRange(startDate, endDate) {
    if (!startDate || !endDate) return "-";
    if (startDate === endDate) return startDate;
    return `${startDate} to ${endDate}`;
}

function getLeaveStatusClass(status) {
    if (status === "APPROVED") return "approved";
    if (status === "REJECTED") return "rejected";
    return "pending";
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function getAdminErrorMessage(response, fallbackMessage) {
    if (response?.status === 404) {
        return "Resource not found.";
    }
    return response.data?.message || fallbackMessage;
}
