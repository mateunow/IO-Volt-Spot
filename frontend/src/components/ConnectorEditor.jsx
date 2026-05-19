import { IconClose } from "./Icons.jsx";

function ConnectorEditor({ connectors = [], onChange }) {
    const handleAdd = () => {
        onChange([
            ...connectors,
            {
                connectorType: "Type2",
                currentType: "AC",
                powerKw: 22.0,
                quantity: 1,
            },
        ]);
    };

    const handleRemove = (index) => {
        const newConnectors = [...connectors];
        newConnectors.splice(index, 1);
        onChange(newConnectors);
    };

    const handleChange = (index, field, value) => {
        const newConnectors = [...connectors];
        newConnectors[index] = { ...newConnectors[index], [field]: value };
        onChange(newConnectors);
    };

    return (
        <div className="connector-editor">
            <div
                style={{
                    marginTop: "1rem",
                    marginBottom: "0.5rem",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                }}
            >
                <div className="section-title" style={{ margin: 0 }}>
                    Złącza
                </div>
                <button
                    type="button"
                    className="action"
                    onClick={handleAdd}
                    style={{ padding: "4px 8px", fontSize: "0.8rem" }}
                >
                    + Dodaj
                </button>
            </div>
            {connectors.length === 0 && (
                <p
                    className="empty-state"
                    style={{ margin: 0, paddingBottom: "10px", textAlign: "left" }}
                >
                    Brak złącz
                </p>
            )}
            {connectors.map((connector, i) => (
                <div
                    key={i}
                    style={{
                        display: "flex",
                        gap: "8px",
                        marginBottom: "8px",
                        alignItems: "center",
                        background: "var(--bg-secondary, #f9fafb)",
                        padding: "8px",
                        borderRadius: "8px",
                        border: "1px solid var(--border, #e5e7eb)"
                    }}
                >
                    <div
                        style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "8px",
                            flex: 1,
                        }}
                    >
                        <div style={{ display: "flex", gap: "8px" }}>
                            <label className="station-edit-field" style={{ flex: 1, margin: 0 }}>
                                Typ złącza
                                <input
                                    type="text"
                                    value={connector.connectorType ?? ""}
                                    onChange={(e) =>
                                        handleChange(i, "connectorType", e.target.value)
                                    }
                                    placeholder="np. CCS"
                                    style={{ marginTop: "4px" }}
                                />
                            </label>
                            <label className="station-edit-field" style={{ width: "80px", margin: 0 }}>
                                Prąd
                                <input
                                    type="text"
                                    value={connector.currentType ?? ""}
                                    onChange={(e) =>
                                        handleChange(i, "currentType", e.target.value)
                                    }
                                    placeholder="AC/DC"
                                    style={{ marginTop: "4px" }}
                                />
                            </label>
                        </div>
                        <div style={{ display: "flex", gap: "8px" }}>
                            <label className="station-edit-field" style={{ flex: 1, margin: 0 }}>
                                Moc (kW)
                                <input
                                    type="number"
                                    step="0.1"
                                    value={connector.powerKw ?? ""}
                                    onChange={(e) =>
                                        handleChange(
                                            i,
                                            "powerKw",
                                            parseFloat(e.target.value) || 0,
                                        )
                                    }
                                    style={{ marginTop: "4px" }}
                                />
                            </label>
                            <label className="station-edit-field" style={{ width: "80px", margin: 0 }}>
                                Ilość
                                <input
                                    type="number"
                                    min="1"
                                    value={connector.quantity ?? ""}
                                    onChange={(e) =>
                                        handleChange(
                                            i,
                                            "quantity",
                                            parseInt(e.target.value, 10) || 1,
                                        )
                                    }
                                    style={{ marginTop: "4px" }}
                                />
                            </label>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => handleRemove(i)}
                        className="action danger"
                        style={{ padding: "8px", height: "fit-content" }}
                        title="Usuń złącze"
                    >
                        <IconClose />
                    </button>
                </div>
            ))}
        </div>
    );
}

export default ConnectorEditor;
