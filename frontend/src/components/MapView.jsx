import { MapContainer, TileLayer, Marker, Popup, useMap, useMapEvents } from "react-leaflet";
import { memo, useCallback, useEffect } from "react";
import { makeIcon, locationIcon, draftStationIcon } from "./markerIcons.js";

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

function CreateModeClickHandler({ onMapClick }) {
    useMapEvents({
        click(e) {
            onMapClick?.(e.latlng.lat, e.latlng.lng);
        },
    });
    return null;
}

function BoundsWatcher({ onViewportChange }) {
    const map = useMapEvents({});

    useEffect(() => {
        const snapshot = () => {
            const zoom = map.getZoom();
            const bounds = map.getBounds();
            const center = bounds.getCenter();
            return {
                minLat: bounds.getSouth(),
                maxLat: bounds.getNorth(),
                minLon: bounds.getWest(),
                maxLon: bounds.getEast(),
                centerLat: center.lat,
                centerLon: center.lng,
                zoom,
            };
        };

        const handler = () => {
            onViewportChange(snapshot());
        };

        map.on("moveend", handler);
        map.on("zoomend", handler);
        onViewportChange(snapshot());

        return () => {
            map.off("moveend", handler);
            map.off("zoomend", handler);
        };
    }, [map, onViewportChange]);

    return null;
}

const StationMarker = memo(function StationMarker({ station, isSelected, onStationClick }) {
    const lat = Number(station.latitude);
    const lon = Number(station.longitude);
    const handleClick = useCallback(() => onStationClick?.(station.id), [station.id, onStationClick]);
    if (Number.isNaN(lat) || Number.isNaN(lon)) return null;
    return (
        <Marker
            position={[lat, lon]}
            icon={makeIcon(station.markerStatus ?? "DEFAULT", isSelected)}
            eventHandlers={{ click: handleClick }}
        >
            <Popup>
                <div className="pop-op">
                    {station.operatorName ?? "—"} · {String(station.id).slice(0, 8)}
                </div>
                <div className="pop-name">{station.name || "Bez nazwy"}</div>
                <div className="pop-addr">
                    {[station.addressLine, station.city].filter(Boolean).join(", ") || "Adres nieznany"}
                </div>
                <button className="pop-btn" onClick={handleClick}>
                    Szczegóły
                </button>
            </Popup>
        </Marker>
    );
});

const MapView = memo(function MapView({
    location,
    stations = [],
    selectedStationId,
    selectedStation,
    onStationClick,
    onViewportChange,
    createMode = false,
    onMapClickInCreateMode,
    createDraft,
}) {
    return (
        <MapContainer
            center={[52.1128, 19.21195]}
            zoom={7}
            maxBounds={outerBounds}
            maxBoundsViscosity={0.8}
            zoomControl={false}
            attributionControl={false}
            className={createMode ? "map-create-mode" : ""}
        >
            <TileLayer
                url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
            />
            <MapController location={location} selectedStation={selectedStation} />
            {onViewportChange && <BoundsWatcher onViewportChange={onViewportChange} />}
            {createMode && <CreateModeClickHandler onMapClick={onMapClickInCreateMode} />}

            {location && typeof location.lat === "number" && typeof location.lon === "number" && (
                <Marker position={[location.lat, location.lon]} icon={locationIcon}>
                    <Popup>Wybrana lokalizacja: {location.name}</Popup>
                </Marker>
            )}

            {stations.map((station) => (
                <StationMarker
                    key={station.id}
                    station={station}
                    isSelected={station.id === selectedStationId}
                    onStationClick={onStationClick}
                />
            ))}

            {createMode && createDraft && (
                <Marker
                    position={[createDraft.latitude, createDraft.longitude]}
                    icon={draftStationIcon}
                    interactive={false}
                />
            )}
        </MapContainer>
    );
});

export default MapView;
