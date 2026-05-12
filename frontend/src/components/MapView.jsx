import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import { useEffect } from "react";
import { makeIcon, locationIcon, statusColor } from "./markerIcons.js";

const outerBounds = [
    [47.0, 8.07],
    [56.5, 30.09],
];

function MapController({ location, selectedStation }) {
    const map = useMap();

    useEffect(() => {
        if (selectedStation && typeof selectedStation.latitude === "number" && typeof selectedStation.longitude === "number") {
            map.flyTo([selectedStation.latitude, selectedStation.longitude], Math.max(map.getZoom(), 13), { duration: 0.6 });
            return;
        }
        if (location && typeof location.lat === "number" && typeof location.lon === "number") {
            map.flyTo([location.lat, location.lon], 12, { duration: 0.8 });
        }
    }, [location, selectedStation, map]);

    useEffect(() => {
        const resize  = () => setTimeout(() => map.invalidateSize(), 50);
        const zoomIn  = () => map.zoomIn();
        const zoomOut = () => map.zoomOut();
        window.addEventListener("voltspot:resize-map", resize);
        window.addEventListener("voltspot:zoom-in",    zoomIn);
        window.addEventListener("voltspot:zoom-out",   zoomOut);
        return () => {
            window.removeEventListener("voltspot:resize-map", resize);
            window.removeEventListener("voltspot:zoom-in",    zoomIn);
            window.removeEventListener("voltspot:zoom-out",   zoomOut);
        };
    }, [map]);

    return null;
}

function MapView({ location, stations = [], selectedStationId, onStationClick }) {
    const selectedStation = stations.find((s) => s.id === selectedStationId);

    return (
        <MapContainer
            center={[52.1128, 19.21195]}
            zoom={7}
            maxBounds={outerBounds}
            maxBoundsViscosity={0.8}
            zoomControl={false}
            attributionControl={false}
        >
            <TileLayer
                url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
            />
            <MapController location={location} selectedStation={selectedStation} />

            {location && typeof location.lat === "number" && typeof location.lon === "number" && (
                <Marker position={[location.lat, location.lon]} icon={locationIcon}>
                    <Popup>Wybrana lokalizacja: {location.name}</Popup>
                </Marker>
            )}

            {stations.map((station) => {
                const lat = Number(station.latitude);
                const lon = Number(station.longitude);
                if (Number.isNaN(lat) || Number.isNaN(lon)) return null;
                const isSelected = station.id === selectedStationId;
                const c = statusColor(station.markerStatus);

                return (
                    <Marker
                        key={station.id}
                        position={[lat, lon]}
                        icon={makeIcon(station.markerStatus ?? "DEFAULT", isSelected)}
                        eventHandlers={{ click: () => onStationClick?.(station.id) }}
                    >
                        <Popup>
                            <div className="pop-op">
                                {station.operatorName ?? "—"} · {String(station.id).slice(0, 8)}
                            </div>
                            <div className="pop-name">{station.name || "Bez nazwy"}</div>
                            <div className="pop-addr">
                                {[station.addressLine, station.city].filter(Boolean).join(", ") || "Adres nieznany"}
                            </div>
                            <button className="pop-btn" onClick={() => onStationClick?.(station.id)}>
                                Szczegóły
                            </button>
                        </Popup>
                    </Marker>
                );
            })}
        </MapContainer>
    );
}

export default MapView;
