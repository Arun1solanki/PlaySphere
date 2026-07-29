# Third-party activation guide

The application is runnable without external accounts. Keep all secrets in the project-root `.env`; never place API secrets in React or commit `.env`.

## 1. Brevo transactional email

Default local mode:

```env
APP_EMAIL_PROVIDER=log
```

Verification and password-reset URLs are printed in the backend terminal.

For real email:

1. Create a Brevo account.
2. Verify a sender address or authenticate your domain.
3. Create an API key.
4. Set:

```env
APP_EMAIL_PROVIDER=brevo
BREVO_API_KEY=your_api_key
BREVO_SENDER_EMAIL=noreply@your-domain.example
BREVO_SENDER_NAME=PlaySphere
```

Restart the backend and register with a new email address.

## 2. Cloudinary image storage

Default local mode:

```env
APP_STORAGE_PROVIDER=local
```

Files are written under `backend/uploads` and served from `/uploads/**`.

For Cloudinary:

1. Create a Cloudinary account.
2. Copy the cloud name, API key, and API secret from the dashboard.
3. Set:

```env
APP_STORAGE_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

The Spring Boot backend signs uploads and deletions. The API secret is never sent to the browser.

## 3. Razorpay payments

Default local mode:

```env
APP_PAYMENT_PROVIDER=development
```

This adapter creates a fake order and marks it paid immediately for local workflow testing.

For Razorpay test mode:

1. Create and activate a Razorpay test account.
2. Generate a test Key ID and Key Secret.
3. Create a webhook secret.
4. In the project-root `.env`, set:

```env
APP_PAYMENT_PROVIDER=razorpay
RAZORPAY_KEY_ID=rzp_test_xxx
RAZORPAY_KEY_SECRET=your_test_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

5. Copy `frontend/.env.example` to `frontend/.env` and set the public Key ID only:

```env
VITE_API_BASE_URL=/api
VITE_RAZORPAY_KEY_ID=rzp_test_xxx
```

6. Configure this webhook endpoint in Razorpay:

```text
https://your-public-api.example/api/payments/razorpay/webhook
```

Select payment authorization/capture events. Localhost webhooks require a trusted HTTPS tunnel. The browser callback is also server-verified, but webhooks remain the reliable final reconciliation channel.

## 4. Maps and nearby turf discovery

The local build includes a lightweight OpenStreetMap/Nominatim adapter:

```env
APP_MAPS_PROVIDER=nominatim
NOMINATIM_USER_AGENT=PlaySphere-Local-Development-0.1-your-email@example.com
```

It supports explicit location search, reverse lookup from the browser's current location, map previews, and nearby approved turfs. Replace the placeholder contact in the User-Agent before use. For production traffic, use a managed maps/geocoding provider or an appropriately hosted service, then implement a provider adapter behind the same backend endpoints.

Indian phone format validation is active. Add an SMS provider later only when phone ownership verification becomes mandatory.
