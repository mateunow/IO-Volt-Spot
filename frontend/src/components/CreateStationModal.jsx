import { useState } from "react";
import ConnectorEditor from "./ConnectorEditor.jsx";

const EMPTY_FORM = {
    name: "",
    addressLine: "",
    city: "",
    country: "Polska",
    operatorName: "",
    openingHours: "",
    accessType: "",
    connectors: [
        {
            connectorType: "Type2",
            currentType: "AC",
            powerKw: 22.0,
            quantity: 1,
        },
    ],
};

function CreateStationModal({ open, draft, submitting, error, onSubmit, onClose }) {
    if (!open || !draft) return null;
    return (
        <CreateStationModalForm
            draft={draft}
            submitting={submitting}
            error={error}
            onSubmit={onSubmit}
            onClose={onClose}
        />
    );
}

function CreateStationModalForm({ draft, submitting, error, onSubmit, onClose }) {
    const [form, setForm] = useState(EMPTY_FORM);

    const handleChange = (field) => (event) => {
        const value = event.target.value;
        setForm((prev) => ({ ...prev, [field]: value }));
    };

    const handleConnectorsChange = (newConnectors) => {
        setForm((prev) => ({ ...prev, connectors: newConnectors }));
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        if (submitting) return;
        onSubmit(form);
    };

    const formatCoord = (value) =>
        typeof value === "number" ? value.toFixed(6) : "—";

    return (
        <div className="modal-backdrop" onClick={onClose}>
            <div
                className="modal modal-wide"
                onClick={(e) => e.stopPropagation()}
                style={{ maxHeight: "90vh", overflowY: "auto" }}
            >
                <h3>Nowa stacja</h3>
                <p className="modal-coords">
                    Współrzędne: <strong>{formatCoord(draft.latitude)}</strong>,{" "}
                    <strong>{formatCoord(draft.longitude)}</strong>
                </p>
                <form onSubmit={handleSubmit}>
                    <label>
                        Nazwa stacji *
                        <input
                            type="text"
                            value={form.name}
                            onChange={handleChange("name")}
                            maxLength={255}
                            required
                            autoFocus
                        />
                    </label>

                    <div className="create-station-grid">
                        <label>
                            Adres
                            <input
                                type="text"
                                value={form.addressLine}
                                onChange={handleChange("addressLine")}
                                maxLength={255}
                                placeholder="np. ul. Marszałkowska 1"
                            />
                        </label>
                        <label>
                            Miasto
                            <input
                                type="text"
                                value={form.city}
                                onChange={handleChange("city")}
                                maxLength={120}
                            />
                        </label>
                        <label>
                            Kraj
                            <input
                                type="text"
                                value={form.country}
                                onChange={handleChange("country")}
                                maxLength={120}
                            />
                        </label>
                        <label>
                            Operator
                            <input
                                type="text"
                                value={form.operatorName}
                                onChange={handleChange("operatorName")}
                                maxLength={255}
                            />
                        </label>
                        <label>
                            Godziny otwarcia
                            <input
                                type="text"
                                value={form.openingHours}
                                onChange={handleChange("openingHours")}
                                placeholder="np. 24/7 lub Pn-Pt 8-20"
                            />
                        </label>
                        <label>
                            Dostęp
                            <input
                                type="text"
                                value={form.accessType}
                                onChange={handleChange("accessType")}
                                placeholder="np. publiczny / prywatny"
                                maxLength={120}
                            />
                        </label>
                    </div>

                    <ConnectorEditor
                        connectors={form.connectors}
                        onChange={handleConnectorsChange}
                    />

                    {error && <p className="modal-error">{error}</p>}

                    <div className="modal-actions" style={{ marginTop: "1rem" }}>
                        <button
                            type="button"
                            className="action"
                            onClick={onClose}
                            disabled={submitting}
                        >
                            Anuluj
                        </button>
                        <button
                            type="submit"
                            className="action primary"
                            disabled={submitting}
                        >
                            {submitting ? "Dodawanie..." : "Dodaj stację"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default CreateStationModal;
