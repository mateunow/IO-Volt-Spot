import { memo, useCallback, useMemo, useState, useRef, useEffect } from "react";
import StationCard from "./StationCard.jsx";
import { IconBolt, IconChevronRight } from "./Icons.jsx";
import { STATUS_COLORS } from "./markerIcons.js";

const INITIAL_LIST_LIMIT = 50;
const LIST_LIMIT_STEP = 50;

const FavoriteRow = memo(function FavoriteRow({ favorite, isActive, onSelect, onRemove }) {
    const station = useMemo(
        () => ({
            id: favorite.stationId,
            name: favorite.stationName,
            city: favorite.city,
            operatorName: favorite.operatorName,
            latitude: favorite.latitude,
            longitude: favorite.longitude,
            markerStatus: "DEFAULT",
            addressLine: null,
        }),
        [favorite],
    );
    const handleRemove = useCallback(
        (e) => {
            e.stopPropagation();
            onRemove?.(favorite.stationId);
        },
        [onRemove, favorite.stationId],
    );

    return (
        <div className="favorite-row">
            <StationCard
                station={station}
                isActive={isActive}
                distanceKm={null}
                onSelect={onSelect}
            />
            <button
                className="favorite-remove-btn"
                title="Usuń z ulubionych"
                onClick={handleRemove}
            >
                <svg width="10" height="2" viewBox="0 0 10 2" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <line x1="0" y1="1" x2="10" y2="1" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
            </button>
        </div>
    );
});

function formatDate(iso) {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("pl-PL", { dateStyle: "short", timeStyle: "short" });
}

const RADIUS_OPTIONS = [2, 10, 25, 50];

const STATUS_FILTERS = [
    { key: "WORKING", label: "Dostępne", color: STATUS_COLORS.WORKING.fill },
    { key: "OCCUPIED", label: "Zajęte", color: STATUS_COLORS.OCCUPIED.fill },
    { key: "DISABLED", label: "Wyłączone", color: STATUS_COLORS.DISABLED.fill },
    { key: "DEFAULT", label: "Nieznane", color: STATUS_COLORS.DEFAULT.fill },
];

const POWER_PRESETS = [
    { label: "7+ kW", value: 7 },
    { label: "22+ kW", value: 22 },
    { label: "50+ kW", value: 50 },
    { label: "150+ kW", value: 150 },
];

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

