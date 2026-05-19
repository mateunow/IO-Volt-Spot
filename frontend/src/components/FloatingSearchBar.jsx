import { useState, useRef, useEffect } from "react";
import { IconSearch } from "./Icons.jsx";

const API_BASE_URL = "";

function norm(str) {
    return (str ?? "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

function scoreMatch(q, station) {
    const nq       = norm(q);
    const name     = norm(station.name);
    const operator = norm(station.operatorName);
    const city     = norm(station.city);
    const addr     = norm(station.addressLine);

    if (city.startsWith(nq))                   return 6;
    if (city.includes(nq))                      return 5;
    if (operator.startsWith(nq))               return 4;
    if (name.startsWith(nq))                   return 3;
    if (operator.includes(nq))                 return 2;
    if (name.includes(nq) || addr.includes(nq)) return 1;
    return 0;
}

function FloatingSearchBar({ setLocation, searchRadiusKm, setSearchRadiusKm, onStationClick, stations: allStations = [] }) {
    const [query, setQuery]               = useState("");
    const [loading, setLoading]           = useState(false);
    const [error, setError]               = useState(null);
    const [suggestions, setSuggestions]   = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const wrapRef = useRef(null);

    // Aktualizuj sugestie podczas pisania
    useEffect(() => {
        const q = query.trim();
        if (!q || q.length < 2) {
            setSuggestions([]);
            setShowSuggestions(false);
            return;
        }
        const matches = allStations
            .map((s) => ({ s, score: scoreMatch(q, s) }))
            .filter((x) => x.score > 0)
            .sort((a, b) => b.score - a.score)
            .slice(0, 8)
            .map((x) => x.s);

        setSuggestions(matches);
        setShowSuggestions(matches.length > 0);
    }, [query, allStations]);

    // Zamknij sugestie po kliknięciu poza komponentem
    useEffect(() => {
        const handler = (e) => {
            if (wrapRef.current && !wrapRef.current.contains(e.target)) {
                setShowSuggestions(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const handleSearch = async () => {
        const q = query.trim();
        if (!q) return;
        setShowSuggestions(false);
        setLoading(true);
        setError(null);
        try {
            const resp = await fetch(
                `${API_BASE_URL}/api/geocoding/search?query=${encodeURIComponent(q)}`
            );
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const data = await resp.json();

            const lat = parseFloat(data.lat);
            const lon = parseFloat(data.lon);
            if (Number.isNaN(lat) || Number.isNaN(lon)) {
                setError("Nie znaleziono lokalizacji");
                return;
            }

            setLocation({ lat, lon, name: data.name ?? q });
            if (!searchRadiusKm) setSearchRadiusKm(10);
        } catch (err) {
            console.error("Błąd wyszukiwania:", err);
            setError("Błąd wyszukiwania");
        } finally {
            setLoading(false);
        }
    };

    const handleSuggestionClick = (station) => {
        setQuery(station.name ?? "");
        setShowSuggestions(false);
        onStationClick?.(station.id);
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter") { e.preventDefault(); handleSearch(); }
        if (e.key === "Escape") setShowSuggestions(false);
    };

    const hasSuggestions = showSuggestions && suggestions.length > 0;
    const hasError = error && !showSuggestions;

    return (
        <div className="route-search-wrap" ref={wrapRef}>
            <div className="route-search">
                <IconSearch />
                <input
                    type="text"
                    placeholder="Szukaj stacji, operatora, adresu…"
                    value={query}
                    onChange={(e) => { setQuery(e.target.value); setError(null); }}
                    onKeyDown={handleKeyDown}
                    onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                    disabled={loading}
                />
                <button
                    className="route-btn"
                    onClick={handleSearch}
                    disabled={loading || !query.trim()}
                >
                    {loading ? "Szukam…" : "Szukaj"}
                </button>
            </div>

            {hasSuggestions && (
                <div className="geo-suggestions">
                    {suggestions.map((s) => (
                        <div
                            key={s.id}
                            className="geo-suggestion"
                            onMouseDown={() => handleSuggestionClick(s)}
                        >
                            <div className="geo-suggestion-name">{s.name || "Bez nazwy"}</div>
                            <div className="geo-suggestion-meta">
                                {[s.operatorName, s.city].filter(Boolean).join(" · ")}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {hasError && (
                <div className="geo-suggestions">
                    <div className="geo-suggestion" style={{ color: "#B91C1C" }}>{error}</div>
                </div>
            )}
        </div>
    );
}

export default FloatingSearchBar;
