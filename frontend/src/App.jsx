import { useEffect, useMemo, useState } from "react";
import "./App.css";
import "leaflet/dist/leaflet.css";
import MapView from "./components/MapView";
import SearchBar from "./components/SearchBar";

const API_BASE_URL = "http://localhost:8080";

function haversineKm(lat1, lon1, lat2, lon2) {
    const toRad = (value) => (value * Math.PI) / 180;
    const earthRadiusKm = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);

    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
}

function formatConnector(connector) {
    const quantity = connector.quantity ?? 1;
    const powerLabel = connector.powerKw ? `${connector.powerKw} kW` : "moc nieznana";
    const currentType = connector.currentType ?? "Rodzaj prądu nieznany";
    const connectorType = connector.connectorType === "Unknown"
        ? "Złącze"
        : (connector.connectorType ?? "typ złącza nieznany");
    return `${connectorType} x${quantity} (${powerLabel}, ${currentType})`;
}


function App() {
    const [location, setLocation] = useState(null);
    const [stations, setStations] = useState([]);
    const [stationsLoading, setStationsLoading] = useState(false);
    const [stationsError, setStationsError] = useState(null);

    const [selectedStationId, setSelectedStationId] = useState(null);
    const [stationDetails, setStationDetails] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [detailsError, setDetailsError] = useState(null);

    useEffect(() => {
        const loadStations = async () => {
            setStationsLoading(true);
            setStationsError(null);

            try {
                const response = await fetch(`${API_BASE_URL}/api/stations`);

                if (!response.ok) {
                    throw new Error(`Błąd HTTP! Status: ${response.status}`);
                }

                const data = await response.json();
                setStations(Array.isArray(data) ? data : []);
            } catch (error) {
                console.error("Błąd pobierania stacji:", error);
                setStationsError("Nie udało się pobrać listy stacji");
            } finally {
                setStationsLoading(false);
            }
        };

        loadStations();
    }, []);

    useEffect(() => {
        if (!selectedStationId) {
            setStationDetails(null);
            setDetailsError(null);
            setDetailsLoading(false);
            return;
        }

        const loadStationDetails = async () => {
            setDetailsLoading(true);
            setDetailsError(null);

            try {
                const response = await fetch(`${API_BASE_URL}/api/stations/${selectedStationId}`);

                if (!response.ok) {
                    throw new Error(`Błąd HTTP! Status: ${response.status}`);
                }

                const data = await response.json();
                setStationDetails(data);
            } catch (error) {
                console.error("Błąd pobierania szczegółów stacji:", error);
                setDetailsError("Nie udało się pobrać szczegółów stacji");
            } finally {
                setDetailsLoading(false);
            }
        };

        loadStationDetails();
    }, [selectedStationId]);

    const distanceKm = useMemo(() => {
        if (
            !location ||
            typeof location.lat !== "number" ||
            typeof location.lon !== "number" ||
            !stationDetails ||
            typeof stationDetails.latitude !== "number" ||
            typeof stationDetails.longitude !== "number"
        ) {
            return null;
        }

        return haversineKm(
            location.lat,
            location.lon,
            stationDetails.latitude,
            stationDetails.longitude
        );
    }, [location, stationDetails]);

    const latestStatus = stationDetails?.latestStatus;

    const handleStationClick = (stationId) => {
        setSelectedStationId(stationId);
    };

    const handleClosePanel = () => {
        setSelectedStationId(null);
        setStationDetails(null);
        setDetailsError(null);
        setDetailsLoading(false);
    };

    return (
        <>
            <SearchBar setLocation={setLocation} />
            <div className="map-wrapper">
                <MapView
                    location={location}
                    stations={stations}
                    selectedStationId={selectedStationId}
                    onStationClick={handleStationClick}
                />
            </div>

            <aside className="station-panel" aria-live="polite">
                <div className="station-panel-header">
                    <h3>Szczegóły stacji</h3>
                    {selectedStationId && (
                        <button
                            type="button"
                            className="panel-close-btn"
                            onClick={handleClosePanel}
                            aria-label="Zamknij panel szczegółów"
                        >
                            X
                        </button>
                    )}
                </div>

                {stationsLoading && <p>Ładowanie stacji...</p>}
                {stationsError && <p className="panel-error">{stationsError}</p>}
                {!stationsLoading && !stationsError && (
                    <p className="panel-meta">Załadowane stacje: {stations.length}</p>
                )}

                {!selectedStationId && (
                    <p className="panel-empty">Kliknij marker, aby zobaczyć szczegóły stacji.</p>
                )}

                {selectedStationId && detailsLoading && <p>Ładowanie szczegółów...</p>}
                {selectedStationId && detailsError && <p className="panel-error">{detailsError}</p>}

                {selectedStationId && stationDetails && !detailsLoading && !detailsError && (
                    <div className="panel-content">
                        <p><strong>{stationDetails.name ?? "Bez nazwy"}</strong></p>
                        <p>{stationDetails.addressLine ?? "Adres nieznany"}</p>
                        <p>{stationDetails.city ?? "Miasto nieznane"}, {stationDetails.country ?? "Kraj nieznany"}</p>

                        <p className="panel-section-title">Odległość</p>
                        <p>
                            {distanceKm == null
                                ? "Wyszukaj lokalizację, aby policzyć odległość"
                                : `${distanceKm.toFixed(2)} km`}
                        </p>

                        <p className="panel-section-title">Godziny dostępności</p>
                        <p>{stationDetails.openingHours || "Nie podano"}</p>

                        <p className="panel-section-title">Status</p>
                        {!latestStatus && <p>Brak danych statusowych</p>}
                        {latestStatus && (
                            <div className="status-grid">
                                <span>Dostępne: {latestStatus.availableCount ?? 0}</span>
                                <span>Zajęte: {latestStatus.occupiedCount ?? 0}</span>
                                <span>Zarezerwowane: {latestStatus.reservedCount ?? 0}</span>
                                <span>Wyłączone: {latestStatus.outOfServiceCount ?? 0}</span>
                                <span>Nieznane: {latestStatus.unknownCount ?? 0}</span>
                            </div>
                        )}

                        <p className="panel-section-title">Złącza i moc</p>
                        {!stationDetails.connectors?.length && <p>Brak informacji o złączach</p>}
                        {!!stationDetails.connectors?.length && (
                            <>
                                <ul className="connector-list">
                                    {stationDetails.connectors.map((connector) => (
                                        <li key={connector.id}>{formatConnector(connector)}</li>
                                    ))}
                                </ul>
                            </>
                        )}
                    </div>
                )}
            </aside>
        </>
    );
}

export default App;
