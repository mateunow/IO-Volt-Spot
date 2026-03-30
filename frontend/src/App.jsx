import { useEffect, useState } from "react";
import "./App.css";
import "leaflet/dist/leaflet.css";
import MapView from "./components/MapView";
import SearchBar from "./components/SearchBar";


function App() {
    const [message, setMessage] = useState("Ładowanie danych z serwera...");
    const [location, setLocation] = useState(null);

    useEffect(() => {
        fetch("http://localhost:8080/api/ping")
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Błąd HTTP! Status: ${response.status}`);
                }
                return response.text();
            })
            .then((data) => {
                setMessage(data);
            })
            .catch((error) => {
                console.error("Błąd połączenia z API:", error);
                setMessage("Nie udało się połączyć z backendem");
            });
    }, []);

    return (
        <>
            {/* <div className="card">
                <p>Wiadomość z backendu:</p>
                <h2>{message}</h2>
            </div> */}
            <SearchBar setLocation={setLocation} />
            <div className="map-wrapper">
            <MapView location={location} />
            </div>
        </>
    );
}

export default App;
