document.addEventListener("DOMContentLoaded", () => {
    SmartApp.applyTheme();
    SmartApp.initThemeControl();
    SmartApp.initHeaderClock();

    const form = document.getElementById("contactForm");
    form.addEventListener("submit", handleSubmit);
});

function handleSubmit(event) {
    event.preventDefault();
    SmartApp.hideAlert("contactAlert");

    const name = document.getElementById("contactName").value.trim();
    const email = document.getElementById("contactEmail").value.trim();
    const subject = document.getElementById("contactSubject").value.trim();
    const message = document.getElementById("contactMessage").value.trim();

    if (!name || !email || !subject || !message) {
        SmartApp.showAlert("contactAlert", "Please fill in all fields", "error");
        return;
    }

    SmartApp.showAlert("contactAlert", "Thanks! Your message has been received.", "success");
    event.target.reset();
}
