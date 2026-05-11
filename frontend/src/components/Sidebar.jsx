import { useMemo, useState } from "react";
import StationCard from "./StationCard.jsx";
import { IconSearch, IconBolt } from "./Icons.jsx";
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
}) {
    const [query, setQuery] = useState("");
    const [activeStatuses, setActiveStatuses] = useState(new Set());

    const toggleStatus = (key) => {
        setActiveStatuses((prev) => {
            const next = new Set(prev);
            if (next.has(key)) next.delete(key); else next.add(key);
            return next;
        });
    };

    const filteredStations = useMemo(() => {
        const q = query.trim().toLowerCase();
        return stations
            .filter((s) => {
                if (activeStatuses.size > 0 && !activeStatuses.has(s.markerStatus ?? "DEFAULT")) return false;
                if (!q) return true;
                return [
                    s.name, s.operatorName, s.city, s.addressLine,
                ].some((field) => (field ?? "").toLowerCase().includes(q));
            })
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
    }, [stations, query, activeStatuses, location]);

    const totalStatus = useMemo(() => {
        let available = 0, occupied = 0, total = 0;
        for (const s of stations) {
            const ls = s.latestStatus ?? {};
            available += ls.availableCount ?? 0;
            occupied  += ls.occupiedCount ?? 0;
            total += (ls.availableCount ?? 0)
                + (ls.occupiedCount ?? 0)
                + (ls.reservedCount ?? 0)
                + (ls.outOfServiceCount ?? 0)
                + (ls.unknownCount ?? 0);
        }
        return { available, occupied, total };
    }, [stations]);

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
                    <div className="user-chip" onClick={onLogoutClick} title="Wyloguj">
                        <div className="avatar">{(currentUser.displayName ?? "?")[0]?.toUpperCase()}</div>
                        <span>{currentUser.displayName ?? "Użytkownik"}</span>
                    </div>
                ) : (
                    <div className="user-chip" onClick={onLoginClick}>
                        <div className="avatar">?</div>
                        <span>Zaloguj</span>
                    </div>
                )}
            </div>

            <div className="search-wrap">
                <div className="search">
                    <IconSearch />
                    <input
                        type="text"
                        placeholder="Szukaj stacji, operatora, miasta…"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                    />
                </div>
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
                    <span className="count">{activeStatuses.size > 0 ? `${activeStatuses.size} aktywnych` : "wszystkie"}</span>
                </div>
                <div className="filter-group">
                    <div className="f-chips">
                        {STATUS_FILTERS.map((f) => (
                            <button
                                key={f.key}
                                className={`f-chip${activeStatuses.has(f.key) ? " active" : ""}`}
                                onClick={() => toggleStatus(f.key)}
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
                <span className="list-meta">{filteredStations.length}</span>
            </div>

            <div className="station-list">
                {stationsLoading && <div className="empty-state">Ładowanie stacji…</div>}
                {stationsError && <div className="empty-state" style={{ color: "#F87171" }}>{stationsError}</div>}
                {!stationsLoading && !stationsError && filteredStations.length === 0 && (
                    <div className="empty-state">Brak stacji do wyświetlenia</div>
                )}
                {filteredStations.map((s) => (
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
                <div>{totalStatus.available}/{totalStatus.total || "-"} dost.</div>
            </div>
        </aside>
    );
}

export default Sidebar;
