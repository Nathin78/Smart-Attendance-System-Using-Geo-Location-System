let allAttendanceRecords = [];
let attendanceSummary = null;
let myLeaveRequests = [];
let currentShift = null;
let stopNotificationPolling = null;

document.addEventListener("DOMContentLoaded", async () => {
    const user = SmartApp.requireAuth("USER");
    if (!user) return;

    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();
    SmartApp.initProfileMenu(user);

    const welcomeNameNode = document.getElementById("welcomeName");
    if (welcomeNameNode) welcomeNameNode.textContent = user.name;
    const userEmailNode = document.getElementById("userEmail");
    if (userEmailNode) userEmailNode.textContent = user.email;

    document.getElementById("markAttendanceBtn").addEventListener("click", () => {
        window.location.href = "attendance.html";
    });
    document.getElementById("statusFilter").addEventListener("change", applyFilters);
    document.getElementById("monthFilter").addEventListener("change", applyFilters);
    document.getElementById("searchFilter").addEventListener("input", applyFilters);
    document.getElementById("sortFilter").addEventListener("change", applyFilters);
    document.getElementById("resetFiltersBtn").addEventListener("click", resetFilters);
    document.getElementById("downloadMyReportBtn").addEventListener("click", downloadMyCsv);
    document.getElementById("leaveRequestForm").addEventListener("submit", submitLeaveRequest);

    const today = new Date().toISOString().split("T")[0];
    document.getElementById("leaveStartDate").value = today;
    document.getElementById("leaveEndDate").value = today;

    await Promise.all([loadGeofence(), loadAttendanceSummary(), loadAttendance(), loadLeaveRequests(), loadShift()]);
    renderCharts();
    stopNotificationPolling = await SmartApp.initNotificationPolling({
        badgeId: "notificationSummaryCount",
        listId: "notificationFeed"
    });
});

async function loadGeofence() {
    const response = await SmartApp.apiRequest("/api/geofence/current");
    if (!response.ok) {
        SmartApp.showAlert("dashboardAlert", response.data?.message || "Unable to load geofence", "warning");
        return;
    }

    const geofence = response.data;
    document.getElementById("geoCenter").textContent = `${geofence.latitude}, ${geofence.longitude}`;
    document.getElementById("geoRadius").textContent = `${geofence.radius} meters`;
}

async function loadAttendance() {
    const response = await SmartApp.apiRequest("/api/attendance/user");

    if (!response.ok) {
        SmartApp.showAlert("dashboardAlert", response.data?.message || "Failed to load attendance history", "error");
        return;
    }

    allAttendanceRecords = Array.isArray(response.data) ? sortAttendanceRecordsDesc(response.data) : [];
    renderStats(allAttendanceRecords);
    applyFilters();
    renderSummary();
}

async function loadAttendanceSummary() {
    const response = await SmartApp.apiRequest("/api/attendance/summary");

    if (!response.ok) {
        renderSummary();
        return;
    }

    attendanceSummary = response.data || null;
    renderSummary();
}

async function loadShift() {
    const response = await SmartApp.apiRequest("/api/shifts/me");
    if (!response.ok || !response.data) {
        renderShift(null);
        return;
    }

    currentShift = response.data;
    renderShift(currentShift);
}

async function loadLeaveRequests() {
    const response = await SmartApp.apiRequest("/api/leave-requests/my");
    if (!response.ok) {
        SmartApp.showAlert("leaveAlert", response.data?.message || "Failed to load leave requests", "error");
        renderLeaveRequests([]);
        return;
    }

    myLeaveRequests = Array.isArray(response.data) ? response.data : [];
    renderLeaveRequests(myLeaveRequests);
}

