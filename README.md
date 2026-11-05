# Safe File Deleter

A small full-stack demo application for safely deleting files within a restricted root directory. The backend exposes a single REST endpoint that validates each requested path against an allowed root, refusing symlinks, directories, and paths that escape the sandbox. The frontend provides a simple textarea-based UI for submitting paths and viewing per-path results.

## Stack

Spring Boot 3.2 / Java 17 / React 18 / Vite 5

## Layout

```
.
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/demo/
│           │   ├── DemoApplication.java
│           │   └── controller/ApiController.java
│           └── resources/application.properties
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx
        ├── App.jsx
        └── App.css
```

## How to run

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. The allowed root defaults to `./allowed_root` (relative to the backend working directory) and can be overridden via the `app.allowed-root` property.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api` requests to the backend on port `8080`.

## API

### `POST /api/delete`

Deletes one or more files under the configured allowed root.

**Request body**

```json
{
  "paths": ["subdir/file1.txt", "subdir/file2.txt"]
}
```

Paths are interpreted as relative to `app.allowed-root`.

**Response**

```json
{
  "deleted": ["subdir/file1.txt"],
  "errors": [
    { "path": "subdir/file2.txt", "reason": "does not exist" }
  ]
}
```

Each requested path is processed independently. A path is rejected (and reported in `errors`) if it is empty, resolves outside the allowed root, does not exist, is a symbolic link, is not a regular file, or fails during deletion.

## Notes

- **Sandboxing is path-based only.** The allowed root is resolved and normalized, and each requested path must remain within it. There is no authentication or authorization layer — do not expose this service on an untrusted network.
- **Symlinks are refused**, not followed. This prevents escaping the sandbox via a symlink inside the allowed root.
- **No directory deletion.** Only regular files are deleted; directories are reported as `not a regular file`.
- **No CSRF protection or CORS configuration** is present. The frontend relies on Vite's dev proxy to avoid cross-origin requests. For production, serve the built frontend from the same origin as the backend or add explicit CORS configuration.
- **`allowed_root` must exist** before use; the controller does not create it.
- **Partial success is expected.** The endpoint returns HTTP 200 even when some paths fail; inspect the `errors` array in the response.
- **No persistence or audit log.** Deletions are not recorded beyond the response payload.
- **Frontend is a demo UI.** It does not list files, confirm deletions, or persist input between sessions.