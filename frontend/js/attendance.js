document.addEventListener("DOMContentLoaded", async () => {
    const user = SmartApp.requireAuth("USER");
    if (!user) return;

    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();
    SmartApp.initProfileMenu(user);

    document.getElementById("nameLabel").textContent = user.name;
    document.getElementById("emailLabel").textContent = user.email;

    await loadGeofence();

    document.getElementById("refreshGeoBtn").addEventListener("click", loadGeofence);
    document.getElementById("markBtn").addEventListener("click", markAttendance);
});

async function loadGeofence() {
    const geofenceRes = await SmartApp.apiRequest("/api/geofence/current");
    if (!geofenceRes.ok) {
        SmartApp.showAlert(
            "attendanceAlert",
            geofenceRes.data?.message || "Unable to load geofence",
            "error"
        );
        return;
    }

    document.getElementById("geofenceText").textContent =
        `${geofenceRes.data.latitude}, ${geofenceRes.data.longitude} (Radius: ${geofenceRes.data.radius} m)`;
}

async function markAttendance() {
    SmartApp.hideAlert("attendanceAlert");
    document.getElementById("coordsText").textContent = "Fetching GPS location...";

    if (!navigator.geolocation) {
        SmartApp.showAlert("attendanceAlert", "Geolocation is not supported in this browser", "error");
        return;
    }

    navigator.geolocation.getCurrentPosition(async (position) => {
        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;
        const accuracyMeters = position.coords.accuracy;
        const note = document.getElementById("attendanceNote").value.trim();

        document.getElementById("coordsText").textContent =
            `Current Coordinates: ${latitude}, ${longitude} (Accuracy: ${accuracyMeters?.toFixed(1) || "-"} m)`;

        const response = await SmartApp.apiRequest("/api/attendance/mark", {
            method: "POST",
            body: JSON.stringify({
                latitude,
                longitude,
                accuracyMeters,
                note: note || null
            })
        });

        if (!response.ok || !response.data?.success) {
            const distanceInfo = response.data?.distanceMeters != null
                ? ` (Distance: ${response.data.distanceMeters} m)`
                : "";
            SmartApp.showAlert(
                "attendanceAlert",
                (response.data?.message || "Attendance failed because of invalid location or request") + distanceInfo,
                "error"
            );
            return;
        }

        SmartApp.showAlert(
            "attendanceAlert",
            `${response.data.message}. Status: ${response.data.status}. Distance: ${response.data.distanceMeters} m`,
            "success"
        );
        document.getElementById("attendanceNote").value = "";
    }, (error) => {
        SmartApp.showAlert("attendanceAlert", `Unable to fetch location: ${error.message}`, "warning");
        document.getElementById("coordsText").textContent = "Location not available";
    }, {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0
    });
}
