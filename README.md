Aplikacja webowa do wyszukiwania stacji ładowania samochodów elektrycznych. Umożliwia przeglądanie stacji na interaktywnej mapie, sprawdzanie ich parametrów i statusu oraz wybór najlepszego punktu ładowania w pobliżu wybranej lokalizacji.

---

## Funkcje

- 🗺️ **Mapa interaktywna** – stacje ładowania wyświetlane jako markery na mapie OpenStreetMap (Leaflet), z ikonami zależnymi od statusu stacji
- 🔍 **Wyszukiwanie lokalizacji** – wpisz adres lub miasto, mapa przesuwa się do wybranego miejsca i pobiera pobliskie stacje w zadanym promieniu
- 📍 **Szczegóły stacji** – po kliknięciu markera wyświetlają się: adres, operator, godziny otwarcia, typy i moc złączy, status gniazd oraz odległość od wyszukiwanej lokalizacji
- 🔄 **Dane z OCM API** – stacje pobierane są z [Open Charge Map](https://openchargemap.org) i zapisywane lokalnie, dzięki czemu aplikacja działa też przy chwilowej niedostępności zewnętrznego API
- 👤 **Konta użytkowników** – rejestracja, logowanie, role (USER / OWNER / ADMIN)
- 💬 **Opinie o stacjach** – zalogowani użytkownicy mogą zgłaszać status stacji i dodawać komentarze

---

## Stack technologiczny

| Warstwa | Technologia |
|---|---|
| Frontend | React 19, Leaflet, Vite |
| Backend | Spring Boot 4, Java 25 |
| Baza danych | PostgreSQL 18 |
| Migracje | Flyway |
| Konteneryzacja | Docker, Docker Compose |
| Mapy | OpenStreetMap + Leaflet |
| Geokodowanie | Nominatim API |
| Dane o stacjach | Open Charge Map API |

---

## Uruchomienie przez Docker

Najszybszy sposób na uruchomienie całej aplikacji.

**Wymagania:** Docker Desktop

```bash
git clone https://github.com/TWOJ_LOGIN/IO-Volt-Spot.git
cd IO-Volt-Spot
docker compose up --build
```

Aplikacja będzie dostępna pod adresem **http://localhost:3000**

Plik `.env` w głównym katalogu zawiera domyślną konfigurację bazy danych:

```env
DB_USER=admin
DB_PASS=admin123
DB_NAME=volt_spot_db
```

---

## Uruchomienie lokalne (bez Dockera)

**Wymagania:** Java 25, Node.js 20+, PostgreSQL

### Backend

```bash
cd backend
./gradlew bootRun
```

Backend startuje na `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend startuje na `http://localhost:5173`. Requesty do `/api/*` są proxowane automatycznie do backendu przez konfigurację Vite.

---

## Architektura

```
IO-Volt-Spot/
├── backend/                  # Spring Boot REST API
│   └── src/main/java/pl/voltspot/backend/
│       ├── controller/       # Endpointy REST
│       ├── service/          # Logika biznesowa
│       ├── repository/       # Warstwa dostępu do danych (JPA)
│       ├── entity/           # Encje bazy danych
│       ├── dto/              # Obiekty transferu danych
│       ├── mapper/           # Mapowanie encji ↔ DTO
│       ├── client/           # Klient OCM API
│       └── auth/             # Autentykacja i autoryzacja
├── frontend/                 # React SPA
│   └── src/
│       ├── components/       # MapView, SearchBar
│       └── App.jsx           # Główny komponent
└── docker-compose.yml
```

W środowisku Docker nginx w kontenerze frontendu działa jako reverse proxy – przeglądarka komunikuje się tylko z jednym hostem, co eliminuje problemy z CORS.

---

## Ciekawostki techniczne

- **Wzór Haversine'a** – odległość od wybranej lokalizacji do stacji obliczana jest z uwzględnieniem kulistości Ziemi, nie zwykłą odległością euklidesową
- **Bounding-box + upsert** – filtrowanie stacji po promieniu wykonuje zapytanie SQL po prostokącie geograficznym, a dane z OCM są upsertowane do lokalnej bazy przy każdym wyszukiwaniu
- **Snapshoty statusów** – każde wyszukiwanie zapisuje aktualny status stacji, dzięki czemu marker na mapie zawsze odzwierciedla najświeższy znany stan

---

## Domyślne konta testowe

Po pierwszym uruchomieniu Flyway załaduje dane testowe:

| Email | Hasło | Rola |
|---|---|---|
| `user@voltspot.pl` | `admin123` | USER |
| `owner@voltspot.pl` | `admin123` | OWNER |
| `admin@voltspot.pl` | `admin123` | ADMIN |

---

## API – wybrane endpointy

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/api/stations` | Lista wszystkich stacji |
| GET | `/api/stations?lat=&lon=&radiusKm=` | Stacje w promieniu od lokalizacji |
| GET | `/api/stations/{id}` | Szczegóły stacji |
| GET | `/api/geocoding/search?query=` | Geokodowanie adresu |
| POST | `/api/auth/register` | Rejestracja |
| POST | `/api/auth/login` | Logowanie |
| POST | `/api/stations/{id}/feedback` | Dodanie opinii (wymaga logowania) |
