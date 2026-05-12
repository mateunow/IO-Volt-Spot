import { IconPlus, IconMinus, IconLocate } from "./Icons.jsx";

function MapControls({ onZoomIn, onZoomOut, onLocate }) {
    return (
        <div className="map-controls">
            <div className="ctrl-group">
                <button className="ctrl-btn primary" onClick={onLocate} title="Moja lokalizacja">
                    <IconLocate />
                </button>
            </div>
            <div className="ctrl-group">
                <button className="ctrl-btn" onClick={onZoomIn} title="Przybliż">
                    <IconPlus />
                </button>
                <button className="ctrl-btn" onClick={onZoomOut} title="Oddal">
                    <IconMinus />
                </button>
            </div>
        </div>
    );
}

export default MapControls;
