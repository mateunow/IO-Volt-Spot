import { STATUS_COLORS } from "./markerIcons.js";

export function Legend() {
    return (
        <div className="legend">
            <div className="legend-title">Legenda</div>
            <div className="legend-grid">
                {Object.entries(STATUS_COLORS).map(([key, val]) => (
                    <div key={key} className="legend-item">
                        <span className="legend-dot" style={{ background: val.fill }} />
                        {val.label}
                    </div>
                ))}
            </div>
        </div>
    );
}

export function Stats({ stations }) {
    let available = 0, occupied = 0, total = 0;
    for (const s of stations) {
        const ls = s.latestStatus ?? {};
        available += ls.availableCount ?? 0;
        occupied  += ls.occupiedCount  ?? 0;
        total += (ls.availableCount ?? 0)
            + (ls.occupiedCount ?? 0)
            + (ls.reservedCount ?? 0)
            + (ls.outOfServiceCount ?? 0)
            + (ls.unknownCount ?? 0);
    }
    return (
        <div className="stats">
            <div className="stat">
                <div className="stat-val">{available}<span className="unit">/{total || "-"}</span></div>
                <div className="stat-lbl">Dostępne</div>
            </div>
            <div className="stat">
                <div className="stat-val">{occupied}<span className="unit">/{total || "-"}</span></div>
                <div className="stat-lbl">Zajęte</div>
            </div>
        </div>
    );
}
