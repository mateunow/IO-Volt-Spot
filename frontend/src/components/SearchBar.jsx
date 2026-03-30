import {useState} from "react";

function SearchBar({ setLocation }) {
  const [query, setQuery] = useState("");

  const handleSearch = async () => {
    if (!query) return;

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
      }
    } catch (err) {
      console.error("Błąd wyszukiwania:", err);
    }
  };

  return (
    <div className="search-bar">
      <input
        type="text"
        placeholder="Szukaj miasta..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <button onClick={handleSearch}>Szukaj</button>
    </div>
  );
}

export default SearchBar;