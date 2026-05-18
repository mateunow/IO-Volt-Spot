// Helpers for marker icons and status colors
import L from "leaflet";

export const STATUS_COLORS = {
    WORKING:              { fill: "#22C55E", stroke: "#15803D", selectedFill: "#15803D", selectedStroke: "#14532D", label: "Dostępna" },
    OCCUPIED:             { fill: "#F97316", stroke: "#C2410C", selectedFill: "#C2410C", selectedStroke: "#7C2D12", label: "Zajęta" },
    DISABLED:             { fill: "#F9A8B4", stroke: "#BE185D", selectedFill: "#BE185D", selectedStroke: "#831843", label: "Wyłączona" },
    DEFAULT:              { fill: "#0F1115", stroke: "#000000", selectedFill: "#374151", selectedStroke: "#111827", label: "Nieznany" },
    WORKING_UNCONFIRMED:  { fill: "#86EFAC", stroke: "#15803D", selectedFill: "#4ADE80", selectedStroke: "#15803D", label: "Dostępna (niezatwierdzono)" },
    DISABLED_UNCONFIRMED: { fill: "#FECDD3", stroke: "#BE185D", selectedFill: "#FDA4AF", selectedStroke: "#9D174D", label: "Wyłączona (niezatwierdzono)" },
};

export function statusColor(markerStatus) {
    return STATUS_COLORS[markerStatus] ?? STATUS_COLORS.DEFAULT;
}

// Generates an SVG pin marker matching the design.
function pinSvg(fillColor, strokeColor, isSelected = false) {
    const filterId = `sh${fillColor.replace(/[^a-z0-9]/gi, "")}${isSelected ? "sel" : ""}`;
    const scale = isSelected ? 1.12 : 1;
    return `
    <svg width="${38 * scale}" height="${46 * scale}" viewBox="0 0 38 46" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <filter id="${filterId}" x="-20%" y="-10%" width="140%" height="140%">
          <feDropShadow dx="0" dy="2" stdDeviation="${isSelected ? 2.5 : 1.5}" flood-opacity="${isSelected ? 0.5 : 0.35}"/>
        </filter>
      </defs>
      <path d="M19 1C9.6 1 2 8.6 2 18c0 11.5 14.8 25.8 15.5 26.4a2.2 2.2 0 0 0 3 0C21.2 43.8 36 29.5 36 18 36 8.6 28.4 1 19 1z"
            fill="${fillColor}" stroke="${strokeColor}" stroke-width="${isSelected ? 2 : 1.2}" filter="url(#${filterId})"/>
      <g transform="translate(19,18)">
        <rect x="-6.5" y="-7" width="10" height="14" rx="1.6" fill="none" stroke="#fff" stroke-width="1.6"/>
        <line x1="-4" y1="-4.5" x2="1" y2="-4.5" stroke="#fff" stroke-width="1.4" stroke-linecap="round"/>
        <line x1="-4" y1="-2" x2="1" y2="-2" stroke="#fff" stroke-width="1.4" stroke-linecap="round"/>
        <path d="M-1 1 L-3 4 L-0.5 4 L-2 7 L1 3 L-1.5 3 Z" fill="#fff"/>
        <path d="M3.5 -5 h2 a1 1 0 0 1 1 1 v5 a2 2 0 0 1 -2 2 h-1" fill="none" stroke="#fff" stroke-width="1.3" stroke-linecap="round"/>
      </g>
    </svg>
  `;
}

const _iconCache = new Map();

export function makeIcon(markerStatus, isSelected = false) {
    const key = `${markerStatus ?? "DEFAULT"}-${isSelected}`;
    if (_iconCache.has(key)) return _iconCache.get(key);
    const c = statusColor(markerStatus);
    const fill   = isSelected ? (c.selectedFill   ?? c.fill)   : c.fill;
    const stroke = isSelected ? (c.selectedStroke ?? c.stroke) : c.stroke;
    const size = isSelected ? [42, 52] : [38, 46];
    const anchor = isSelected ? [21, 50] : [19, 44];
    const icon = L.divIcon({
        className: "ev-marker",
        html: pinSvg(fill, stroke, isSelected),
        iconSize: size,
        iconAnchor: anchor,
        popupAnchor: [0, -38],
    });
    _iconCache.set(key, icon);
    return icon;
}

export const locationIcon = L.divIcon({
    className: "ev-marker",
    html: `
    <svg width="32" height="42" viewBox="0 0 32 42" xmlns="http://www.w3.org/2000/svg">
      <path d="M16 1C8 1 2 7 2 15c0 10 12 24 13 24.5a1.6 1.6 0 0 0 2 0C18 39 30 25 30 15 30 7 24 1 16 1z"
            fill="oklch(0.65 0.18 250)" stroke="#1E3A8A" stroke-width="1.5"/>
      <circle cx="16" cy="15" r="5" fill="#fff"/>
    </svg>`,
    iconSize: [32, 42],
    iconAnchor: [16, 40],
});
