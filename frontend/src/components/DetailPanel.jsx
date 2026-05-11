import { useState } from "react";
import { IconClose, IconHeart, IconPin, IconPlug } from "./Icons.jsx";

const OPERATIONAL_STATUS_OPTIONS = [
    { value: "WORKING", label: "Działa" },
    { value: "NOT_WORKING", label: "Nie działa" },
    { value: "BUSY", label: "Zajęta" },
    { value: "LIMITED", label: "Ograniczona" },
    { value: "UNKNOWN", label: "Nieznany" },
];

function formatOperationalStatus(status) {
    return OPERATIONAL_STATUS_OPTIONS.find((o) => o.value === status)?.label ?? status;
}

function maxConnectorPower(connectors = []) {
    let max = 0;
    for (const c of connectors) {
        const p = Number(c.powerKw ?? 0);
        if (p > max) max = p;
    }
    return max;
}

function DetailPanel({
    open,
    station,
    details,
    detailsLoading,
    detailsError,
    feedbacks,
    feedbacksLoading,
    feedbacksError,
    distanceKm,
    currentUser,
    onClose,
    onSubmitFeedback,
    feedbackSubmitting,
    feedbackActionMessage,
    onDeleteFeedback,
    onDeleteStation,
}) {
    const [feedbackStatus, setFeedbackStatus] = useState("WORKING");
    const [feedbackComment, setFeedbackComment] = useState("");

    const handleSubmit = () => {
        onSubmitFeedback(feedbackStatus, feedbackComment);
        setFeedbackComment("");
    };

    const data = details || station || {};
    const latest = data.latestStatus ?? {};
    const connectors = data.connectors ?? [];
    const maxPower = maxConnectorPower(connectors);

    const operatorInitial = (data.operatorName ?? "?")[0]?.toUpperCase() ?? "?";

    return (
        <aside className={`detail${open ? " open" : ""}`}>
            <div className="detail-hero">
                <button className="detail-close" onClick={onClose} aria-label="Zamknij">
                    <IconClose />
                </button>

                <div className="detail-op">
                    <div className="logo">{operatorInitial}</div>
                    <span>{data.operatorName ?? "Operator nieznany"}</span>
                    {data.id != null && (
                        <>
                            <span style={{ opacity: 0.5 }}>·</span>
                            <span>{String(data.id).slice(0, 8)}</span>
                        </>
                    )}
                </div>

                <h2 className="detail-name">{data.name ?? "Bez nazwy"}</h2>

                <div className="detail-addr">
                    <IconPin />
                    <span>
                        {[data.addressLine, data.city, data.country].filter(Boolean).join(", ") || "Adres nieznany"}
                    </span>
                </div>

                <div className="detail-status-row">
                    {data.openingHours && (
                        <div className="status-pill open">
                            <span className="dot" />
                            <span>{data.openingHours}</span>
                        </div>
                    )}
                    {distanceKm != null && (
                        <div className="status-pill neutral">
                            <IconPin />
                            {distanceKm.toFixed(1)} km
                        </div>
                    )}
                    {maxPower > 0 && (
                        <div className="status-pill neutral">
                            <IconPlug />
                            do {maxPower} kW
                        </div>
                    )}
                </div>
            </div>

            <div className="detail-actions">
                <button className="action primary">
                    <IconHeart />
                    Zapisz
                </button>
            </div>

            <div className="detail-body">
                {detailsLoading && <p className="empty-state">Ładowanie szczegółów…</p>}
                {detailsError && <p className="empty-state" style={{ color: "#B91C1C" }}>{detailsError}</p>}

                {/* AVAILABILITY GRID */}
                {latest && (
                    <div className="section">
                        <div className="section-title">
                            Dostępność złączy
                            <span className="pill">w tej chwili</span>
                        </div>
                        <div className="avail-grid">
                            <div className="avail-tile">
                                <div className="n">{latest.availableCount ?? 0}</div>
                                <div className="l"><span className="d" style={{ background: "var(--status-available)" }} />Wolne</div>
                            </div>
                            <div className="avail-tile">
                                <div className="n">{latest.occupiedCount ?? 0}</div>
                                <div className="l"><span className="d" style={{ background: "var(--status-occupied)" }} />Zajęte</div>
                            </div>
                            <div className="avail-tile">
                                <div className="n">{latest.outOfServiceCount ?? 0}</div>
                                <div className="l"><span className="d" style={{ background: "var(--status-disabled)" }} />Wyłącz.</div>
                            </div>
                            <div className="avail-tile">
                                <div className="n">{latest.unknownCount ?? 0}</div>
                                <div className="l"><span className="d" style={{ background: "var(--status-default)" }} />Nieznane</div>
                            </div>
                        </div>
                    </div>
                )}

                {/* CONNECTORS */}
                {connectors.length > 0 && (
                    <div className="section">
                        <div className="section-title">
                            Złącza
                            <span className="pill">{connectors.length}</span>
                        </div>
                        <div className="connector-list">
                            {connectors.map((c) => (
                                <div key={c.id} className="conn">
                                    <div className="conn-icon"><IconPlug /></div>
                                    <div className="conn-info">
                                        <div className="type">
                                            {c.connectorType === "Unknown" ? "Złącze" : (c.connectorType ?? "Złącze")}
                                            {c.quantity > 1 ? ` × ${c.quantity}` : ""}
                                        </div>
                                        <div className="desc">{c.currentType ?? "—"}</div>
                                    </div>
                                    <div className="conn-power">
                                        {c.powerKw ?? "—"}<span className="u">kW</span>
                                    </div>
                                    <div className="conn-status" style={{ background: "var(--status-available)" }} />
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* FEEDBACK FORM */}
                <div className="section">
                    <div className="section-title">Zgłoś status</div>
                    {!currentUser && (
                        <p className="empty-state" style={{ padding: "10px 0", textAlign: "left" }}>
                            Zaloguj się, aby zgłosić status stacji.
                        </p>
                    )}
                    {currentUser && (
                        <>
                            <div className="feedback-actions">
                                {["WORKING", "NOT_WORKING", "BUSY"].map((st) => (
                                    <button
                                        key={st}
                                        className={`feedback-chip${feedbackStatus === st ? " active" : ""}`}
                                        onClick={() => setFeedbackStatus(st)}
                                        disabled={feedbackSubmitting}
                                    >
                                        {formatOperationalStatus(st)}
                                    </button>
                                ))}
                            </div>
                            <textarea
                                className="feedback-textarea"
                                placeholder="Opcjonalny komentarz…"
                                value={feedbackComment}
                                onChange={(e) => setFeedbackComment(e.target.value)}
                                rows={2}
                            />
                            <button
                                className="feedback-submit"
                                disabled={feedbackSubmitting}
                                onClick={handleSubmit}
                            >
                                {feedbackSubmitting ? "Zapisywanie…" : "Zapisz zgłoszenie"}
                            </button>
                            {feedbackActionMessage && (
                                <div className="feedback-msg">{feedbackActionMessage}</div>
                            )}
                        </>
                    )}
                </div>

                {/* REVIEWS */}
                <div className="section">
                    <div className="section-title">
                        Opinie społeczności
                        <span className="pill">{feedbacks?.length ?? 0}</span>
                    </div>
                    {feedbacksLoading && <p className="empty-state">Ładowanie…</p>}
                    {feedbacksError && <p className="empty-state" style={{ color: "#B91C1C" }}>{feedbacksError}</p>}
                    {!feedbacksLoading && !feedbacksError && (!feedbacks || feedbacks.length === 0) && (
                        <p className="empty-state" style={{ padding: "10px 0", textAlign: "left" }}>
                            Brak opinii.
                        </p>
                    )}
                    <div className="review-list">
                        {feedbacks?.map((f) => (
                            <div key={f.id} className="review">
                                <div className="review-head">
                                    <div className="review-author">{f.userDisplayName ?? "Anonim"}</div>
                                    <div className="review-date">
                                        {f.createdAt ? new Date(f.createdAt).toLocaleString("pl-PL") : ""}
                                    </div>
                                </div>
                                <div>
                                    <span className={`review-status ${f.operationalStatus}`}>
                                        {formatOperationalStatus(f.operationalStatus)}
                                    </span>
                                </div>
                                {f.comment && <p className="review-comment">{f.comment}</p>}
                                {currentUser?.role === "ADMIN" && (
                                    <button className="review-delete" onClick={() => onDeleteFeedback(f.id)}>
                                        Usuń opinię
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>

                {/* ADMIN: DELETE STATION */}
                {currentUser?.role === "ADMIN" && (
                    <div className="section">
                        <button className="action danger" style={{ width: "100%" }} onClick={onDeleteStation}>
                            Usuń stację (admin)
                        </button>
                    </div>
                )}
            </div>
        </aside>
    );
}

export default DetailPanel;
