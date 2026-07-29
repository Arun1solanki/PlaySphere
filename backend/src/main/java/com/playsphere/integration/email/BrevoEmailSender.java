package com.playsphere.integration.email;

import com.playsphere.config.AppProperties;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
public class BrevoEmailSender implements TransactionalEmailSender {
    private final RestClient restClient;
    private final AppProperties.Email.Brevo settings;

    public BrevoEmailSender(RestClient.Builder builder, AppProperties properties) {
        this.restClient = builder.baseUrl("https://api.brevo.com/v3").build();
        this.settings = properties.email().brevo();
    }

    @Override
    public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationUrl) {
        validateConfiguration();

        String safeName = escapeHtml(recipientName);
        String safeUrl = escapeHtml(verificationUrl);
        String html = """
                <html><body style="font-family:Arial,sans-serif;color:#102033">
                  <h2>Verify your PlaySphere account</h2>
                  <p>Hello %s,</p>
                  <p>Use the button below to confirm your email address.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 18px;background:#30d7f4;color:#06131f;text-decoration:none;border-radius:8px;font-weight:700">Verify email</a></p>
                  <p>This link expires soon. If the button does not work, open:</p>
                  <p>%s</p>
                </body></html>
                """.formatted(safeName, safeUrl, safeUrl);

        send(recipientEmail, recipientName, "Verify your PlaySphere email", html);
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl) {
        validateConfiguration();
        String safeName = escapeHtml(recipientName);
        String safeUrl = escapeHtml(resetUrl);
        String html = """
                <html><body style="font-family:Arial,sans-serif;color:#102033">
                  <h2>Reset your PlaySphere password</h2>
                  <p>Hello %s,</p>
                  <p>Use the button below to choose a new password.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 18px;background:#30d7f4;color:#06131f;text-decoration:none;border-radius:8px;font-weight:700">Reset password</a></p>
                  <p>If you did not request this, you can ignore this email.</p>
                  <p>%s</p>
                </body></html>
                """.formatted(safeName, safeUrl, safeUrl);

        send(recipientEmail, recipientName, "Reset your PlaySphere password", html);
    }

    private void send(String recipientEmail, String recipientName, String subject, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", settings.senderName(), "email", settings.senderEmail()),
                "to", List.of(Map.of("name", recipientName, "email", recipientEmail)),
                "subject", subject,
                "htmlContent", html
        );
        restClient.post()
                .uri("/smtp/email")
                .header("api-key", settings.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void validateConfiguration() {
        if (isBlank(settings.apiKey()) || isBlank(settings.senderEmail())) {
            throw new IllegalStateException("Brevo is selected but its API key or sender email is missing");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