function Sidebar({
    stations,
    stationsLoading,
    stationsError,
    selectedStationId,
    onStationClick,
    favoritesList = [],
    favoritesListLoading = false,
    onFavoriteClick,
    onRemoveFavoriteById,
    location,
    mapCenter,
    searchRadiusKm,
    setSearchRadiusKm,
    currentUser,
    onLoginClick,
    onLogoutClick,
    activeStatuses,
    onToggleStatus,
    onClearStatuses,
    allStations = [],
    advancedFilters,
    onAdvancedFilterChange,
    onClearAdvancedFilters,
    onToggleSidebar,
    isAdmin = false,
    adminOverrides = [],
    adminOverridesLoading = false,
    onConfirmOverride,
    onRejectOverride,
}) {
    const [showProfileMenu, setShowProfileMenu] = useState(false);
    const [favoritesCollapsed, setFavoritesCollapsed] = useState(true);
    const [advancedCollapsed, setAdvancedCollapsed] = useState(true);
    const [adminCollapsed, setAdminCollapsed] = useState(false);
    const [listLimit, setListLimit] = useState(INITIAL_LIST_LIMIT);
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

    const availableConnectorTypes = useMemo(() => {
        const types = new Set();
        for (const s of allStations) {
            for (const t of s.connectorTypes ?? []) {
                types.add(t);
            }
        }
        return [...types].sort();
    }, [allStations]);

    const availableOperators = useMemo(() => {
        const ops = new Set();
        for (const s of allStations) {
            if (s.operatorName) ops.add(s.operatorName);
        }
        return [...ops].sort();
    }, [allStations]);

    const advancedActiveCount = useMemo(() => {
        let count = 0;
        if (advancedFilters.connectorTypes.size > 0) count++;
        if (advancedFilters.minPowerKw != null) count++;
        if (advancedFilters.only24h) count++;
        if (advancedFilters.operators.size > 0) count++;
        return count;
    }, [advancedFilters]);

    const sortedStations = useMemo(() => {
        const ref = mapCenter ?? location;
        const withDist = stations.map((s) => {
            const lat = Number(s.latitude);
            const lon = Number(s.longitude);
            const dist =
                ref && !Number.isNaN(lat) && !Number.isNaN(lon)
                    ? haversineKm(ref.lat, ref.lon, lat, lon)
                    : null;
            return { station: s, distance: dist };
        });
        withDist.sort((a, b) => {
            if (a.distance != null && b.distance != null)
                return a.distance - b.distance;
            if (a.distance != null) return -1;
            if (b.distance != null) return 1;
            return 0;
        });
        return withDist;
    }, [stations, mapCenter, location]);

    const visibleSortedStations = useMemo(
        () => sortedStations.slice(0, listLimit),
        [sortedStations, listLimit],
    );

    useEffect(() => {
        setListLimit(INITIAL_LIST_LIMIT);
    }, [stations.length]);

    function toggleConnectorType(type) {
        const next = new Set(advancedFilters.connectorTypes);
        if (next.has(type)) next.delete(type);
        else next.add(type);
        onAdvancedFilterChange("connectorTypes", next);
    }

    function toggleOperator(op) {
        const next = new Set(advancedFilters.operators);
        if (next.has(op)) next.delete(op);
        else next.add(op);
        onAdvancedFilterChange("operators", next);
    }

    return (
        <aside className="sidebar">
            <div className="brand">
                <div className="brand-mark">
                    <IconBolt />
                </div>
                <div>
                    <div className="brand-name">
                        Volt Spot <span className="dot" />
                    </div>
                    <div className="brand-sub">dane na żywo</div>
                </div>
                {currentUser ? (
                    <div className="user-chip-wrap" ref={profileRef}>
                        <div
                            className="user-chip"
                            onClick={() => setShowProfileMenu((v) => !v)}
                            title="Konto"
                        >
                            <div className="avatar">
                                {(currentUser.displayName ??
                                    "?")[0]?.toUpperCase()}
                            </div>
                            <span>
                                {currentUser.displayName ?? "Użytkownik"}
                            </span>
                        </div>
                        {showProfileMenu && (
                            <div className="profile-menu">
                                <div className="profile-menu-name">
                                    {currentUser.displayName ?? "Użytkownik"}
                                </div>
                                <div className="profile-menu-email">
                                    {currentUser.email ?? ""}
                                </div>
                                <button
                                    className="profile-menu-logout"
                                    onClick={() => {
                                        setShowProfileMenu(false);
                                        onLogoutClick();
                                    }}
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
                                <span
                                    className="dot"
                                    style={{ background: f.color }}
                                />
                                {f.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            <div className="filters-section">
                <div className="filters-header">
                    <button
                        type="button"
                        className="section-toggle"
                        onClick={() => setAdvancedCollapsed((v) => !v)}
                        aria-expanded={!advancedCollapsed}
                    >
                        <span>Filtry zaawansowane</span>
                        {advancedActiveCount > 0 && (
                            <span
                                className="count"
                                style={{ cursor: "pointer" }}
                                onClick={(e) => { e.stopPropagation(); onClearAdvancedFilters(); }}
                                title="Wyczyść filtry zaawansowane"
                            >
                                {advancedActiveCount} aktywnych · wyczyść
                            </span>
                        )}
                        <IconChevronRight
                            className={`section-toggle-icon${advancedCollapsed ? "" : " open"}`}
                        />
                    </button>
                </div>

                {!advancedCollapsed && (
                    <div className="advanced-filters">
                        {availableConnectorTypes.length > 0 && (
                            <div className="filter-group">
                                <div className="filter-label">Typ złącza</div>
                                <div className="f-chips">
                                    {availableConnectorTypes.map((type) => (
                                        <button
                                            key={type}
                                            className={`f-chip${advancedFilters.connectorTypes.has(type) ? " active" : ""}`}
                                            onClick={() => toggleConnectorType(type)}
                                        >
                                            {type}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="filter-group">
                            <div className="filter-label">Minimalna moc</div>
                            <div className="f-chips">
                                {POWER_PRESETS.map((p) => (
                                    <button
                                        key={p.value}
                                        className={`f-chip${advancedFilters.minPowerKw === p.value ? " active" : ""}`}
                                        onClick={() =>
                                            onAdvancedFilterChange(
                                                "minPowerKw",
                                                advancedFilters.minPowerKw === p.value ? null : p.value,
                                            )
                                        }
                                    >
                                        {p.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="filter-group">
                            <div className="filter-label">Dostępność</div>
                            <div className="f-chips">
                                <button
                                    className={`f-chip${advancedFilters.only24h ? " active" : ""}`}
                                    onClick={() =>
                                        onAdvancedFilterChange("only24h", !advancedFilters.only24h)
                                    }
                                >
                                    24/7
                                </button>
                            </div>
                        </div>

                        {availableOperators.length > 0 && (
                            <div className="filter-group">
                                <div className="filter-label">Operator</div>
                                <div className="f-chips">
                                    {availableOperators.map((op) => (
                                        <button
                                            key={op}
                                            className={`f-chip${advancedFilters.operators.has(op) ? " active" : ""}`}
                                            onClick={() => toggleOperator(op)}
                                        >
                                            {op}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {currentUser && (
                <div className="filters-section">
                    <div className="filters-header">
                        <button
                            type="button"
                            className="section-toggle"
                            onClick={() => setFavoritesCollapsed((v) => !v)}
                            aria-expanded={!favoritesCollapsed}
                        >
                            <span>Ulubione</span>
                            <span className="count">{favoritesList.length}</span>
                            <IconChevronRight
                                className={`section-toggle-icon${favoritesCollapsed ? "" : " open"}`}
                            />
                        </button>
                    </div>
                    {!favoritesCollapsed && (
                        <>
                            {favoritesListLoading && (
                                <div className="empty-state">
                                    Ładowanie ulubionych…
                                </div>
                            )}
                            {!favoritesListLoading && favoritesList.length === 0 && (
                                <div className="empty-state">
                                    Brak ulubionych stacji
                                </div>
                            )}
                            {!favoritesListLoading && favoritesList.length > 0 && (
                                <div className="station-list favorites-list">
                                    {favoritesList.map((favorite) => (
                                        <FavoriteRow
                                            key={favorite.id}
                                            favorite={favorite}
                                            isActive={
                                                String(favorite.stationId) ===
                                                String(selectedStationId)
                                            }
                                            onSelect={onStationClick}
                                            onRemove={onRemoveFavoriteById}
                                        />
                                    ))}
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}

            {isAdmin && (
                <div className="filters-section">
                    <div className="filters-header">
                        <button
                            type="button"
                            className="section-toggle"
                            onClick={() => setAdminCollapsed((v) => !v)}
                            aria-expanded={!adminCollapsed}
                        >
                            <span>Zgłoszenia statusów</span>
                            <span className="count">{adminOverrides.length}</span>
                            <IconChevronRight
                                className={`section-toggle-icon${adminCollapsed ? "" : " open"}`}
                            />
                        </button>
                    </div>
                    {!adminCollapsed && (
                        <div className="admin-reports-list">
                            {adminOverridesLoading && (
                                <div className="empty-state">Ładowanie zgłoszeń…</div>
                            )}
                            {!adminOverridesLoading && adminOverrides.length === 0 && (
                                <div className="empty-state">Brak oczekujących zgłoszeń</div>
                            )}
                            {adminOverrides.map((o) => (
                                <div key={o.id} className="admin-report-card">
                                    <div className="admin-report-station">
                                        <span className="admin-report-name">{o.stationName}</span>
                                        {o.stationCity && (
                                            <span className="admin-report-city">{o.stationCity}</span>
                                        )}
                                    </div>
                                    <div className="admin-report-meta">
                                        <span className={`admin-report-badge ${o.reportedStatus === "NOT_WORKING" ? "bad" : o.reportedStatus === "OCCUPIED" ? "busy" : "good"}`}>
                                            {o.reportedStatus === "NOT_WORKING" ? "Nie działa" : o.reportedStatus === "OCCUPIED" ? "Zajęta" : "Działa"}
                                        </span>
                                        <span className="admin-report-counts">
                                            ✓ {o.workingCount} · ✗ {o.notWorkingCount}
                                        </span>
                                        <span className="admin-report-date">{formatDate(o.createdAt)}</span>
                                    </div>
                                    <div className="admin-report-actions">
                                        <button
                                            className="admin-report-btn confirm"
                                            onClick={() => onConfirmOverride?.(o.id)}
                                        >
                                            Zatwierdź
                                        </button>
                                        <button
                                            className="admin-report-btn reject"
                                            onClick={() => onRejectOverride?.(o.id)}
                                        >
                                            Odrzuć
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            <div className="list-header">
                <span className="list-title">
                    {location ? "Stacje w pobliżu" : "Wszystkie stacje"}
                </span>
                <span className="list-meta">{sortedStations.length}</span>
            </div>

            <div className="station-list">
                {stationsLoading && (
                    <div className="empty-state">Ładowanie stacji…</div>
                )}
                {stationsError && sortedStations.length === 0 && (
                    <div className="empty-state" style={{ color: "#F87171" }}>
                        {stationsError}
                    </div>
                )}
                {!stationsLoading &&
                    sortedStations.length === 0 &&
                    !stationsError && (
                        <div className="empty-state">
                            Brak stacji do wyświetlenia
                        </div>
                    )}
                {visibleSortedStations.map(({ station, distance }) => (
                    <StationCard
                        key={station.id}
                        station={station}
                        isActive={station.id === selectedStationId}
                        distanceKm={distance}
                        onSelect={onStationClick}
                    />
                ))}
                {sortedStations.length > listLimit && (
                    <button
                        type="button"
                        className="show-more-btn"
                        onClick={() => setListLimit((n) => n + LIST_LIMIT_STEP)}
                    >
                        Pokaż więcej ({sortedStations.length - listLimit} pozostało)
                    </button>
                )}
            </div>

            <div className="sb-footer">
                <div className="live">
                    <span className="dot" /> Dane na żywo · {stations.length}{" "}
                    stacji
                </div>
            </div>
        </aside>
    );
}

export default Sidebar;
