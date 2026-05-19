import { useState, useEffect, useMemo } from "react";
import ConnectorEditor from "./ConnectorEditor.jsx";
import { IconClose, IconHeart, IconPin, IconPlug } from "./Icons.jsx";

const OPERATIONAL_STATUS_OPTIONS = [
    { value: "WORKING", label: "Działa" },
    { value: "NOT_WORKING", label: "Nie działa" },
    { value: "BUSY", label: "Zajęta" },
    { value: "LIMITED", label: "Ograniczona" },
    { value: "UNKNOWN", label: "Nieznany" },
];

function formatOperationalStatus(status) {
    return (
        OPERATIONAL_STATUS_OPTIONS.find((o) => o.value === status)?.label ??
        status
    );
}

function maxConnectorPower(connectors = []) {
    let max = 0;
    for (const c of connectors) {
        const p = Number(c.powerKw ?? 0);
        if (p > max) max = p;
    }
    return max;
}

function markerStatusIsWorking(ms) {
    return ms === "WORKING" || ms === "AVAILABLE" || ms === "WORKING_UNCONFIRMED";
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
    isFavorited,
    favoritesLoading,
    onAddFavorite,
    onRemoveFavorite,
    stationEditMode,
    stationEditForm,
    stationEditLoading,
    stationEditMessage,
    onStartStationEdit,
    onCancelStationEdit,
    onSaveStationEdit,
    onStationEditChange,
    onSubmitReport,
    onSubmitConnectorReport,
    canManageStation = false,
    isStationAdmin = false,
}) {
    const [feedbackStatus, setFeedbackStatus] = useState("WORKING");
    const [feedbackComment, setFeedbackComment] = useState("");
    const [connectorStates, setConnectorStates] = useState({});

    const stationId = (details || station)?.id;
    useEffect(() => {
        setConnectorStates({});
    }, [stationId]);

    const handleSubmit = () => {
        onSubmitFeedback(feedbackStatus, feedbackComment);
        setFeedbackComment("");
        if (onSubmitReport && (feedbackStatus === "WORKING" || feedbackStatus === "NOT_WORKING")) {
            onSubmitReport(feedbackStatus);
        }
    };

    const setConnState = (connId, update) => {
        setConnectorStates((prev) => ({
            ...prev,
            [connId]: { ...prev[connId], ...update },
        }));
    };

    const handleConnectorSubmit = async (connector) => {
        const cs = connectorStates[connector.id] || {};
        const qty = connector.quantity ?? 1;
        const apiStatus = cs.status === "ZAJETE" ? "OCCUPIED" : cs.status;
        const occupiedCount = cs.status === "ZAJETE" ? cs.count : null;
        setConnState(connector.id, { loading: true });
        try {
            await onSubmitConnectorReport(connector.id, apiStatus, occupiedCount);
            setConnState(connector.id, { expanded: false, status: null, count: null, loading: false });
        } catch {
            setConnState(connector.id, { loading: false });
        }
    };

    const canSubmitConnector = (cs) => {
        if (!cs.status) return false;
        if (cs.status === "ZAJETE" && cs.count == null) return false;
        return true;
    };

    const data = details || station || {};
    const latest = data.latestStatus ?? {};
    const connectors = data.connectors ?? [];

    const stationOverride = station?.communityOverride;
    const stationNotWorking = stationOverride?.reportedStatus === "NOT_WORKING";

    const communityStats = useMemo(() => {
        if (stationNotWorking) {
            const total = connectors.reduce((sum, c) => sum + (c.quantity ?? 1), 0);
            return { availableCount: 0, occupiedCount: 0, outOfServiceCount: total, unknownCount: 0 };
        }
        const withStatus = connectors.filter((c) => c.communityStatus != null);
        if (withStatus.length === 0) return null;
        let available = 0, occupied = 0, outOfService = 0, unknown = 0;
        for (const c of connectors) {
            const qty = c.quantity ?? 1;
            if (c.communityStatus === "OCCUPIED") {
                const occ = c.reportedOccupiedCount ?? qty;
                occupied += occ;
                available += qty - occ;
            } else if (c.communityStatus === "WORKING") {
                available += qty;
            } else if (c.communityStatus === "NOT_WORKING") {
                outOfService += qty;
            } else {
                unknown += qty;
            }
        }
        return { availableCount: available, occupiedCount: occupied, outOfServiceCount: outOfService, unknownCount: unknown };
    }, [connectors, stationNotWorking]);

    const displayStats = communityStats ?? latest;
    const maxPower = maxConnectorPower(connectors);

    const operatorInitial = (data.operatorName ?? "?")[0]?.toUpperCase() ?? "?";

    return (
        <aside className={`detail${open ? " open" : ""}`}>
            <div className="detail-hero">
                <button
                    className="detail-close"
                    onClick={onClose}
                    aria-label="Zamknij"
                >
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
                        {[data.addressLine, data.city, data.country]
                            .filter(Boolean)
                            .join(", ") || "Adres nieznany"}
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
                {currentUser && (
                    <button
                        className="action primary"
                        disabled={favoritesLoading}
                        onClick={isFavorited ? onRemoveFavorite : onAddFavorite}
                    >
                        <IconHeart />
                        {isFavorited
                            ? "Usuń z ulubionych"
                            : "Dodaj do ulubionych"}
                    </button>
                )}
                {!currentUser && (
                    <button className="action primary" disabled>
                        <IconHeart />
                        Zaloguj, aby dodać do ulubionych
                    </button>
                )}
            </div>

            <div className="detail-body">
                {canManageStation && (
                    <div className="section admin-section">
                        <div className="section-title">
                            {isStationAdmin
                                ? "Administracja stacją"
                                : "Zarządzanie stacją"}
                        </div>
                        {!stationEditMode && (
                            <div className="station-admin-actions">
                                <button
                                    type="button"
                                    className="action primary"
                                    onClick={onStartStationEdit}
                                >
                                    Edytuj stację
                                </button>
                                <button
                                    type="button"
                                    className="action danger"
                                    onClick={onDeleteStation}
                                >
                                    Usuń stację
                                </button>
                            </div>
                        )}
                        {stationEditMode && stationEditForm && (
                            <div className="station-edit-section">
                                <div className="station-edit-grid">
                                    <label className="station-edit-field">
                                        Nazwa
                                        <input
                                            type="text"
                                            value={stationEditForm.name}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "name",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Operator
                                        <input
                                            type="text"
                                            value={stationEditForm.operatorName}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "operatorName",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Szerokość
                                        <input
                                            type="number"
                                            step="any"
                                            value={stationEditForm.latitude}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "latitude",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Długość
                                        <input
                                            type="number"
                                            step="any"
                                            value={stationEditForm.longitude}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "longitude",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Adres
                                        <input
                                            type="text"
                                            value={stationEditForm.addressLine}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "addressLine",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Miasto
                                        <input
                                            type="text"
                                            value={stationEditForm.city}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "city",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Kraj
                                        <input
                                            type="text"
                                            value={stationEditForm.country}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "country",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Godziny otwarcia
                                        <input
                                            type="text"
                                            value={stationEditForm.openingHours}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "openingHours",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <label className="station-edit-field">
                                        Typ dostępu
                                        <input
                                            type="text"
                                            value={stationEditForm.accessType}
                                            onChange={(e) =>
                                                onStationEditChange(
                                                    "accessType",
                                                    e.target.value,
                                                )
                                            }
                                        />
                                    </label>
                                    <div className="station-edit-status-label">Status stacji</div>
                                    <div className="station-edit-status-group">
                                        {[
                                            { value: "WORKING", label: "Działa" },
                                            { value: "OCCUPIED", label: "Zajęta" },
                                            { value: "NOT_WORKING", label: "Nie działa" },
                                        ].map(({ value, label }) => (
                                            <label key={value} className="station-edit-radio">
                                                <input
                                                    type="radio"
                                                    name="adminStatus"
                                                    value={value}
                                                    checked={stationEditForm.adminStatus === value}
                                                    onChange={() => onStationEditChange("adminStatus", value)}
                                                />
                                                {label}
                                            </label>
                                        ))}
                                    </div>
                                </div>
                                <ConnectorEditor
                                    connectors={stationEditForm.connectors ?? []}
                                    onChange={(connectors) =>
                                        onStationEditChange(
                                            "connectors",
                                            connectors,
                                        )
                                    }
                                />
                                <div className="station-edit-actions">
                                    <button
                                        type="button"
                                        className="action primary"
                                        disabled={stationEditLoading}
                                        onClick={onSaveStationEdit}
                                    >
                                        {stationEditLoading
                                            ? "Zapisywanie…"
                                            : "Zapisz zmiany"}
                                    </button>
                                    <button
                                        type="button"
                                        className="action"
                                        disabled={stationEditLoading}
                                        onClick={onCancelStationEdit}
                                    >
                                        Anuluj
                                    </button>
                                </div>
                                {stationEditMessage && (
                                    <p className="feedback-msg">
                                        {stationEditMessage}
                                    </p>
                                )}
                            </div>
                        )}
                    </div>
                )}

                {detailsLoading && (
                    <p className="empty-state">Ładowanie szczegółów…</p>
                )}
                {detailsError && (
                    <p className="empty-state" style={{ color: "#B91C1C" }}>
                        {detailsError}
                    </p>
                )}

                {/* AVAILABILITY GRID */}
                {(latest || communityStats) && (
                    <div className="section">
                        <div className="section-title">
                            Dostępność złączy
                            <span className="pill">{communityStats ? "społeczność" : "w tej chwili"}</span>
                        </div>
                        <div className="avail-grid">
                            <div className="avail-tile">
                                <div className="n">
                                    {displayStats.availableCount ?? 0}
                                </div>
                                <div className="l">
                                    <span
                                        className="d"
                                        style={{
                                            background:
                                                "var(--status-available)",
                                        }}
                                    />
                                    Wolne
                                </div>
                            </div>
                            <div className="avail-tile">
                                <div className="n">
                                    {displayStats.occupiedCount ?? 0}
                                </div>
                                <div className="l">
                                    <span
                                        className="d"
                                        style={{
                                            background:
                                                "var(--status-occupied)",
                                        }}
                                    />
                                    Zajęte
                                </div>
                            </div>
                            <div className="avail-tile">
                                <div className="n">
                                    {displayStats.outOfServiceCount ?? 0}
                                </div>
                                <div className="l">
                                    <span
                                        className="d"
                                        style={{
                                            background:
                                                "var(--status-disabled)",
                                        }}
                                    />
                                    Wyłącz.
                                </div>
                            </div>
                            <div className="avail-tile">
                                <div className="n">
                                    {displayStats.unknownCount ?? 0}
                                </div>
                                <div className="l">
                                    <span
                                        className="d"
                                        style={{
                                            background: "var(--status-default)",
                                        }}
                                    />
                                    Nieznane
                                </div>
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
                            {connectors.map((c) => {
                                const cs = connectorStates[c.id] || {};
                                const qty = c.quantity ?? 1;
                                return (
                                    <div key={c.id} className="conn-card">
                                        <div className="conn">
                                            <div className="conn-icon">
                                                <IconPlug />
                                            </div>
                                            <div className="conn-info">
                                                <div className="type">
                                                    {c.connectorType === "Unknown"
                                                        ? "Złącze"
                                                        : (c.connectorType ?? "Złącze")}
                                                    {qty > 1 ? ` × ${qty}` : ""}
                                                </div>
                                                <div className="desc">
                                                    {c.currentType ?? "—"}
                                                </div>
                                            </div>
                                            <div className="conn-power">
                                                {c.powerKw ?? "—"}
                                                <span className="u">kW</span>
                                            </div>
                                            <div className="conn-status-badge">
                                                {(stationNotWorking || c.communityStatus === "NOT_WORKING") && (
                                                    <span className="conn-badge not-working">Nie działa</span>
                                                )}
                                                {!stationNotWorking && c.communityStatus === "OCCUPIED" && (
                                                    <span className="conn-badge occupied">
                                                        {c.reportedOccupiedCount ?? "?"}/{qty} zajętych
                                                    </span>
                                                )}
                                                {!stationNotWorking && c.communityStatus === "WORKING" && (
                                                    <span className="conn-badge working">Działa</span>
                                                )}
                                            </div>
                                            {currentUser && (
                                                <button
                                                    className={`conn-toggle${cs.expanded ? " open" : ""}`}
                                                    onClick={() => setConnState(c.id, { expanded: !cs.expanded, status: null, count: null })}
                                                    aria-label="Zgłoś status złącza"
                                                >
                                                    Zgłoś {cs.expanded ? "▲" : "▼"}
                                                </button>
                                            )}
                                        </div>
                                        {cs.expanded && (
                                            <div className="conn-report">
                                                <div className="conn-report-chips">
                                                    {["WORKING", "NOT_WORKING", "ZAJETE"].map((st) => (
                                                        <button
                                                            key={st}
                                                            className={`conn-chip${cs.status === st ? " active" : ""}`}
                                                            disabled={cs.loading}
                                                            onClick={() => setConnState(c.id, { status: st, count: null })}
                                                        >
                                                            {st === "WORKING" ? "Działa" : st === "NOT_WORKING" ? "Nie działa" : "Zajęte"}
                                                        </button>
                                                    ))}
                                                </div>
                                                {cs.status === "ZAJETE" && (
                                                    <div className="conn-count-picker">
                                                        <span className="conn-count-label">ile zajętych?</span>
                                                        {Array.from({ length: qty + 1 }, (_, i) => i).map((n) => (
                                                            <button
                                                                key={n}
                                                                className={`conn-count-btn${cs.count === n ? " active" : ""}`}
                                                                disabled={cs.loading}
                                                                onClick={() => setConnState(c.id, { count: n })}
                                                            >
                                                                {n === 0 ? "0" : n}
                                                            </button>
                                                        ))}
                                                    </div>
                                                )}
                                                {canSubmitConnector(cs) && (
                                                    <button
                                                        className="conn-submit"
                                                        disabled={cs.loading}
                                                        onClick={() => handleConnectorSubmit(c)}
                                                    >
                                                        {cs.loading ? "Wysyłanie…" : "Wyślij"}
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* FEEDBACK FORM */}
                <div className="section">
                    <div className="section-title">Oceń stację</div>
                    {!currentUser && (
                        <p
                            className="empty-state"
                            style={{ padding: "10px 0", textAlign: "left" }}
                        >
                            Zaloguj się, aby zgłosić status stacji.
                        </p>
                    )}
                    {currentUser && (
                        <>
                            <div className="feedback-actions">
                                {["WORKING", "NOT_WORKING"].map(
                                    (st) => (
                                        <button
                                            key={st}
                                            className={`feedback-chip${feedbackStatus === st ? " active" : ""}`}
                                            onClick={() =>
                                                setFeedbackStatus(st)
                                            }
                                            disabled={feedbackSubmitting}
                                        >
                                            {formatOperationalStatus(st)}
                                        </button>
                                    ),
                                )}
                            </div>
                            <textarea
                                className="feedback-textarea"
                                placeholder="Opcjonalny komentarz…"
                                value={feedbackComment}
                                onChange={(e) =>
                                    setFeedbackComment(e.target.value)
                                }
                                rows={2}
                            />
                            <button
                                className="feedback-submit"
                                disabled={feedbackSubmitting}
                                onClick={handleSubmit}
                            >
                                {feedbackSubmitting
                                    ? "Zapisywanie…"
                                    : "Zapisz zgłoszenie"}
                            </button>
                            {feedbackActionMessage && (
                                <div className="feedback-msg">
                                    {feedbackActionMessage}
                                </div>
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
                    {feedbacksLoading && (
                        <p className="empty-state">Ładowanie…</p>
                    )}
                    {feedbacksError && (
                        <p className="empty-state" style={{ color: "#B91C1C" }}>
                            {feedbacksError}
                        </p>
                    )}
                    {!feedbacksLoading &&
                        !feedbacksError &&
                        (!feedbacks || feedbacks.length === 0) && (
                            <p
                                className="empty-state"
                                style={{ padding: "10px 0", textAlign: "left" }}
                            >
                                Brak opinii.
                            </p>
                        )}
                    <div className="review-list">
                        {feedbacks?.map((f) => (
                            <div key={f.id} className="review">
                                <div className="review-head">
                                    <div className="review-author">
                                        {f.userDisplayName ?? "Anonim"}
                                    </div>
                                    <div className="review-date">
                                        {f.createdAt
                                            ? new Date(
                                                  f.createdAt,
                                              ).toLocaleString("pl-PL")
                                            : ""}
                                    </div>
                                </div>
                                <div>
                                    <span
                                        className={`review-status ${f.operationalStatus}`}
                                    >
                                        {formatOperationalStatus(
                                            f.operationalStatus,
                                        )}
                                    </span>
                                </div>
                                {f.comment && (
                                    <p className="review-comment">
                                        {f.comment}
                                    </p>
                                )}
                                {(currentUser?.role === "ADMIN" ||
                                    String(f.userId) ===
                                        String(currentUser?.id)) && (
                                    <button
                                        className="review-delete"
                                        onClick={() => onDeleteFeedback(f.id)}
                                    >
                                        Usuń opinię
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>

                {/* ADMIN: DELETE STATION moved to admin section above */}
            </div>
        </aside>
    );
}

export default DetailPanel;
