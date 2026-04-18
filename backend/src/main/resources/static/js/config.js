(() => {
    const LOCAL_FALLBACK_API = "http://localhost:8081";
    const isHttpProtocol = window.location.protocol === "http:" || window.location.protocol === "https:";
    const sameOriginBase = isHttpProtocol ? window.location.origin : null;
    const host8080 = (isHttpProtocol && window.location.hostname)
        ? `${window.location.protocol}//${window.location.hostname}:8080`
        : null;
    const host8081 = (isHttpProtocol && window.location.hostname)
        ? `${window.location.protocol}//${window.location.hostname}:8081`
        : LOCAL_FALLBACK_API;
    const configuredApiBase = window.__SMART_API_BASE__ || localStorage.getItem("sa_api_base") || null;
    const candidates = [configuredApiBase, host8081, host8080, sameOriginBase, LOCAL_FALLBACK_API]
        .filter(Boolean)
        .filter((value, index, array) => array.indexOf(value) === index);

    window.SMART_ATTENDANCE_CONFIG = {
        apiBase: candidates[0],
        apiBaseCandidates: candidates
    };
})();
