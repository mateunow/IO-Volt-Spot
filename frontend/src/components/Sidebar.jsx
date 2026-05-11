import { useMemo, useState, useRef, useEffect } from "react";
import StationCard from "./StationCard.jsx";
import { IconBolt } from "./Icons.jsx";
import { STATUS_COLORS } from "./markerIcons.js";

const RADIUS_OPTIONS = [2, 10, 25, 50];

const STATUS_FILTERS = [
    { key: "WORKING",  label: "Dostępne",  color: STATUS_COLORS.WORKING.fill },
    { key: "OCCUPIED", label: "Zajęte",    color: STATUS_COLORS.OCCUPIED.fill },
    { key: "DISABLED", label: "Wyłączone", color: STATUS_COLORS.DISABLED.fill },
    { key: "DEFAULT",  label: "Nieznane",  color: STATUS_COLORS.DEFAULT.fill },
];

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

function Sidebar({
    stations,
    stationsLoading,
    stationsError,
    selectedStationId,
    onStationClick,
    location,
    searchRadiusKm,
    setSearchRadiusKm,
    currentUser,
    onLoginClick,
    onLogoutClick,
    activeStatuses,
    onToggleStatus,
    onClearStatuses,
    onToggleSidebar,
}) {
    const [showProfileMenu, setShowProfileMenu] = useState(false);
    const profileRef = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (profileRef.current && !profileRef.current.contains(e.target)) {
                setShowProfileMenu(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const sortedStations = useMemo(() => {
        return stations
            .map((s) => {
                const lat = Number(s.latitude);
                const lon = Number(s.longitude);
                const dist = (location && !Number.isNaN(lat) && !Number.isNaN(lon))
                    ? haversineKm(location.lat, location.lon, lat, lon)
                    : null;
                return { ...s, _distance: dist };
            })
            .sort((a, b) => {
                if (a._distance != null && b._distance != null) return a._distance - b._distance;
                if (a._distance != null) return -1;
                if (b._distance != null) return 1;
                return 0;
            });
    }, [stations, location]);

    return (
        <aside className="sidebar">
            <div className="brand">
                <div className="brand-mark">
                    <IconBolt />
                </div>
                <div>
                    <div className="brand-name">Volt Spot <span className="dot" /></div>
                    <div className="brand-sub">dane na żywo</div>
                </div>
                {currentUser ? (
                    <div className="user-chip-wrap" ref={profileRef}>
                        <div
                            className="user-chip"
                            onClick={() => setShowProfileMenu((v) => !v)}
                            title="Konto"
                        >
                            <div className="avatar">{(currentUser.displayName ?? "?")[0]?.toUpperCase()}</div>
                            <span>{currentUser.displayName ?? "Użytkownik"}</span>
                        </div>
                        {showProfileMenu && (
                            <div className="profile-menu">
                                <div className="profile-menu-name">{currentUser.displayName ?? "Użytkownik"}</div>
                                <div className="profile-menu-email">{currentUser.email ?? ""}</div>
                                <button
                                    className="profile-menu-logout"
                                    onClick={() => { setShowProfileMenu(false); onLogoutClick(); }}
                                >
                                    Wyloguj
                                </button>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="user-chip" onClick={onLoginClick}>
                        <div className="avatar">?</div>
                        <span>Zaloguj</span>
                    </div>
                )}
            </div>

            <div className="radius-row">
                {RADIUS_OPTIONS.map((r) => (
                    <button
                        key={r}
                        className={`chip${searchRadiusKm === r ? " active" : ""}`}
                        onClick={() => setSearchRadiusKm(r)}
                        disabled={!location}
                        title={!location ? "Najpierw wybierz lokalizację" : ""}
                    >
                        {r} km
                    </button>
                ))}
            </div>

            <div className="filters-section">
                <div className="filters-header">
                    <span>Filtry statusu</span>
                    {activeStatuses.size > 0 && (
                        <span
                            className="count"
                            style={{ cursor: "pointer" }}
                            onClick={onClearStatuses}
                            title="Wyczyść filtry"
                        >
                            {activeStatuses.size} aktywnych · wyczyść
                        </span>
                    )}
                    {activeStatuses.size === 0 && (
                        <span className="count">wszystkie</span>
                    )}
                </div>
                <div className="filter-group">
                    <div className="f-chips">
                        {STATUS_FILTERS.map((f) => (
                            <button
                                key={f.key}
                                className={`f-chip${activeStatuses.has(f.key) ? " active" : ""}`}
                                onClick={() => onToggleStatus(f.key)}
                            >
                                <span className="dot" style={{ background: f.color }} />
                                {f.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            <div className="list-header">
                <span className="list-title">
                    {location ? "Stacje w pobliżu" : "Wszystkie stacje"}
                </span>
                <span className="list-meta">{sortedStations.length}</span>
            </div>

            <div className="station-list">
                {stationsLoading && <div className="empty-state">Ładowanie stacji…</div>}
                {stationsError && sortedStations.length === 0 && (
                    <div className="empty-state" style={{ color: "#F87171" }}>{stationsError}</div>
                )}
                {!stationsLoading && sortedStations.length === 0 && !stationsError && (
                    <div className="empty-state">Brak stacji do wyświetlenia</div>
                )}
                {sortedStations.map((s) => (
                    <StationCard
                        key={s.id}
                        station={s}
                        isActive={s.id === selectedStationId}
                        distanceKm={s._distance}
                        onClick={() => onStationClick(s.id)}
                    />
                ))}
            </div>

            <div className="sb-footer">
                <div className="live"><span className="dot" /> Dane na żywo · {stations.length} stacji</div>
            </div>
        </aside>
    );
}

export default Sidebar;
