package com.playsphere.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements TransactionalEmailSender {
    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationUrl) {
        log.info("EMAIL LOG MODE | type=verification | to={} | name={} | actionUrl={}",
                recipientEmail, recipientName, verificationUrl);
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl) {
        log.info("EMAIL LOG MODE | type=password-reset | to={} | name={} | actionUrl={}",
                recipientEmail, recipientName, resetUrl);
    }
}
