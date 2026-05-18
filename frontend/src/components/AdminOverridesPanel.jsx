const STATUS_LABEL = { WORKING: "Działa", NOT_WORKING: "Nie działa" };
const STATE_LABEL  = { PENDING: "Oczekuje", CONFIRMED: "Potwierdzone", REJECTED: "Odrzucone", EXPIRED: "Wygasłe" };

function formatDate(iso) {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("pl-PL", { dateStyle: "short", timeStyle: "short" });
}

export default function AdminOverridesPanel({ overrides, onConfirm, onReject, loading }) {
    if (loading) return <div className="admin-overrides-empty">Ładowanie zgłoszeń…</div>;
    if (!overrides || overrides.length === 0)
        return <div className="admin-overrides-empty">Brak oczekujących zgłoszeń.</div>;

    return (
        <div className="admin-overrides-list">
            {overrides.map((o) => (
                <div key={o.id} className="admin-override-card">
                    <div className="override-station">
                        <strong>{o.stationName}</strong>
                        {o.stationCity && <span className="override-city">{o.stationCity}</span>}
                    </div>
                    <div className="override-meta">
                        <span className={`override-status ${o.reportedStatus === "NOT_WORKING" ? "bad" : "good"}`}>
                            {STATUS_LABEL[o.reportedStatus] ?? o.reportedStatus}
                        </span>
                        <span className="override-counts">
                            ✓ {o.workingCount} działa · ✗ {o.notWorkingCount} nie działa
                        </span>
                        <span className="override-date">od {formatDate(o.createdAt)}</span>
                    </div>
                    <div className="override-actions">
                        <button
                            className="action primary small"
                            onClick={() => onConfirm(o.id)}
                        >
                            Zatwierdź
                        </button>
                        <button
                            className="action danger small"
                            onClick={() => onReject(o.id)}
                        >
                            Odrzuć
                        </button>
                    </div>
                </div>
            ))}
        </div>
    );
}
