import { useEffect, useState } from "react";
import "./App.css";

function App() {
    const [message, setMessage] = useState("Ładowanie danych z serwera...");

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
            <h1>Frontend Volt Spot</h1>
            <div className="card">
                <p>Wiadomość z backendu:</p>
                <h2>{message}</h2>
            </div>
        </>
    );
}

export default App;
