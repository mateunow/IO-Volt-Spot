import { MapContainer, TileLayer, Marker, Popup, useMap} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect } from "react";
import L from "leaflet";
import defaultStationIcon from "./images/default_station.png";
import disabledStationIcon from "./images/disabled_station.png";
import occupiedStationIcon from "./images/occupied_station.png";
import availableStationIcon from "./images/working_station.png";

const outerBounds = [
  [47.00, 8.07],
  [56.50, 30.09],
]

const createIcon = (iconUrl) =>
  new L.Icon({
    iconUrl,
    iconSize: [38, 38],
    iconAnchor: [25, 30],
    popupAnchor: [-5, -30],
  });


function MapView({ location, stations = [], selectedStationId, onStationClick }) {
  const defaultIcon = createIcon(defaultStationIcon);
  const selectedIcon = createIcon(availableStationIcon);

  return (
    <MapContainer
        center={[52.1128, 19.21195]}
        zoom={7}
        className="map"
        maxBounds={outerBounds} 
        maxBoundsViscosity={0.8}  
        >
        <TileLayer
            attribution="&copy; OpenStreetMap contributors"
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
          <MapController location={location} />

          {location && location.lat && location.lon && (
          <Marker position={[location.lat, location.lon]}>
            <Popup>Wybrana lokalizacja: {location.name}</Popup>
          </Marker>
          )}

          {stations.map((station) => {
            const lat = Number(station.latitude);
            const lon = Number(station.longitude);
            if (Number.isNaN(lat) || Number.isNaN(lon)) {
              return null;
            }

            const markerIcon = station.id === selectedStationId ? selectedIcon : defaultIcon;

            return (
              <Marker
                key={station.id}
                position={[lat, lon]}
                icon={markerIcon}
                eventHandlers={{
                  click: () => onStationClick?.(station.id),
                }}
              >
                <Popup>
                  <strong>{station.name || "Bez nazwy"}</strong>
                  <br />
                  {station.city || "Miasto nieznane"}
                  <br />
                  Operator: {station.operatorName || "Nieznany"}
                </Popup>
              </Marker>
            );
          })}
    </MapContainer>
  );
}

export default MapView;

function MapController({ location }) {
  const map = useMap();
   useEffect(() => {
    if (
      location &&
      typeof location.lat === "number" &&
      typeof location.lon === "number"
    ) {
      map.flyTo([location.lat, location.lon], 12);
    }
  }, [location, map]);
}