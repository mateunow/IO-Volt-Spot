import {useState} from "react";

function SearchBar({ setLocation, setSearchRadiusKm }) {
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
        `http://localhost:8080/api/geocoding/search?query=${query}`
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
      <button onClick={handleSearch}>Szukaj</button>
    </div>
  );
}

export default SearchBar;