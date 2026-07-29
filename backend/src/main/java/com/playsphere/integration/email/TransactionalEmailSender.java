package com.playsphere.integration.email;

public interface TransactionalEmailSender {
    void sendVerificationEmail(String recipientEmail, String recipientName, String verificationUrl);
    void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl);
}
