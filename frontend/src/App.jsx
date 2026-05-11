import { useEffect, useMemo, useRef, useState } from "react";
import Sidebar from "./components/Sidebar.jsx";
import MapView from "./components/MapView.jsx";
import MapControls from "./components/MapControls.jsx";
import { Legend, Stats } from "./components/MapOverlays.jsx";
import FloatingSearchBar from "./components/FloatingSearchBar.jsx";
import DetailPanel from "./components/DetailPanel.jsx";
import LoginModal from "./components/LoginModal.jsx";

const API_BASE_URL = "";
const AUTH_TOKEN_KEY = "voltspot_auth_token";
const AUTH_USER_KEY  = "voltspot_auth_user";

function haversineKm(lat1, lon1, lat2, lon2) {
    const toRad = (v) => (v * Math.PI) / 180;
    const R = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function buildAuthHeaders(token, extra = {}) {
    return { ...extra, Authorization: `Bearer ${token}` };
}

function readStoredUser() {
    const stored = localStorage.getItem(AUTH_USER_KEY);
    if (!stored) return null;
    try { return JSON.parse(stored); } catch { return null; }
}

function App() {
    // ─── stations / location state ────────────────────────────────────
    const [location, setLocation] = useState(null);
    const [searchRadiusKm, setSearchRadiusKm] = useState(null);
    const [stations, setStations] = useState([]);
    const [stationsLoading, setStationsLoading] = useState(false);
    const [stationsError, setStationsError] = useState(null);

    // ─── selected station ─────────────────────────────────────────────
    const [selectedStationId, setSelectedStationId] = useState(null);
    const [stationDetails, setStationDetails] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [detailsError, setDetailsError] = useState(null);

    const [stationFeedbacks, setStationFeedbacks] = useState([]);
    const [feedbacksLoading, setFeedbacksLoading] = useState(false);
    const [feedbacksError, setFeedbacksError] = useState(null);

    // ─── auth ─────────────────────────────────────────────────────────
    const [currentUser, setCurrentUser] = useState(() => readStoredUser());
    const [authToken, setAuthToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY) || "");
    const [loginModalOpen, setLoginModalOpen] = useState(false);
    const [authError, setAuthError] = useState(null);

    // ─── feedback form ────────────────────────────────────────────────
    const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
    const [feedbackActionMessage, setFeedbackActionMessage] = useState(null);

    const mapRef = useRef(null);

    // ─── fetch all stations on mount ──────────────────────────────────
    useEffect(() => {
        (async () => {
            setStationsLoading(true);
            setStationsError(null);
            try {
                const resp = await fetch(`${API_BASE_URL}/api/stations`);
                if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                const data = await resp.json();
                setStations(Array.isArray(data) ? data : []);
            } catch (err) {
                console.error(err);
                setStationsError("Nie udało się pobrać listy stacji");
            } finally {
                setStationsLoading(false);
            }
        })();
    }, []);

    // ─── refresh auth user when token changes ─────────────────────────
    useEffect(() => {
        if (!authToken) {
            setCurrentUser(null);
            localStorage.removeItem(AUTH_TOKEN_KEY);
            localStorage.removeItem(AUTH_USER_KEY);
            return;
        }
        (async () => {
            try {
                const resp = await fetch(`${API_BASE_URL}/api/auth/me`, { headers: buildAuthHeaders(authToken) });
                if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                const user = await resp.json();
                setCurrentUser(user);
                localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
            } catch (err) {
                console.error("Auth refresh failed", err);
                setCurrentUser(null);
                setAuthToken("");
            }
        })();
    }, [authToken]);

    // ─── re-fetch stations by location + radius ───────────────────────
    useEffect(() => {
        if (!location) return;
        if (typeof searchRadiusKm !== "number" || searchRadiusKm <= 0) return;

        (async () => {
            setStationsLoading(true);
            setStationsError(null);
            setSelectedStationId(null);
            try {
                const params = new URLSearchParams({
                    lat: String(location.lat),
                    lon: String(location.lon),
                    radiusKm: String(searchRadiusKm),
                });
                const resp = await fetch(`${API_BASE_URL}/api/stations?${params}`);
                if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                const data = await resp.json();
                setStations(Array.isArray(data) ? data : []);
            } catch (err) {
                console.error(err);
                setStationsError("Nie udało się pobrać stacji dla podanej lokalizacji");
            } finally {
                setStationsLoading(false);
            }
        })();
    }, [location, searchRadiusKm]);

    // ─── fetch details + feedback when station selected ───────────────
    useEffect(() => {
        if (!selectedStationId) {
            setStationDetails(null);
            setStationFeedbacks([]);
            setFeedbackActionMessage(null);
            return;
        }
        (async () => {
            setDetailsLoading(true); setDetailsError(null);
            setFeedbacksLoading(true); setFeedbacksError(null);
            try {
                const [dResp, fResp] = await Promise.all([
                    fetch(`${API_BASE_URL}/api/stations/${selectedStationId}`),
                    fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/feedback`),
                ]);
                if (!dResp.ok) throw new Error(`HTTP ${dResp.status}`);
                setStationDetails(await dResp.json());
                if (!fResp.ok) throw new Error(`HTTP ${fResp.status}`);
                setStationFeedbacks(await fResp.json());
            } catch (err) {
                console.error(err);
                setDetailsError("Nie udało się pobrać szczegółów stacji");
                setFeedbacksError("Nie udało się pobrać opinii");
            } finally {
                setDetailsLoading(false);
                setFeedbacksLoading(false);
            }
        })();
    }, [selectedStationId]);

    // ─── notify map to resize when panel opens/closes ─────────────────
    useEffect(() => {
        window.dispatchEvent(new Event("voltspot:resize-map"));
    }, [selectedStationId]);

    const distanceKm = useMemo(() => {
        if (!location || !stationDetails) return null;
        if (typeof stationDetails.latitude !== "number" || typeof stationDetails.longitude !== "number") return null;
        return haversineKm(location.lat, location.lon, stationDetails.latitude, stationDetails.longitude);
    }, [location, stationDetails]);

    // ─── handlers ─────────────────────────────────────────────────────
    const handleStationClick = (id) => setSelectedStationId(id);
    const handleClosePanel = () => setSelectedStationId(null);

    const handleLogin = async ({ mode, email, password, displayName }) => {
        setAuthError(null);
        try {
            const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
            const payload = mode === "login"
                ? { email, password }
                : { email, password, displayName };

            const resp = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => null);
                throw new Error(err?.message ?? "Nie udało się zalogować");
            }
            const data = await resp.json();
            setAuthToken(data.token);
            setCurrentUser(data.user);
            localStorage.setItem(AUTH_TOKEN_KEY, data.token);
            localStorage.setItem(AUTH_USER_KEY, JSON.stringify(data.user));
            setLoginModalOpen(false);
        } catch (err) {
            setAuthError(err.message);
        }
    };

    const handleLogout = async () => {
        try {
            if (authToken) {
                await fetch(`${API_BASE_URL}/api/auth/logout`, {
                    method: "POST",
                    headers: buildAuthHeaders(authToken),
                });
            }
        } finally {
            setAuthToken("");
            setCurrentUser(null);
        }
    };

    const refreshFeedbacks = async (stationId) => {
        try {
            const resp = await fetch(`${API_BASE_URL}/api/stations/${stationId}/feedback`);
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            setStationFeedbacks(await resp.json());
        } catch (err) {
            console.error(err);
        }
    };

    const handleSubmitFeedback = async (status, comment) => {
        if (!authToken || !selectedStationId) return;
        setFeedbackSubmitting(true);
        setFeedbackActionMessage(null);
        try {
            const resp = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/feedback`, {
                method: "POST",
                headers: buildAuthHeaders(authToken, { "Content-Type": "application/json" }),
                body: JSON.stringify({
                    operationalStatus: status,
                    comment: (comment ?? "").trim() || null,
                }),
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => null);
                throw new Error(err?.message ?? "Nie udało się zapisać zgłoszenia");
            }
            setFeedbackActionMessage("Zgłoszenie zapisane");
            await refreshFeedbacks(selectedStationId);
        } catch (err) {
            setFeedbackActionMessage(err.message);
        } finally {
            setFeedbackSubmitting(false);
        }
    };

    const handleDeleteStation = async () => {
        if (!currentUser || currentUser.role !== "ADMIN" || !selectedStationId) return;
        if (!window.confirm("Na pewno usunąć tę stację?")) return;
        try {
            const resp = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}`, {
                method: "DELETE",
                headers: buildAuthHeaders(authToken),
            });
            if (!resp.ok && resp.status !== 204) throw new Error("Nie udało się usunąć stacji");
            setStations((curr) => curr.filter((s) => s.id !== selectedStationId));
            handleClosePanel();
        } catch (err) {
            setFeedbackActionMessage(err.message);
        }
    };

    const handleDeleteFeedback = async (feedbackId) => {
        if (!currentUser || currentUser.role !== "ADMIN" || !selectedStationId) return;
        try {
            const resp = await fetch(
                `${API_BASE_URL}/api/stations/${selectedStationId}/feedback/${feedbackId}`,
                { method: "DELETE", headers: buildAuthHeaders(authToken) }
            );
            if (!resp.ok && resp.status !== 204) throw new Error("Nie udało się usunąć opinii");
            await refreshFeedbacks(selectedStationId);
        } catch (err) {
            setFeedbackActionMessage(err.message);
        }
    };

    // ─── map controls ─────────────────────────────────────────────────
    const handleZoomIn  = () => mapRef.current?.zoomIn();
    const handleZoomOut = () => mapRef.current?.zoomOut();
    const handleLocate  = () => {
        if (!navigator.geolocation) return;
        navigator.geolocation.getCurrentPosition((pos) => {
            setLocation({
                lat: pos.coords.latitude,
                lon: pos.coords.longitude,
                name: "Moja lokalizacja",
            });
            if (!searchRadiusKm) setSearchRadiusKm(10);
        });
    };

    const detailOpen = !!selectedStationId;
    const selectedStation = stations.find((s) => s.id === selectedStationId);

    return (
        <div className="app">
            <Sidebar
                stations={stations}
                stationsLoading={stationsLoading}
                stationsError={stationsError}
                selectedStationId={selectedStationId}
                onStationClick={handleStationClick}
                location={location}
                searchRadiusKm={searchRadiusKm}
                setSearchRadiusKm={setSearchRadiusKm}
                currentUser={currentUser}
                onLoginClick={() => setLoginModalOpen(true)}
                onLogoutClick={handleLogout}
            />

            <main className={`map-area${detailOpen ? " detail-open" : ""}`}>
                <MapView
                    location={location}
                    stations={stations}
                    selectedStationId={selectedStationId}
                    onStationClick={handleStationClick}
                    onMapReady={(m) => { mapRef.current = m; }}
                />

                <div className="map-top">
                    <FloatingSearchBar
                        setLocation={setLocation}
                        searchRadiusKm={searchRadiusKm}
                        setSearchRadiusKm={setSearchRadiusKm}
                    />
                </div>

                <MapControls
                    onZoomIn={handleZoomIn}
                    onZoomOut={handleZoomOut}
                    onLocate={handleLocate}
                />

                <Legend />
                <Stats stations={stations} />
            </main>

            <DetailPanel
                open={detailOpen}
                station={selectedStation}
                details={stationDetails}
                detailsLoading={detailsLoading}
                detailsError={detailsError}
                feedbacks={stationFeedbacks}
                feedbacksLoading={feedbacksLoading}
                feedbacksError={feedbacksError}
                distanceKm={distanceKm}
                currentUser={currentUser}
                onClose={handleClosePanel}
                onSubmitFeedback={handleSubmitFeedback}
                feedbackSubmitting={feedbackSubmitting}
                feedbackActionMessage={feedbackActionMessage}
                onDeleteFeedback={handleDeleteFeedback}
                onDeleteStation={handleDeleteStation}
            />

            <LoginModal
                open={loginModalOpen}
                onClose={() => { setLoginModalOpen(false); setAuthError(null); }}
                onSubmit={handleLogin}
                authError={authError}
            />
        </div>
    );
}

export default App;