function renderShift(shift) {
    const shiftRange = document.getElementById("shiftRange");
    const shiftLateAfter = document.getElementById("shiftLateAfter");
    const shiftUpdatedAt = document.getElementById("shiftUpdatedAt");

    if (!shift) {
        if (shiftRange) shiftRange.textContent = "09:00 AM to 04:30 PM";
        if (shiftLateAfter) shiftLateAfter.textContent = "09:30 AM";
        if (shiftUpdatedAt) shiftUpdatedAt.textContent = "Using default schedule";
        return;
    }

    if (shiftRange) {
        shiftRange.textContent = `${SmartApp.formatTime(shift.startTime)} to ${SmartApp.formatTime(shift.endTime)}`;
    }
    if (shiftLateAfter) {
        shiftLateAfter.textContent = SmartApp.formatTime(shift.lateAfter);
    }
    if (shiftUpdatedAt) {
        shiftUpdatedAt.textContent = shift.updatedAt ? new Date(shift.updatedAt).toLocaleString() : "-";
    }
}

async function submitLeaveRequest(event) {
    event.preventDefault();
    SmartApp.hideAlert("leaveAlert");

    const startDate = document.getElementById("leaveStartDate").value;
    const endDate = document.getElementById("leaveEndDate").value;
    const reason = document.getElementById("leaveReason").value.trim();

    const response = await SmartApp.apiRequest("/api/leave-requests", {
        method: "POST",
        body: JSON.stringify({ startDate, endDate, reason })
    });

    if (!response.ok) {
        SmartApp.showAlert("leaveAlert", response.data?.message || "Failed to submit leave request", "error");
        return;
    }

    SmartApp.showAlert("leaveAlert", "Leave request submitted successfully", "success");
    document.getElementById("leaveReason").value = "";
    await loadLeaveRequests();
    renderCharts();
}

function applyFilters() {
    const status = document.getElementById("statusFilter").value;
    const month = document.getElementById("monthFilter").value;
    const query = document.getElementById("searchFilter").value.trim().toLowerCase();
    const sort = document.getElementById("sortFilter").value;

    const filtered = allAttendanceRecords.filter(item => {
        const matchesStatus = status === "ALL" || item.status === status;
        const matchesMonth = !month || (item.date && item.date.startsWith(month));
        const searchText = [
            item.date,
            item.note,
            item.latitude != null && item.longitude != null ? `${item.latitude}, ${item.longitude}` : "",
            item.status
        ].join(" ").toLowerCase();
        const matchesSearch = !query || searchText.includes(query);
        return matchesStatus && matchesMonth && matchesSearch;
    });

    const sorted = filtered.sort((a, b) => {
        const aValue = `${a.date || ""}T${a.time || "00:00:00"}`;
        const bValue = `${b.date || ""}T${b.time || "00:00:00"}`;
        return sort === "OLDEST" ? aValue.localeCompare(bValue) : bValue.localeCompare(aValue);
    });

    document.getElementById("filteredCount").textContent = String(sorted.length);
    renderRows(sorted);
}

function resetFilters() {
    document.getElementById("statusFilter").value = "ALL";
    document.getElementById("monthFilter").value = "";
    document.getElementById("searchFilter").value = "";
    document.getElementById("sortFilter").value = "NEWEST";
    applyFilters();
}

function renderRows(records) {
    const tbody = document.getElementById("attendanceRows");
    if (!records.length) {
        tbody.innerHTML = "<tr><td colspan='7' class='muted'>No attendance records found for selected filters.</td></tr>";
        return;
    }

    tbody.innerHTML = records.map(item => `
        <tr>
            <td>${escapeHtml(item.date || "-")}</td>
            <td>${SmartApp.formatTime(item.time)}</td>
            <td><span class="status-pill ${getStatusClass(item.status)}">${escapeHtml(item.status || "-")}</span></td>
            <td>${formatCoordinate(item.latitude)}, ${formatCoordinate(item.longitude)}</td>
            <td>${item.distanceMeters ?? "-"} m</td>
            <td>${item.accuracyMeters ? `${item.accuracyMeters} m` : "-"}</td>
            <td>${escapeHtml(item.note || "-")}</td>
        </tr>
    `).join("");
}

