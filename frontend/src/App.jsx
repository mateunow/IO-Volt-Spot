import { useEffect, useMemo, useState } from "react";
import "./App.css";
import "leaflet/dist/leaflet.css";
import MapView from "./components/MapView";
import SearchBar from "./components/SearchBar";

const API_BASE_URL = "http://localhost:8080";
const AUTH_TOKEN_KEY = "voltspot_auth_token";
const AUTH_USER_KEY = "voltspot_auth_user";

const OPERATIONAL_STATUS_OPTIONS = [
    { value: "WORKING", label: "Działa" },
    { value: "NOT_WORKING", label: "Nie działa" },
    { value: "BUSY", label: "Zajęta" },
    { value: "LIMITED", label: "Ograniczona" },
    { value: "UNKNOWN", label: "Nieznany" },
];

function haversineKm(lat1, lon1, lat2, lon2) {
    const toRad = (value) => (value * Math.PI) / 180;
    const earthRadiusKm = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);

    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
}

function formatConnector(connector) {
    const quantity = connector.quantity ?? 1;
    const powerLabel = connector.powerKw ? `${connector.powerKw} kW` : "moc nieznana";
    const currentType = connector.currentType ?? "Rodzaj prądu nieznany";
    const connectorType = connector.connectorType === "Unknown"
        ? "Złącze"
        : (connector.connectorType ?? "typ złącza nieznany");
    return `${connectorType} x${quantity} (${powerLabel}, ${currentType})`;
}

function formatOperationalStatus(status) {
    return OPERATIONAL_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status;
}

function buildAuthHeaders(token, extraHeaders = {}) {
    return {
        ...extraHeaders,
        Authorization: `Bearer ${token}`,
    };
}

function readStoredUser() {
    const storedUser = localStorage.getItem(AUTH_USER_KEY);
    if (!storedUser) {
        return null;
    }

    try {
        return JSON.parse(storedUser);
    } catch {
        return null;
    }
}


