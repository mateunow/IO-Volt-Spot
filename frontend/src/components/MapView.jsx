import { MapContainer, TileLayer, Marker, Popup, useMap} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect } from "react";

const outerBounds = [
  [47.00, 8.07],
  [56.50, 30.09],
]


function MapView({location}) {
  return (
    <MapContainer
        center={[52.112, 19.211]}
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
            <Popup>{location.name}</Popup>
          </Marker>
          )}
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