function renderStats(records) {
    const orderedRecords = sortAttendanceRecordsDesc(records);

    if (!orderedRecords.length) {
        document.getElementById("statTotal").textContent = "0";
        document.getElementById("statPresent").textContent = "0";
        document.getElementById("statLate").textContent = "0";
        document.getElementById("statLast").textContent = "-";
        document.getElementById("statStreak").textContent = "0 days";
        document.getElementById("todayStatus").textContent = "No attendance marked yet.";
        document.getElementById("attendanceRate").textContent = "0%";
        return;
    }

    const total = orderedRecords.length;
    const present = orderedRecords.filter(item => item.status === "PRESENT").length;
    const late = orderedRecords.filter(item => item.status === "LATE").length;
    const latest = orderedRecords[0];
    const today = new Date().toISOString().split("T")[0];
    const todayRecord = orderedRecords.find(item => item.date === today);
    const markedDays = present + late;
    const rate = Math.round((markedDays / total) * 100);
    const bestStreak = calculateBestStreak(orderedRecords);

    document.getElementById("statTotal").textContent = String(total);
    document.getElementById("statPresent").textContent = String(present);
    document.getElementById("statLate").textContent = String(late);
    document.getElementById("statLast").textContent = `${latest.date} ${SmartApp.formatTime(latest.time)}`;
    document.getElementById("statStreak").textContent = `${bestStreak} day${bestStreak === 1 ? "" : "s"}`;
    document.getElementById("todayStatus").textContent = todayRecord
        ? `Marked today as ${todayRecord.status} at ${SmartApp.formatTime(todayRecord.time)}`
        : "Not marked for today yet";
    document.getElementById("attendanceRate").textContent = `${rate}%`;
}

function renderSummary() {
    const summary = attendanceSummary || buildSummaryFromRecords(allAttendanceRecords);
    if (!summary) {
        return;
    }

    document.getElementById("statCurrentStreak").textContent = `${summary.currentStreak || 0} day${summary.currentStreak === 1 ? "" : "s"}`;
    document.getElementById("statMonthRecords").textContent = String(summary.thisMonthRecords || 0);
    document.getElementById("statMonthPresent").textContent = String(summary.thisMonthPresent || 0);
    document.getElementById("statMonthLate").textContent = String(summary.thisMonthLate || 0);
    document.getElementById("attendanceRate").textContent = `${summary.monthlyCompletionRate ?? 0}%`;

    const todayRecord = summary.todayRecord;
    if (todayRecord) {
        document.getElementById("todayStatus").textContent =
            `Marked today as ${todayRecord.status} at ${SmartApp.formatTime(todayRecord.time)}`;
    } else if (summary.lastRecord) {
        document.getElementById("todayStatus").textContent =
            `Last mark: ${summary.lastRecord.date} at ${SmartApp.formatTime(summary.lastRecord.time)} (${summary.lastRecord.status})`;
    } else {
        document.getElementById("todayStatus").textContent = "No attendance marked yet.";
    }

    renderRecentActivity(summary.recentRecords || []);
}

function calculateBestStreak(records) {
    const uniqueDates = [...new Set(records.map(item => item.date).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b));

    if (!uniqueDates.length) return 0;

    let best = 1;
    let current = 1;

    for (let i = 1; i < uniqueDates.length; i++) {
        const prev = new Date(`${uniqueDates[i - 1]}T00:00:00`);
        const next = new Date(`${uniqueDates[i]}T00:00:00`);
        const dayDiff = Math.round((next - prev) / (1000 * 60 * 60 * 24));

        if (dayDiff === 1) {
            current += 1;
            best = Math.max(best, current);
        } else {
            current = 1;
        }
    }

    return best;
}

function calculateCurrentStreak(records) {
    const uniqueDates = [...new Set(records.map(item => item.date).filter(Boolean))]
        .sort((a, b) => b.localeCompare(a));
    if (!uniqueDates.length) return 0;

    let streak = 1;
    for (let i = 1; i < uniqueDates.length; i++) {
        const previous = new Date(`${uniqueDates[i - 1]}T00:00:00`);
        const current = new Date(`${uniqueDates[i]}T00:00:00`);
        const dayDiff = Math.round((previous - current) / (1000 * 60 * 60 * 24));
        if (dayDiff === 1) {
            streak += 1;
        } else {
            break;
        }
    }
    return streak;
}

