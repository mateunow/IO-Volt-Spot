import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Sidebar from "./components/Sidebar.jsx";
import MapView from "./components/MapView.jsx";
import MapControls from "./components/MapControls.jsx";
import { Legend } from "./components/MapOverlays.jsx";
import FloatingSearchBar from "./components/FloatingSearchBar.jsx";
import DetailPanel from "./components/DetailPanel.jsx";
import LoginModal from "./components/LoginModal.jsx";
import { IconChevronLeft, IconChevronRight } from "./components/Icons.jsx";

const API_BASE_URL = "";
const AUTH_TOKEN_KEY = "voltspot_auth_token";
const AUTH_USER_KEY = "voltspot_auth_user";

function haversineKm(lat1, lon1, lat2, lon2) {
    const toRad = (v) => (v * Math.PI) / 180;
    const R = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function buildAuthHeaders(token, extra = {}) {
    return { ...extra, Authorization: `Bearer ${token}` };
}

function readStoredUser() {
    const stored = localStorage.getItem(AUTH_USER_KEY);
    if (!stored) return null;
    try {
        return JSON.parse(stored);
    } catch {
        return null;
    }
}

function resolveMarkerStatusFromOverride(override) {
    if (!override) return null;
    if (override.state === "CONFIRMED") return override.reportedStatus === "NOT_WORKING" ? "DISABLED" : "WORKING";
    if (override.state === "PENDING")   return override.reportedStatus === "NOT_WORKING" ? "DISABLED_UNCONFIRMED" : "WORKING_UNCONFIRMED";
    return null;
}

function App() {
    const [location, setLocation] = useState(null);
    const [searchRadiusKm, setSearchRadiusKm] = useState(null);
    const [stations, setStations] = useState([]);
    const [stationsLoading, setStationsLoading] = useState(false);
    const [stationsError, setStationsError] = useState(null);

    const [activeStatuses, setActiveStatuses] = useState(new Set());
    const [advancedFilters, setAdvancedFilters] = useState({
        connectorTypes: new Set(),
        minPowerKw: null,
        only24h: false,
        operators: new Set(),
    });

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
    const [authError, setAuthError] = useState(null);

    const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
    const [feedbackActionMessage, setFeedbackActionMessage] = useState(null);

    const [isFavorited, setIsFavorited] = useState(false);
    const [favoritesLoading, setFavoritesLoading] = useState(false);
    const [favoritesList, setFavoritesList] = useState([]);
    const [favoritesListLoading, setFavoritesListLoading] = useState(false);

    const [stationEditMode, setStationEditMode] = useState(false);
    const [stationEditForm, setStationEditForm] = useState(null);
    const [stationEditLoading, setStationEditLoading] = useState(false);
    const [stationEditMessage, setStationEditMessage] = useState(null);

    const [mapCenter, setMapCenter] = useState(null);
    const [mapViewport, setMapViewport] = useState(null);

    const [reportSubmitting, setReportSubmitting] = useState(false);
    const [adminOverrides, setAdminOverrides] = useState([]);
    const [adminOverridesLoading, setAdminOverridesLoading] = useState(false);
    const [activeOverrides, setActiveOverrides] = useState([]);
    const lastEtagRef = useRef(null);

    const effectiveStations = useMemo(() => {
        if (activeOverrides.length === 0) return stations;
        const overrideMap = new Map(activeOverrides.map(o => [o.stationId, o]));
        return stations.map(s => {
            const o = overrideMap.get(s.id);
            return o ? { ...s, communityOverride: o, markerStatus: resolveMarkerStatusFromOverride(o) } : s;
        });
    }, [stations, activeOverrides]);

    const STATUS_FILTER_GROUP = {
        WORKING_UNCONFIRMED: "WORKING",
        DISABLED_UNCONFIRMED: "DISABLED",
    };

    const visibleStations = useMemo(() => {
        return effectiveStations.filter((station) => {
            if (activeStatuses.size > 0) {
                const status = station.markerStatus ?? "DEFAULT";
                const group = STATUS_FILTER_GROUP[status] ?? status;
                if (!activeStatuses.has(group)) return false;
            }

            const { connectorTypes, minPowerKw, only24h, operators } = advancedFilters;

            if (connectorTypes.size > 0) {
                const stationTypes = new Set(station.connectorTypes ?? []);
                if (![...connectorTypes].some((t) => stationTypes.has(t))) return false;
            }

            if (minPowerKw != null && (station.maxPowerKw ?? 0) < minPowerKw) return false;

            if (only24h && !(station.openingHours ?? "").toLowerCase().includes("24")) return false;

            if (operators.size > 0 && !operators.has(station.operatorName ?? "")) return false;

            return true;
        });
    }, [effectiveStations, activeStatuses, advancedFilters]);

    const markersOnMap = useMemo(() => {
        if (!mapViewport || mapViewport.zoom < 8) return [];
        const { minLat, maxLat, minLon, maxLon, centerLat, centerLon, zoom } = mapViewport;

        const latPad = (maxLat - minLat) * 0.4;
        const lonPad = (maxLon - minLon) * 0.4;
        const inView = visibleStations.filter((s) => {
            const lat = Number(s.latitude);
            const lon = Number(s.longitude);
            return lat >= minLat - latPad && lat <= maxLat + latPad
                && lon >= minLon - lonPad && lon <= maxLon + lonPad;
        });

        const MAX = zoom >= 13 ? 600 : zoom >= 11 ? 300 : zoom >= 9 ? 150 : 80;
        if (inView.length <= MAX) return inView;

        return inView
            .map((s) => ({ s, d: (Number(s.latitude) - centerLat) ** 2 + (Number(s.longitude) - centerLon) ** 2 }))
            .sort((a, b) => a.d - b.d)
            .slice(0, MAX)
            .map(({ s }) => s);
    }, [visibleStations, mapViewport]);

    const fetchStations = useCallback(async (silent = false) => {
        if (!silent) setStationsLoading(true);
        setStationsError(null);
        try {
            const headers = lastEtagRef.current ? { "If-None-Match": lastEtagRef.current } : {};
            const response = await fetch(`${API_BASE_URL}/api/stations`, { headers });
            if (response.status === 304) return;
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const etag = response.headers.get("ETag");
            if (etag) lastEtagRef.current = etag;
            const data = await response.json();
            setStations(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error(error);
            if (!silent) setStationsError("Nie udało się pobrać listy stacji");
        } finally {
            if (!silent) setStationsLoading(false);
        }
    }, []);

    const fetchActiveOverrides = useCallback(async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/api/community/overrides/active`);
            if (res.ok) setActiveOverrides(await res.json());
        } catch (err) {
            console.error("Failed to fetch active overrides", err);
        }
    }, []);

    useEffect(() => {
        fetchStations();
        const id = setInterval(() => fetchStations(true), 2 * 60 * 1000);
        return () => clearInterval(id);
    }, [fetchStations]);

    useEffect(() => {
        fetchActiveOverrides();
        const id = setInterval(fetchActiveOverrides, 60 * 1000);
        return () => clearInterval(id);
    }, [fetchActiveOverrides]);

    useEffect(() => {
        if (!authToken) {
            setCurrentUser(null);
            localStorage.removeItem(AUTH_TOKEN_KEY);
            localStorage.removeItem(AUTH_USER_KEY);
            return;
        }

        (async () => {
            try {
                const resp = await fetch(`${API_BASE_URL}/api/auth/me`, {
                    headers: buildAuthHeaders(authToken),
                });
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


    useEffect(() => {
        if (!selectedStationId) {
            setStationDetails(null);
            setStationFeedbacks([]);
            setFeedbackActionMessage(null);
            setIsFavorited(false);
            setStationEditMode(false);
            setStationEditForm(null);
            setStationEditMessage(null);
            return;
        }

        (async () => {
            setDetailsLoading(true);
            setDetailsError(null);
            setFeedbacksLoading(true);
            setFeedbacksError(null);
            try {
                const [detailsResponse, feedbackResponse] = await Promise.all([
                    fetch(`${API_BASE_URL}/api/stations/${selectedStationId}`),
                    fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/feedback`),
                ]);
                if (!detailsResponse.ok) throw new Error(`HTTP ${detailsResponse.status}`);
                setStationDetails(await detailsResponse.json());
                if (!feedbackResponse.ok) throw new Error(`HTTP ${feedbackResponse.status}`);
                setStationFeedbacks(await feedbackResponse.json());

                const favoriteResponse = authToken
                    ? await fetch(`${API_BASE_URL}/api/favorites/${selectedStationId}/is-favorited`, {
                          headers: buildAuthHeaders(authToken),
                      })
                    : await fetch(`${API_BASE_URL}/api/favorites/${selectedStationId}/is-favorited`);

                setIsFavorited(favoriteResponse.ok ? Boolean(await favoriteResponse.json()) : false);
            } catch (error) {
                console.error(error);
                setDetailsError("Nie udało się pobrać szczegółów stacji");
                setFeedbacksError("Nie udało się pobrać opinii");
            } finally {
                setDetailsLoading(false);
                setFeedbacksLoading(false);
            }
        })();
    }, [selectedStationId, authToken]);

    useEffect(() => {
        if (!stationDetails) {
            setStationEditForm(null);
            return;
        }

        setStationEditForm({
            name: stationDetails.name ?? "",
            latitude: stationDetails.latitude ?? "",
            longitude: stationDetails.longitude ?? "",
            addressLine: stationDetails.addressLine ?? "",
            city: stationDetails.city ?? "",
            country: stationDetails.country ?? "",
            operatorName: stationDetails.operatorName ?? "",
            openingHours: stationDetails.openingHours ?? "",
            accessType: stationDetails.accessType ?? "",
            active: Boolean(stationDetails.active),
            adminStatus: null,
        });
        setStationEditMode(false);
    }, [stationDetails]);

    useEffect(() => {
        window.dispatchEvent(new Event("voltspot:resize-map"));
    }, [selectedStationId]);

    const distanceKm = useMemo(() => {
        if (!location || !stationDetails) return null;
        if (typeof stationDetails.latitude !== "number" || typeof stationDetails.longitude !== "number") {
            return null;
        }
        return haversineKm(location.lat, location.lon, stationDetails.latitude, stationDetails.longitude);
    }, [location, stationDetails]);

    async function loadUserFavorites() {
        if (!authToken) {
            setFavoritesList([]);
            return;
        }

        setFavoritesListLoading(true);
        try {
            const response = await fetch(`${API_BASE_URL}/api/favorites`, {
                headers: buildAuthHeaders(authToken),
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            setFavoritesList(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error(error);
            setFavoritesList([]);
        } finally {
            setFavoritesListLoading(false);
        }
    }

    useEffect(() => {
        loadUserFavorites();
        if (currentUser?.role === "ADMIN") loadAdminOverrides();
    }, [authToken]);

    const loadAdminOverrides = async () => {
        if (!authToken) return;
        setAdminOverridesLoading(true);
        try {
            const resp = await fetch(`${API_BASE_URL}/api/admin/overrides`, {
                headers: buildAuthHeaders(authToken),
            });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            setAdminOverrides(await resp.json());
        } catch (err) {
            console.error("Failed to load overrides", err);
        } finally {
            setAdminOverridesLoading(false);
        }
    };

    const handleSubmitReport = async (reportedStatus) => {
        if (!selectedStationId || !authToken) return;
        setReportSubmitting(true);
        try {
            const resp = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/report`, {
                method: "POST",
                headers: buildAuthHeaders(authToken, { "Content-Type": "application/json" }),
                body: JSON.stringify({ reportedStatus }),
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                throw new Error(err.message || `HTTP ${resp.status}`);
            }
            const override = await resp.json();
            setActiveOverrides(prev => [
                ...prev.filter(o => o.stationId !== selectedStationId),
                override,
            ]);
            if (currentUser?.role === "ADMIN") loadAdminOverrides();
        } catch (err) {
            console.error("Report failed", err);
        } finally {
            setReportSubmitting(false);
        }
    };

    const handleConfirmOverride = async (overrideId) => {
        try {
            const resp = await fetch(`${API_BASE_URL}/api/admin/overrides/${overrideId}/confirm`, {
                method: "POST",
                headers: buildAuthHeaders(authToken),
            });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const updated = await resp.json();
            setActiveOverrides(prev => prev.map(o => o.id === overrideId ? updated : o));
            await loadAdminOverrides();
        } catch (err) {
            console.error("Confirm override failed", err);
        }
    };

    const handleRejectOverride = async (overrideId) => {
        try {
            const resp = await fetch(`${API_BASE_URL}/api/admin/overrides/${overrideId}/reject`, {
                method: "POST",
                headers: buildAuthHeaders(authToken),
            });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            setAdminOverrides(prev => prev.filter(o => o.id !== overrideId));
            setActiveOverrides(prev => prev.filter(o => o.id !== overrideId));
        } catch (err) {
            console.error("Reject override failed", err);
        }
    };

    const refreshFeedbacks = async (stationId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/stations/${stationId}/feedback`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            setStationFeedbacks(await response.json());
        } catch (error) {
            console.error(error);
        }
    };

    const handleStationClick = (id) => setSelectedStationId(id);
    const handleFavoriteClick = (favorite) =>
        setSelectedStationId(favorite.stationId);
    const handleClosePanel = () => setSelectedStationId(null);

    const handleToggleStatus = (key) => {
        setActiveStatuses((prev) => {
            const next = new Set(prev);
            if (next.has(key)) next.delete(key);
            else next.add(key);
            return next;
        });
    };

    const handleClearStatuses = () => setActiveStatuses(new Set());

    const handleAdvancedFilterChange = (key, value) => {
        setAdvancedFilters((prev) => ({ ...prev, [key]: value }));
    };

    const handleClearAdvancedFilters = () => {
        setAdvancedFilters({ connectorTypes: new Set(), minPowerKw: null, only24h: false, operators: new Set() });
    };

    const handleLogin = async ({ mode, email, password, displayName }) => {
        setAuthError(null);
        try {
            const endpoint =
                mode === "login" ? "/api/auth/login" : "/api/auth/register";
            const payload =
                mode === "login"
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

    const handleSubmitFeedback = async (status, comment) => {
        if (!authToken || !selectedStationId) return;

        setFeedbackSubmitting(true);
        setFeedbackActionMessage(null);
        try {
            const resp = await fetch(
                `${API_BASE_URL}/api/stations/${selectedStationId}/feedback`,
                {
                    method: "POST",
                    headers: buildAuthHeaders(authToken, {
                        "Content-Type": "application/json",
                    }),
                    body: JSON.stringify({
                        operationalStatus: status,
                        comment: (comment ?? "").trim() || null,
                    }),
                },
            );
            if (!resp.ok) {
                const err = await resp.json().catch(() => null);
                throw new Error(
                    err?.message ?? "Nie udało się zapisać zgłoszenia",
                );
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
            const resp = await fetch(
                `${API_BASE_URL}/api/stations/${selectedStationId}`,
                {
                    method: "DELETE",
                    headers: buildAuthHeaders(authToken),
                },
            );
            if (!resp.ok && resp.status !== 204)
                throw new Error("Nie udało się usunąć stacji");
            setStations((curr) =>
                curr.filter((s) => s.id !== selectedStationId),
            );
            handleClosePanel();
        } catch (err) {
            setFeedbackActionMessage(err.message);
        }
    };

    const handleDeleteFeedback = async (feedbackId) => {
        if (!currentUser || !selectedStationId) return;

        const feedback = stationFeedbacks.find(
            (item) => item.id === feedbackId,
        );
        const isOwner =
            feedback && String(feedback.userId) === String(currentUser.id);
        const isAdmin = currentUser.role === "ADMIN";
        if (!isOwner && !isAdmin) return;

        try {
            const resp = await fetch(
                `${API_BASE_URL}/api/stations/${selectedStationId}/feedback/${feedbackId}`,
                { method: "DELETE", headers: buildAuthHeaders(authToken) },
            );
            if (!resp.ok && resp.status !== 204)
                throw new Error("Nie udało się usunąć opinii");
            await refreshFeedbacks(selectedStationId);
        } catch (err) {
            setFeedbackActionMessage(err.message);
        }
    };

    const handleAddFavorite = async () => {
        if (!authToken || !selectedStationId) return;

        setFavoritesLoading(true);
        try {
            const resp = await fetch(
                `${API_BASE_URL}/api/favorites/${selectedStationId}`,
                {
                    method: "POST",
                    headers: buildAuthHeaders(authToken),
                },
            );

            if (!resp.ok) {
                const err = await resp.json().catch(() => null);
                throw new Error(
                    err?.message ?? "Nie udało się dodać do ulubionych",
                );
            }

            setIsFavorited(true);
            await loadUserFavorites();
        } catch (err) {
            setFeedbackActionMessage(err.message);
        } finally {
            setFavoritesLoading(false);
        }
    };

    const handleRemoveFavorite = async () => {
        if (!authToken || !selectedStationId) return;

        setFavoritesLoading(true);
        try {
            const resp = await fetch(
                `${API_BASE_URL}/api/favorites/${selectedStationId}`,
                {
                    method: "DELETE",
                    headers: buildAuthHeaders(authToken),
                },
            );

            if (!resp.ok && resp.status !== 204) {
                throw new Error("Nie udało się usunąć z ulubionych");
            }

            setIsFavorited(false);
            await loadUserFavorites();
        } catch (err) {
            setFeedbackActionMessage(err.message);
        } finally {
            setFavoritesLoading(false);
        }
    };

    const handleRemoveFavoriteById = async (stationId) => {
        if (!authToken || !stationId) return;
        try {
            const resp = await fetch(`${API_BASE_URL}/api/favorites/${stationId}`, {
                method: "DELETE",
                headers: buildAuthHeaders(authToken),
            });
            if (!resp.ok && resp.status !== 204) throw new Error("Nie udało się usunąć z ulubionych");
            if (stationId === selectedStationId) setIsFavorited(false);
            await loadUserFavorites();
        } catch (err) {
            setFeedbackActionMessage(err.message);
        }
    };

    const handleStartStationEdit = () => {
        if (!stationEditForm) return;
        setStationEditMode(true);
        setStationEditMessage(null);
    };

    const handleCancelStationEdit = () => {
        setStationEditMode(false);
        setStationEditMessage(null);
        if (stationDetails) {
            setStationEditForm({
                name: stationDetails.name ?? "",
                latitude: stationDetails.latitude ?? "",
                longitude: stationDetails.longitude ?? "",
                addressLine: stationDetails.addressLine ?? "",
                city: stationDetails.city ?? "",
                country: stationDetails.country ?? "",
                operatorName: stationDetails.operatorName ?? "",
                openingHours: stationDetails.openingHours ?? "",
                accessType: stationDetails.accessType ?? "",
                active: Boolean(stationDetails.active),
            });
        }
    };

    const handleStationEditChange = (field, value) => {
        setStationEditForm((currentForm) => ({
            ...(currentForm ?? {}),
            [field]: value,
        }));
    };

    const handleSaveStationEdit = async () => {
        if (
            !currentUser ||
            currentUser.role !== "ADMIN" ||
            !selectedStationId ||
            !stationEditForm
        ) {
            return;
        }

        const latitude = Number(stationEditForm.latitude);
        const longitude = Number(stationEditForm.longitude);
        if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
            setStationEditMessage("Podaj poprawne współrzędne stacji");
            return;
        }

        setStationEditLoading(true);
        setStationEditMessage(null);

        const str = (v) => (v ?? "").toString().trim();
        try {
            const payload = {
                name: str(stationEditForm.name),
                latitude,
                longitude,
                addressLine: str(stationEditForm.addressLine) || null,
                city: str(stationEditForm.city) || null,
                country: str(stationEditForm.country) || null,
                operatorName: str(stationEditForm.operatorName) || null,
                openingHours: str(stationEditForm.openingHours) || null,
                accessType: str(stationEditForm.accessType) || null,
                active: Boolean(stationEditForm.active),
            };

            const resp = await fetch(
                `${API_BASE_URL}/api/stations/${selectedStationId}`,
                {
                    method: "PUT",
                    headers: buildAuthHeaders(authToken, {
                        "Content-Type": "application/json",
                    }),
                    body: JSON.stringify(payload),
                },
            );

            if (!resp.ok) {
                const err = await resp.json().catch(() => null);
                throw new Error(
                    err?.message ?? "Nie udało się zapisać zmian stacji",
                );
            }

            const updatedStation = await resp.json();
            setStationDetails(updatedStation);
            setStations((currentStations) =>
                currentStations.map((station) =>
                    station.id === updatedStation.id
                        ? {
                              ...station,
                              name: updatedStation.name,
                              latitude: updatedStation.latitude,
                              longitude: updatedStation.longitude,
                              city: updatedStation.city,
                              operatorName: updatedStation.operatorName,
                          }
                        : station,
                ),
            );

            const reportedStatus = stationEditForm.active ? "WORKING" : "NOT_WORKING";
            const reportResp = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}/report`, {
                method: "POST",
                headers: buildAuthHeaders(authToken, { "Content-Type": "application/json" }),
                body: JSON.stringify({ reportedStatus }),
            });
            if (reportResp.ok) {
                const override = await reportResp.json();
                const confirmResp = await fetch(`${API_BASE_URL}/api/admin/overrides/${override.id}/confirm`, {
                    method: "POST",
                    headers: buildAuthHeaders(authToken),
                });
                if (confirmResp.ok) {
                    const confirmed = await confirmResp.json();
                    setActiveOverrides(prev => [
                        ...prev.filter(o => o.stationId !== selectedStationId),
                        confirmed,
                    ]);
                    setAdminOverrides(prev => prev.filter(o => o.id !== override.id));
                }
            }

            setStationEditMode(false);
            setStationEditMessage("Zapisano zmiany stacji");
        } catch (err) {
            setStationEditMessage(err.message);
        } finally {
            setStationEditLoading(false);
        }
    };

    const handleViewportChange = useCallback(({ minLat, maxLat, minLon, maxLon, centerLat, centerLon, zoom }) => {
        setMapCenter({ lat: centerLat, lon: centerLon });
        setMapViewport({ minLat, maxLat, minLon, maxLon, centerLat, centerLon, zoom });
    }, []);

    const handleZoomIn  = () => window.dispatchEvent(new Event("voltspot:zoom-in"));
    const handleZoomOut = () => window.dispatchEvent(new Event("voltspot:zoom-out"));
    const handleLocate = () => {
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

    const [sidebarOpen, setSidebarOpen] = useState(true);

    const detailOpen = !!selectedStationId;
    const selectedStation = effectiveStations.find((s) => s.id === selectedStationId);

    return (
        <div className={`app${sidebarOpen ? "" : " sidebar-hidden"}`}>
            <Sidebar
                stations={visibleStations}
                stationsLoading={stationsLoading}
                stationsError={stationsError}
                selectedStationId={selectedStationId}
                onStationClick={handleStationClick}
                favoritesList={favoritesList}
                favoritesListLoading={favoritesListLoading}
                onFavoriteClick={handleFavoriteClick}
                onRemoveFavoriteById={handleRemoveFavoriteById}
                location={location}
                mapCenter={mapCenter}
                searchRadiusKm={searchRadiusKm}
                setSearchRadiusKm={setSearchRadiusKm}
                currentUser={currentUser}
                onLoginClick={() => setLoginModalOpen(true)}
                onLogoutClick={handleLogout}
                activeStatuses={activeStatuses}
                onToggleStatus={handleToggleStatus}
                onClearStatuses={handleClearStatuses}
                allStations={stations}
                advancedFilters={advancedFilters}
                onAdvancedFilterChange={handleAdvancedFilterChange}
                onClearAdvancedFilters={handleClearAdvancedFilters}
                onToggleSidebar={() => setSidebarOpen((v) => !v)}
                isAdmin={currentUser?.role === "ADMIN"}
                adminOverrides={adminOverrides}
                adminOverridesLoading={adminOverridesLoading}
                onConfirmOverride={handleConfirmOverride}
                onRejectOverride={handleRejectOverride}
            />

            <main className={`map-area${detailOpen ? " detail-open" : ""}`}>
                <button
                    className="sidebar-toggle-btn"
                    onClick={() => setSidebarOpen((v) => !v)}
                    title={sidebarOpen ? "Zwiń panel" : "Rozwiń panel"}
                >
                    {sidebarOpen ? <IconChevronLeft /> : <IconChevronRight />}
                </button>
                <MapView
                    location={location}
                    stations={markersOnMap}
                    selectedStationId={selectedStationId}
                    selectedStation={selectedStation}
                    onStationClick={handleStationClick}
                    onViewportChange={handleViewportChange}
                />

                <div className="map-top">
                    <FloatingSearchBar
                        setLocation={setLocation}
                        searchRadiusKm={searchRadiusKm}
                        setSearchRadiusKm={setSearchRadiusKm}
                        onStationClick={handleStationClick}
                        stations={stations}
                    />
                </div>

                <MapControls
                    onZoomIn={handleZoomIn}
                    onZoomOut={handleZoomOut}
                    onLocate={handleLocate}
                />

                <Legend />
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
                isFavorited={isFavorited}
                favoritesLoading={favoritesLoading}
                onAddFavorite={handleAddFavorite}
                onRemoveFavorite={handleRemoveFavorite}
                stationEditMode={stationEditMode}
                stationEditForm={stationEditForm}
                stationEditLoading={stationEditLoading}
                stationEditMessage={stationEditMessage}
                onStartStationEdit={handleStartStationEdit}
                onCancelStationEdit={handleCancelStationEdit}
                onSaveStationEdit={handleSaveStationEdit}
                onStationEditChange={handleStationEditChange}
                onSubmitReport={handleSubmitReport}
            />

            <LoginModal
                open={loginModalOpen}
                onClose={() => {
                    setLoginModalOpen(false);
                    setAuthError(null);
                }}
                onSubmit={handleLogin}
                authError={authError}
            />
        </div>
    );
}

export default App;
