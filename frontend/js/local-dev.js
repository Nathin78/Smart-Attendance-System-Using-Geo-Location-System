(function () {
    if (window.location.protocol !== "file:") {
        return;
    }

    const fileName = window.location.pathname.split("/").pop() || "login.html";
    const target = `http://localhost:8081/${fileName}`;
    window.location.replace(target);
})();