function buildSummaryFromRecords(records) {
    const orderedRecords = sortAttendanceRecordsDesc(records);

    if (!orderedRecords.length) {
        return {
            currentStreak: 0,
            bestStreak: 0,
            thisMonthRecords: 0,
            thisMonthPresent: 0,
            thisMonthLate: 0,
            monthlyCompletionRate: 0,
            recentRecords: [],
            todayRecord: null,
            lastRecord: null
        };
    }

    const today = new Date().toISOString().split("T")[0];
    const currentMonth = today.slice(0, 7);
    const thisMonthRecords = orderedRecords.filter(item => item.date && item.date.startsWith(currentMonth));
    const lastRecord = orderedRecords[0];
    const todayRecord = orderedRecords.find(item => item.date === today) || null;

    return {
        currentStreak: calculateCurrentStreak(orderedRecords),
        bestStreak: calculateBestStreak(orderedRecords),
        thisMonthRecords: thisMonthRecords.length,
        thisMonthPresent: thisMonthRecords.filter(item => item.status === "PRESENT").length,
        thisMonthLate: thisMonthRecords.filter(item => item.status === "LATE").length,
        monthlyCompletionRate: Math.round((thisMonthRecords.length / Math.max(Number(today.slice(8, 10)), 1)) * 100),
        recentRecords: orderedRecords.slice(0, 5),
        todayRecord,
        lastRecord
    };
}

function renderCharts() {
    renderMonthlyTrendChart();
    renderLateFrequencyChart();
    renderLeavePatternChart();
}

function renderMonthlyTrendChart() {
    const canvas = document.getElementById("monthlyTrendChart");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const months = [...new Set(allAttendanceRecords.map(item => item.date?.slice(0, 7)).filter(Boolean))].slice(0, 6).reverse();
    const data = months.map(month => allAttendanceRecords.filter(item => item.date?.startsWith(month)).length);
    drawBarChart(ctx, canvas, months.map(formatMonthLabel), data, "#0e7490");
}

function renderLateFrequencyChart() {
    const canvas = document.getElementById("lateFrequencyChart");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const lateRecords = allAttendanceRecords.filter(item => item.status === "LATE");
    const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    const counts = days.map((_, index) => lateRecords.filter(item => new Date(`${item.date}T00:00:00`).getDay() === ((index + 1) % 7)).length);
    drawBarChart(ctx, canvas, days, counts, "#c2410c");
}

function renderLeavePatternChart() {
    const canvas = document.getElementById("leavePatternChart");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const approved = myLeaveRequests.filter(item => item.status === "APPROVED").length;
    const pending = myLeaveRequests.filter(item => item.status === "PENDING").length;
    const rejected = myLeaveRequests.filter(item => item.status === "REJECTED").length;
    drawDonutChart(ctx, canvas, [
        { label: "Approved", value: approved, color: "#0b8b53" },
        { label: "Pending", value: pending, color: "#1d4ed8" },
        { label: "Rejected", value: rejected, color: "#b42318" }
    ]);
}

function drawBarChart(ctx, canvas, labels, values, color) {
    if (!ctx) return;
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, width, height);
    const max = Math.max(...values, 1);
    const padding = 32;
    const barWidth = (width - padding * 2) / Math.max(values.length * 1.5, 1);
    ctx.font = "12px sans-serif";
    ctx.textAlign = "center";
    values.forEach((value, index) => {
        const barHeight = ((height - padding * 2) * value) / max;
        const x = padding + index * barWidth * 1.5;
        const y = height - padding - barHeight;
        ctx.fillStyle = color;
        ctx.fillRect(x, y, barWidth, barHeight);
        ctx.fillStyle = "#334155";
        ctx.fillText(labels[index] || "", x + barWidth / 2, height - 10);
    });
}

function drawDonutChart(ctx, canvas, slices) {
    if (!ctx) return;
    const width = canvas.width;
    const height = canvas.height;
    const total = slices.reduce((sum, item) => sum + item.value, 0) || 1;
    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, width, height);

    let start = -Math.PI / 2;
    const cx = width / 2;
    const cy = height / 2 - 10;
    const radius = Math.min(width, height) / 3;

    slices.forEach(slice => {
        const angle = (Math.PI * 2 * slice.value) / total;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.fillStyle = slice.color;
        ctx.arc(cx, cy, radius, start, start + angle);
        ctx.closePath();
        ctx.fill();
        start += angle;
    });

    ctx.globalCompositeOperation = "destination-out";
    ctx.beginPath();
    ctx.arc(cx, cy, radius * 0.55, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalCompositeOperation = "source-over";

    ctx.font = "12px sans-serif";
    ctx.textAlign = "left";
    let legendY = height - 65;
    slices.forEach(slice => {
        ctx.fillStyle = slice.color;
        ctx.fillRect(18, legendY - 10, 10, 10);
        ctx.fillStyle = "#334155";
        ctx.fillText(`${slice.label}: ${slice.value}`, 34, legendY - 1);
        legendY += 18;
    });
}

