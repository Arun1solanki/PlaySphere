# Build verification

## Checks completed in the generation environment

- `pom.xml` parsed as valid XML.
- `package.json` and TypeScript configuration parsed as valid JSON.
- Spring configuration files parsed as valid YAML.
- Shell launch scripts passed `bash -n` syntax checks.
- The project structure and relative frontend/backend source imports passed the bundled verification script.
- The React application source passed an offline TypeScript source check using a temporary declaration harness. That harness is **not** included in this project, so the authoritative type check is the real `npm install && npm run typecheck` command below.
- Java source was passed through the JDK parser; no Java syntax diagnostics were found before expected dependency-resolution errors.
- The old Jackson 2 databind imports were removed from the Spring Boot 4 code paths.
- No real Brevo, Razorpay, Cloudinary, database, or JWT secrets are included.

## Environment limitation

A complete Maven build and npm production build were not possible in the generation environment because external package repositories could not be resolved. Run the commands below on a machine with internet access:

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm install
npm run typecheck
npm run build
npm run dev
```

The first run downloads Maven/npm dependencies. Configure MySQL first and start from a clean terminal so the root `.env` file is loaded by the provided launcher scripts.
