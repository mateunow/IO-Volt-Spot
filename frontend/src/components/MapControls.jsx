import { IconPlus, IconMinus, IconLocate, IconClose, IconStation } from "./Icons.jsx";

function MapControls({
    onZoomIn,
    onZoomOut,
    onLocate,
    canCreateStation = false,
    createStationMode = false,
    onStartCreateStation,
    onCancelCreateStation,
}) {
    return (
        <div className="map-controls">
            {canCreateStation && (
                <div className="ctrl-group">
                    {createStationMode ? (
                        <button
                            className="ctrl-btn create-station-btn active"
                            onClick={onCancelCreateStation}
                            title="Anuluj dodawanie stacji"
                            type="button"
                        >
                            <IconClose />
                        </button>
                    ) : (
                        <button
                            className="ctrl-btn create-station-btn"
                            onClick={onStartCreateStation}
                            title="Dodaj nową stację"
                            type="button"
                        >
                            <IconStation />
                        </button>
                    )}
                </div>
            )}
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