function formatMonthLabel(value) {
    if (!value) return "";
    const [year, month] = value.split("-");
    return `${month}/${year.slice(2)}`;
}

function renderRecentActivity(records) {
    const container = document.getElementById("recentActivityList");
    if (!container) return;

    if (!records.length) {
        container.innerHTML = "<p class='muted'>No recent attendance activity yet.</p>";
        return;
    }

    container.innerHTML = records.map(item => `
        <article class="activity-item">
            <div class="activity-top">
                <div>
                    <div class="activity-title">${escapeHtml(item.date || "-")} - ${SmartApp.formatTime(item.time)}</div>
                    <div class="activity-meta">${formatCoordinate(item.latitude)}, ${formatCoordinate(item.longitude)} - ${item.distanceMeters ?? "-"} m from center</div>
                </div>
                <span class="status-pill ${getStatusClass(item.status)}">${escapeHtml(item.status || "-")}</span>
            </div>
            <p class="activity-note">${escapeHtml(item.note || "No note added")}</p>
        </article>
    `).join("");
}

function renderLeaveRequests(records) {
    const tbody = document.getElementById("leaveRequestRows");
    if (!tbody) return;

    if (!records.length) {
        tbody.innerHTML = "<tr><td colspan='4' class='muted'>No leave requests submitted yet.</td></tr>";
        return;
    }

    tbody.innerHTML = records.map(item => `
        <tr>
            <td>${escapeHtml(formatLeaveRange(item.startDate, item.endDate))}</td>
            <td><span class="status-pill ${getLeaveStatusClass(item.status)}">${escapeHtml(item.status || "-")}</span></td>
            <td>${escapeHtml(item.reason || "-")}</td>
            <td>${escapeHtml(item.adminComment || "-")}</td>
        </tr>
    `).join("");
}

function formatCoordinate(value) {
    return typeof value === "number" ? value.toFixed(6) : "-";
}

function getStatusClass(status) {
    if (status === "PRESENT") return "present";
    if (status === "LATE") return "late";
    return "muted";
}

function getLeaveStatusClass(status) {
    if (status === "APPROVED") return "approved";
    if (status === "REJECTED") return "rejected";
    return "pending";
}

function sortAttendanceRecordsDesc(records) {
    return [...records].sort((a, b) => {
        const aValue = `${a.date || ""}T${a.time || "00:00:00"}`;
        const bValue = `${b.date || ""}T${b.time || "00:00:00"}`;
        return bValue.localeCompare(aValue);
    });
}

function formatLeaveRange(startDate, endDate) {
    if (!startDate || !endDate) return "-";
    if (startDate === endDate) return startDate;
    return `${startDate} to ${endDate}`;
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function downloadMyCsv() {
    if (!allAttendanceRecords.length) {
        SmartApp.showAlert("dashboardAlert", "No attendance data available to download", "warning");
        return;
    }

    const rows = [
        ["Date", "Time", "Status", "Latitude", "Longitude", "DistanceMeters", "AccuracyMeters", "Note"]
    ];

    allAttendanceRecords.forEach(item => {
        rows.push([
            item.date || "",
            SmartApp.formatTime(item.time),
            item.status || "",
            item.latitude ?? "",
            item.longitude ?? "",
            item.distanceMeters ?? "",
            item.accuracyMeters ?? "",
            item.note ?? ""
        ]);
    });

    const csv = rows.map(row => row.map(value => `"${String(value).replace(/"/g, "\"\"")}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `my-attendance-${new Date().toISOString().split("T")[0]}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    SmartApp.showAlert("dashboardAlert", "CSV downloaded successfully", "success");
}
