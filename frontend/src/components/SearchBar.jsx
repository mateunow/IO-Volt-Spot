import {useState} from "react";

function SearchBar({ setLocation, setSearchRadiusKm, currentUser, authLoading, onLoginClick, onLogoutClick }) {
  const [query, setQuery] = useState("");
  const [radiusKm, setRadiusKm] = useState("10");

  const handleSearch = async () => {
    if (!query) return;

    const parsedRadius = Number(radiusKm);
    if (Number.isNaN(parsedRadius) || parsedRadius <= 0) {
      return;
    }

    try {
      const res = await fetch(
        `api/geocoding/search?query=${query}`
      );

      const data = await res.json();
      
      if (data.lat && data.lon) {
        setLocation({
          lat: parseFloat(data.lat),
          lon: parseFloat(data.lon),
          name: data.name,
        });
        setSearchRadiusKm(parsedRadius);
      }
    } catch (err) {
      console.error("Błąd wyszukiwania:", err);
    }
  };

  return (
    <div className="search-bar">
      <div className="search-bar-row">
        <input
          type="text"
          placeholder="Szukaj adresu..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <input
          type="number"
          min="1"
          step="1"
          placeholder="Promień (km)"
          value={radiusKm}
          onChange={(e) => setRadiusKm(e.target.value)}
          aria-label="Promień wyszukiwania w kilometrach"
        />
        <button type="button" onClick={handleSearch}>Szukaj</button>
      </div>

      <div className="search-bar-auth">
        {currentUser ? (
          <>
            <span>
              {currentUser.displayName} · {currentUser.role}
            </span>
            <button type="button" onClick={onLogoutClick} disabled={authLoading}>
              Wyloguj
            </button>
          </>
        ) : (
          <button type="button" onClick={onLoginClick} disabled={authLoading}>
            Zaloguj
          </button>
        )}
      </div>
    </div>
  );
}

export default SearchBar;