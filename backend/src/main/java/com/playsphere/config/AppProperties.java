package com.playsphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Jwt jwt,
        Session session,
        Email email,
        Payment payment,
        Storage storage,
        SeedAdmin seedAdmin
) {
    public record Jwt(String secretBase64, long accessMinutes, long refreshDays) {}
    public record Session(String cookieName, boolean secureCookie, String sameSite, long regularLoginHours,
                          long playerIdleMinutes, long organizerIdleMinutes, long turfOwnerIdleMinutes,
                          long adminIdleMinutes, long superAdminIdleMinutes) {}
    public record Email(String provider, long verificationMinutes, long passwordResetMinutes, long resendCooldownSeconds, Brevo brevo) {
        public record Brevo(String apiKey, String senderEmail, String senderName) {}
    }
    public record Payment(String provider, Razorpay razorpay) {
        public record Razorpay(String keyId, String keySecret, String webhookSecret) {}
    }
    public record Storage(String provider, Cloudinary cloudinary) {
        public record Cloudinary(String cloudName, String apiKey, String apiSecret) {}
    }
    public record SeedAdmin(boolean enabled, String email, String password) {}
}
