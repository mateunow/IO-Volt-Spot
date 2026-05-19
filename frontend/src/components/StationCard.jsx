import { memo, useCallback } from "react";
import { statusColor } from "./markerIcons.js";
import { IconStation } from "./Icons.jsx";

function maxConnectorPower(connectors = []) {
    let max = 0;
    for (const c of connectors) {
        const p = Number(c.powerKw ?? 0);
        if (p > max) max = p;
    }
    return max;
}

function availSegments(available, total) {
    const segs = [];
    for (let i = 0; i < Math.min(total, 5); i++) {
        if (i < available) segs.push("on");
        else if (i < total) segs.push("busy");
        else segs.push("off");
    }
    while (segs.length < 5) segs.push("off");
    return segs;
}

const StationCard = memo(function StationCard({ station, isActive, onSelect, distanceKm }) {
    const c = statusColor(station.markerStatus);
    const status = station.latestStatus ?? {};
    const total =
        (status.availableCount ?? 0) +
        (status.occupiedCount ?? 0) +
        (status.reservedCount ?? 0) +
        (status.outOfServiceCount ?? 0) +
        (status.unknownCount ?? 0);
    const available = status.availableCount ?? 0;
    const power = maxConnectorPower(station.connectors);
    const handleClick = useCallback(() => onSelect?.(station.id), [onSelect, station.id]);

    return (
        <div className={`station-card${isActive ? " active" : ""}`} onClick={handleClick}>
            <div className="marker-mini" style={{ background: c.fill }}>
                <IconStation />
            </div>
            <div className="info">
                <div className="name">{station.name || "Bez nazwy"}</div>
                <div className="meta">
                    <span>{station.operatorName || "—"}</span>
                    <span className="sep">·</span>
                    <span>{station.city || ""}</span>
                </div>
            </div>
            <div className="right">
                {distanceKm != null && (
                    <div className="distance">{distanceKm.toFixed(1)} km</div>
                )}
                {power > 0 && <div className="power">{power} kW</div>}
            </div>
            {total > 0 && (
                <div className="avail-bar">
                    {availSegments(available, total).map((k, i) => (
                        <div key={i} className={`avail-seg ${k}`} />
                    ))}
                </div>
            )}
        </div>
    );
});

export default StationCard;
