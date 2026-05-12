import { useState } from "react";

function LoginModal({ open, onClose, onSubmit, authError }) {
    const [mode, setMode] = useState("login");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [displayName, setDisplayName] = useState("");

    if (!open) return null;

    const handleSubmit = (e) => {
        e.preventDefault();
        onSubmit({ mode, email, password, displayName });
    };

    return (
        <div className="modal-backdrop" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <h3>{mode === "login" ? "Logowanie" : "Rejestracja"}</h3>
                <form onSubmit={handleSubmit}>
                    {mode === "register" && (
                        <label>
                            Nazwa użytkownika
                            <input
                                type="text"
                                value={displayName}
                                onChange={(e) => setDisplayName(e.target.value)}
                                autoComplete="name"
                                required
                            />
                        </label>
                    )}
                    <label>
                        Email
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="email"
                            required
                        />
                    </label>
                    <label>
                        Hasło
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />
                    </label>
                    {authError && <p className="modal-error">{authError}</p>}
                    <div className="modal-actions">
                        <button type="button" className="action" onClick={onClose}>Anuluj</button>
                        <button type="submit" className="action primary">
                            {mode === "login" ? "Zaloguj" : "Załóż konto"}
                        </button>
                    </div>
                    <button
                        type="button"
                        className="modal-toggle"
                        onClick={() => setMode(mode === "login" ? "register" : "login")}
                    >
                        {mode === "login" ? "Nie masz konta? Zarejestruj się" : "Masz już konto? Zaloguj się"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default LoginModal;