function App() {
    const [location, setLocation] = useState(null);
    const [searchRadiusKm, setSearchRadiusKm] = useState(null);
    const [stations, setStations] = useState([]);
    const [stationsLoading, setStationsLoading] = useState(false);
    const [stationsError, setStationsError] = useState(null);

    const [selectedStationId, setSelectedStationId] = useState(null);
    const [stationDetails, setStationDetails] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [detailsError, setDetailsError] = useState(null);

    const [stationFeedbacks, setStationFeedbacks] = useState([]);
    const [feedbacksLoading, setFeedbacksLoading] = useState(false);
    const [feedbacksError, setFeedbacksError] = useState(null);

    const [currentUser, setCurrentUser] = useState(() => readStoredUser());
    const [authToken, setAuthToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY) || "");
    const [loginModalOpen, setLoginModalOpen] = useState(false);
    const [authMode, setAuthMode] = useState("login");
    const [loginEmail, setLoginEmail] = useState("");
    const [loginPassword, setLoginPassword] = useState("");
    const [registerDisplayName, setRegisterDisplayName] = useState("");
    const [authLoading, setAuthLoading] = useState(false);
    const [authError, setAuthError] = useState(null);

    const [feedbackStatus, setFeedbackStatus] = useState("WORKING");
    const [feedbackComment, setFeedbackComment] = useState("");
    const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
    const [feedbackActionMessage, setFeedbackActionMessage] = useState(null);

    useEffect(() => {
        const loadStations = async () => {
            setStationsLoading(true);
            setStationsError(null);

            try {
                const response = await fetch(`${API_BASE_URL}/api/stations`);

                if (!response.ok) {
                    throw new Error(`Błąd HTTP! Status: ${response.status}`);
                }

                const data = await response.json();
                setStations(Array.isArray(data) ? data : []);
            } catch (error) {
                console.error("Błąd pobierania stacji:", error);
                setStationsError("Nie udało się pobrać listy stacji");
            } finally {
                setStationsLoading(false);
            }
        };

        loadStations();
    }, []);

    useEffect(() => {
        if (!authToken) {
            setCurrentUser(null);
            localStorage.removeItem(AUTH_TOKEN_KEY);
            localStorage.removeItem(AUTH_USER_KEY);
            return;
        }

        const loadCurrentUser = async () => {
            setAuthLoading(true);

            try {
                const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
                    headers: buildAuthHeaders(authToken),
                });

                if (!response.ok) {
                    throw new Error(`Błąd HTTP! Status: ${response.status}`);
                }

                const user = await response.json();
                setCurrentUser(user);
                localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
            } catch (error) {
                console.error("Nie udało się odczytać sesji:", error);
                setCurrentUser(null);
                setAuthToken("");
                localStorage.removeItem(AUTH_TOKEN_KEY);
                localStorage.removeItem(AUTH_USER_KEY);
            } finally {
                setAuthLoading(false);
            }
        };

        loadCurrentUser();
    }, [authToken]);

    useEffect(() => {
        if (!location) {
            return;
        }

        if (typeof searchRadiusKm !== "number" || Number.isNaN(searchRadiusKm) || searchRadiusKm <= 0) {
            setStationsError("Promień wyszukiwania musi być większy od 0 km");
            return;
        }

        const loadNearbyStations = async () => {
            setStationsLoading(true);
            setStationsError(null);
            setSelectedStationId(null);

            try {
                const params = new URLSearchParams({
                    lat: String(location.lat),
                    lon: String(location.lon),
                    radiusKm: String(searchRadiusKm),
                });

                const response = await fetch(`${API_BASE_URL}/api/stations?${params.toString()}`);

                if (!response.ok) {
                    throw new Error(`Błąd HTTP! Status: ${response.status}`);
                }

                const data = await response.json();
                setStations(Array.isArray(data) ? data : []);
            } catch (error) {
                console.error("Błąd pobierania stacji po odległości:", error);
                setStationsError("Nie udało się pobrać stacji dla podanej lokalizacji i promienia");
            } finally {
                setStationsLoading(false);
            }
        };

        loadNearbyStations();
    }, [location, searchRadiusKm]);

    useEffect(() => {
        if (!selectedStationId) {
            return;
        }

        const stationIsVisible = stations.some((station) => station.id === selectedStationId);
        if (!stationIsVisible) {
            setSelectedStationId(null);
            setStationDetails(null);
        }
    }, [stations, selectedStationId]);

    useEffect(() => {
        if (!selectedStationId) {
            setStationDetails(null);
            setDetailsError(null);
            setDetailsLoading(false);
            setStationFeedbacks([]);
            setFeedbacksError(null);
            setFeedbackActionMessage(null);
            return;
        }

        const loadStationData = async () => {
            setDetailsLoading(true);
            setDetailsError(null);
            setFeedbacksLoading(true);
            setFeedbacksError(null);

            try {
                const [detailsResponse, feedbackResponse] = await Promise.all([
                    fetch(`${API_BASE_URL}/api/stations/${selectedStationId}`),
                    fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/feedback`),
                ]);

                if (!detailsResponse.ok) {
                    throw new Error(`Błąd HTTP! Status: ${detailsResponse.status}`);
                }

                const detailsData = await detailsResponse.json();
                setStationDetails(detailsData);

                if (!feedbackResponse.ok) {
                    throw new Error(`Błąd HTTP! Status: ${feedbackResponse.status}`);
                }

                const feedbackData = await feedbackResponse.json();
                setStationFeedbacks(Array.isArray(feedbackData) ? feedbackData : []);
            } catch (error) {
                console.error("Błąd pobierania danych stacji:", error);
                setDetailsError("Nie udało się pobrać szczegółów stacji");
                setFeedbacksError("Nie udało się pobrać opinii o stacji");
            } finally {
                setDetailsLoading(false);
                setFeedbacksLoading(false);
            }
        };

        loadStationData();
    }, [selectedStationId]);

    const distanceKm = useMemo(() => {
        if (
            !location ||
            typeof location.lat !== "number" ||
            typeof location.lon !== "number" ||
            !stationDetails ||
            typeof stationDetails.latitude !== "number" ||
            typeof stationDetails.longitude !== "number"
        ) {
            return null;
        }

        return haversineKm(
            location.lat,
            location.lon,
            stationDetails.latitude,
            stationDetails.longitude
        );
    }, [location, stationDetails]);

    const latestStatus = stationDetails?.latestStatus;

    const handleStationClick = (stationId) => {
        setSelectedStationId(stationId);
    };

    const handleClosePanel = () => {
        setSelectedStationId(null);
        setStationDetails(null);
        setDetailsError(null);
        setDetailsLoading(false);
        setStationFeedbacks([]);
        setFeedbacksError(null);
        setFeedbackActionMessage(null);
    };

    const handleOpenLogin = () => {
        setAuthError(null);
        setAuthMode("login");
        setLoginModalOpen(true);
    };

    const handleCloseLogin = () => {
        setLoginModalOpen(false);
        setAuthError(null);
    };

    const handleAuthModeToggle = () => {
        setAuthError(null);
        setAuthMode((currentMode) => currentMode === "login" ? "register" : "login");
    };

    const handleLoginSubmit = async (event) => {
        event.preventDefault();
        setAuthError(null);

        try {
            const endpoint = authMode === "login" ? "/api/auth/login" : "/api/auth/register";
            const payload = authMode === "login"
                ? {
                    email: loginEmail,
                    password: loginPassword,
                }
                : {
                    email: loginEmail,
                    password: loginPassword,
                    displayName: registerDisplayName,
                };

            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                throw new Error(errorData?.message || "Nie udało się zalogować");
            }

            const data = await response.json();
            setAuthToken(data.token);
            setCurrentUser(data.user);
            localStorage.setItem(AUTH_TOKEN_KEY, data.token);
            localStorage.setItem(AUTH_USER_KEY, JSON.stringify(data.user));
            setLoginModalOpen(false);
            setLoginPassword("");
            setRegisterDisplayName("");
        } catch (error) {
            setAuthError(error.message);
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
            localStorage.removeItem(AUTH_TOKEN_KEY);
            localStorage.removeItem(AUTH_USER_KEY);
        }
    };

    const refreshStationFeedbacks = async (stationId) => {
        setFeedbacksLoading(true);
        setFeedbacksError(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/stations/${stationId}/feedback`);

            if (!response.ok) {
                throw new Error(`Błąd HTTP! Status: ${response.status}`);
            }

            const data = await response.json();
            setStationFeedbacks(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error("Błąd pobierania opinii:", error);
            setFeedbacksError("Nie udało się pobrać opinii o stacji");
        } finally {
            setFeedbacksLoading(false);
        }
    };

    const handleCreateFeedback = async (status) => {
        if (!authToken || !selectedStationId) {
            return;
        }

        setFeedbackSubmitting(true);
        setFeedbackActionMessage(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/feedback`, {
                method: "POST",
                headers: buildAuthHeaders(authToken, {
                    "Content-Type": "application/json",
                }),
                body: JSON.stringify({
                    operationalStatus: status,
                    comment: feedbackComment.trim() || null,
                }),
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                throw new Error(errorData?.message || "Nie udało się zapisać zgłoszenia");
            }

            setFeedbackComment("");
            setFeedbackStatus(status);
            setFeedbackActionMessage("Zgłoszenie zapisane");
            await refreshStationFeedbacks(selectedStationId);
        } catch (error) {
            setFeedbackActionMessage(error.message);
        } finally {
            setFeedbackSubmitting(false);
        }
    };

    const handleDeleteStation = async () => {
        if (!currentUser || currentUser.role !== "ADMIN" || !selectedStationId) {
            return;
        }

        const confirmed = window.confirm("Na pewno usunąć tę stację?");
        if (!confirmed) {
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}`, {
                method: "DELETE",
                headers: buildAuthHeaders(authToken),
            });

            if (!response.ok && response.status !== 204) {
                throw new Error("Nie udało się usunąć stacji");
            }

            setStations((currentStations) => currentStations.filter((station) => station.id !== selectedStationId));
            handleClosePanel();
        } catch (error) {
            setFeedbackActionMessage(error.message);
        }
    };

    const handleDeleteFeedback = async (feedbackId) => {
        if (!currentUser || currentUser.role !== "ADMIN" || !selectedStationId) {
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/feedback/${feedbackId}`, {
                method: "DELETE",
                headers: buildAuthHeaders(authToken),
            });

            if (!response.ok && response.status !== 204) {
                throw new Error("Nie udało się usunąć opinii");
            }

            await refreshStationFeedbacks(selectedStationId);
        } catch (error) {
            setFeedbackActionMessage(error.message);
        }
    };

    return (
        <>
            <SearchBar
                setLocation={setLocation}
                setSearchRadiusKm={setSearchRadiusKm}
                currentUser={currentUser}
                authLoading={authLoading}
                onLoginClick={handleOpenLogin}
                onLogoutClick={handleLogout}
            />
            <div className="map-wrapper">
                <MapView
                    location={location}
                    stations={stations}
                    selectedStationId={selectedStationId}
                    onStationClick={handleStationClick}
                />
            </div>

            <aside className="station-panel" aria-live="polite">
                <div className="station-panel-header">
                    <h3>Szczegóły stacji</h3>
                    {selectedStationId && (
                        <button
                            type="button"
                            className="panel-close-btn"
                            onClick={handleClosePanel}
                            aria-label="Zamknij panel szczegółów"
                        >
                            X
                        </button>
                    )}
                </div>

                {stationsLoading && <p>Ładowanie stacji...</p>}
                {stationsError && <p className="panel-error">{stationsError}</p>}
                {!stationsLoading && !stationsError && (
                    <>
                        <p className="panel-meta">Załadowane stacje: {stations.length}</p>
                        {!location && <p className="panel-meta">Domyślny widok: Kraków (dane startowe)</p>}
                        {location && typeof searchRadiusKm === "number" && (
                            <p className="panel-meta">
                                Filtrowanie: {searchRadiusKm} km od {location.name ?? "wybranej lokalizacji"}
                            </p>
                        )}
                    </>
                )}

                {!selectedStationId && (
                    <p className="panel-empty">Kliknij marker, aby zobaczyć szczegóły stacji.</p>
                )}

                {selectedStationId && detailsLoading && <p>Ładowanie szczegółów...</p>}
                {selectedStationId && detailsError && <p className="panel-error">{detailsError}</p>}

                {selectedStationId && stationDetails && !detailsLoading && !detailsError && (
                    <div className="panel-content">
                        <p><strong>{stationDetails.name ?? "Bez nazwy"}</strong></p>
                        <p>{stationDetails.addressLine ?? "Adres nieznany"}</p>
                        <p>{stationDetails.city ?? "Miasto nieznane"}, {stationDetails.country ?? "Kraj nieznany"}</p>
                        {currentUser?.role === "ADMIN" && (
                            <button type="button" className="danger-button" onClick={handleDeleteStation}>
                                Usuń stację
                            </button>
                        )}

                        <p className="panel-section-title">Odległość</p>
                        <p>
                            {distanceKm == null
                                ? "Wyszukaj lokalizację, aby policzyć odległość"
                                : `${distanceKm.toFixed(2)} km`}
                        </p>

                        <p className="panel-section-title">Godziny dostępności</p>
                        <p>{stationDetails.openingHours || "Nie podano"}</p>

                        <p className="panel-section-title">Status</p>
                        {!latestStatus && <p>Brak danych statusowych</p>}
                        {latestStatus && (
                            <div className="status-grid">
                                <span>Dostępne: {latestStatus.availableCount ?? 0}</span>
                                <span>Zajęte: {latestStatus.occupiedCount ?? 0}</span>
                                <span>Zarezerwowane: {latestStatus.reservedCount ?? 0}</span>
                                <span>Wyłączone: {latestStatus.outOfServiceCount ?? 0}</span>
                                <span>Nieznane: {latestStatus.unknownCount ?? 0}</span>
                            </div>
                        )}

                        <p className="panel-section-title">Złącza i moc</p>
                        {!stationDetails.connectors?.length && <p>Brak informacji o złączach</p>}
                        {!!stationDetails.connectors?.length && (
                            <>
                                <ul className="connector-list">
                                    {stationDetails.connectors.map((connector) => (
                                        <li key={connector.id}>{formatConnector(connector)}</li>
                                    ))}
                                </ul>
                            </>
                        )}

                        <section className="feedback-section">
                            <p className="panel-section-title">Zgłoś status</p>
                            {!currentUser && (
                                <p className="panel-meta">Zaloguj się, aby zgłosić działanie stacji.</p>
                            )}

                            {currentUser && (
                                <>
                                    <div className="feedback-actions">
                                        <button
                                            type="button"
                                            className={feedbackStatus === "WORKING" ? "feedback-chip active" : "feedback-chip"}
                                            disabled={feedbackSubmitting}
                                            onClick={() => setFeedbackStatus("WORKING")}
                                        >
                                            Działa
                                        </button>
                                        <button
                                            type="button"
                                            className={feedbackStatus === "NOT_WORKING" ? "feedback-chip active" : "feedback-chip"}
                                            disabled={feedbackSubmitting}
                                            onClick={() => setFeedbackStatus("NOT_WORKING")}
                                        >
                                            Nie działa
                                        </button>
                                    </div>

                                    <label className="feedback-label">
                                        Komentarz
                                        <textarea
                                            value={feedbackComment}
                                            onChange={(event) => setFeedbackComment(event.target.value)}
                                            rows="3"
                                            placeholder="Opcjonalny komentarz"
                                        />
                                    </label>

                                    <div className="feedback-submit-row">
                                        <button
                                            type="button"
                                            className="submit-button"
                                            disabled={feedbackSubmitting}
                                            onClick={() => handleCreateFeedback(feedbackStatus)}
                                        >
                                            {feedbackSubmitting ? "Zapisywanie..." : "Zapisz zgłoszenie"}
                                        </button>
                                        <span className="panel-meta">Status: {formatOperationalStatus(feedbackStatus)}</span>
                                    </div>

                                    {feedbackActionMessage && <p className="panel-meta">{feedbackActionMessage}</p>}
                                </>
                            )}
                        </section>

                        <section className="feedback-section">
                            <p className="panel-section-title">Opinie</p>
                            {feedbacksLoading && <p>Ładowanie opinii...</p>}
                            {feedbacksError && <p className="panel-error">{feedbacksError}</p>}
                            {!feedbacksLoading && !feedbacksError && stationFeedbacks.length === 0 && (
                                <p>Brak opinii dla tej stacji.</p>
                            )}
                            <ul className="feedback-list">
                                {stationFeedbacks.map((feedback) => (
                                    <li key={feedback.id} className="feedback-item">
                                        <div>
                                            <strong>{feedback.userDisplayName}</strong>
                                            <span>{formatOperationalStatus(feedback.operationalStatus)}</span>
                                        </div>
                                        {feedback.comment && <p>{feedback.comment}</p>}
                                        <small>{new Date(feedback.createdAt).toLocaleString("pl-PL")}</small>
                                        {currentUser?.role === "ADMIN" && (
                                            <button
                                                type="button"
                                                className="danger-button small"
                                                onClick={() => handleDeleteFeedback(feedback.id)}
                                            >
                                                Usuń opinię
                                            </button>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        </section>
                    </div>
                )}
            </aside>

            {loginModalOpen && (
                <div className="login-modal-backdrop" onClick={handleCloseLogin}>
                    <div className="login-modal" onClick={(event) => event.stopPropagation()}>
                        <h3>{authMode === "login" ? "Logowanie" : "Rejestracja"}</h3>
                        <form onSubmit={handleLoginSubmit}>
                            {authMode === "register" && (
                                <label>
                                    Nazwa użytkownika
                                    <input
                                        type="text"
                                        value={registerDisplayName}
                                        onChange={(event) => setRegisterDisplayName(event.target.value)}
                                        autoComplete="name"
                                        required
                                    />
                                </label>
                            )}
                            <label>
                                Email
                                <input
                                    type="email"
                                    value={loginEmail}
                                    onChange={(event) => setLoginEmail(event.target.value)}
                                    autoComplete="email"
                                    required
                                />
                            </label>
                            <label>
                                Hasło
                                <input
                                    type="password"
                                    value={loginPassword}
                                    onChange={(event) => setLoginPassword(event.target.value)}
                                    autoComplete="current-password"
                                    required
                                />
                            </label>
                            {authError && <p className="panel-error">{authError}</p>}
                            <div className="login-modal-actions">
                                <button type="button" className="panel-close-btn" onClick={handleCloseLogin}>
                                    Anuluj
                                </button>
                                <button type="submit" className="submit-button">
                                    {authMode === "login" ? "Zaloguj" : "Załóż konto"}
                                </button>
                            </div>
                            <button
                                type="button"
                                className="auth-mode-toggle"
                                onClick={handleAuthModeToggle}
                            >
                                {authMode === "login"
                                    ? "Nie masz konta? Zarejestruj się"
                                    : "Masz już konto? Zaloguj się"}
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </>
    );
}

export default App;
