import { useState } from "react";
import { IconSearch } from "./Icons.jsx";

const API_BASE_URL = "";

// Górna wyszukiwarka lokalizacji — wywołuje /api/geocoding/search po naciśnięciu
// Enter lub klinięciu "Szukaj". Backend zwraca {lat, lon, name}.
function FloatingSearchBar({ setLocation, searchRadiusKm, setSearchRadiusKm }) {
    const [query, setQuery] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleSearch = async () => {
        const q = query.trim();
        if (!q) return;

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

    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            handleSearch();
        }
    };

    return (
        <div className="route-search-wrap" style={{ position: "relative" }}>
            <div className="route-search">
                <IconSearch />
                <input
                    type="text"
                    placeholder="Szukaj lokalizacji, miasta lub adresu…"
                    value={query}
                    onChange={(e) => { setQuery(e.target.value); setError(null); }}
                    onKeyDown={handleKeyDown}
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
            {error && (
                <div className="geo-suggestions">
                    <div className="geo-suggestion" style={{ color: "#B91C1C" }}>
                        {error}
                    </div>
                </div>
            )}
        </div>
    );
}

export default FloatingSearchBar;
