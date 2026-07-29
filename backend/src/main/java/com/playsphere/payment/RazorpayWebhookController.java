package com.playsphere.payment;

import com.playsphere.common.ApiResponse;
import com.playsphere.common.BusinessException;
import com.playsphere.config.AppProperties;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayWebhookController {
    private final PaymentService payments;
    private final AppProperties properties;
    private final JsonMapper jsonMapper;
    private final CurrentUserService currentUser;

    public RazorpayWebhookController(
            PaymentService payments,
            AppProperties properties,
            JsonMapper jsonMapper,
            CurrentUserService currentUser
    ) {
        this.payments = payments;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.currentUser = currentUser;
    }

    public record VerifyCheckoutRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature
    ) {}

    @PostMapping("/verify")
    public ApiResponse<Payment> verifyCheckout(
            @Valid @RequestBody VerifyCheckoutRequest request,
            Authentication authentication
    ) {
        AppProperties.Payment.Razorpay razorpay = requireRazorpayConfiguration();
        String expected = hmacSha256(
                request.razorpayOrderId() + "|" + request.razorpayPaymentId(),
                razorpay.keySecret()
        );
        if (!constantTimeEquals(expected, request.razorpaySignature())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid Razorpay payment signature");
        }
        String userId = currentUser.require(authentication).getId();
        Payment payment = payments.confirmGatewayPaymentForUser(
                userId,
                request.razorpayOrderId(),
                request.razorpayPaymentId()
        );
        return ApiResponse.ok("Payment verified", payment);
    }

    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String rawBody
    ) {
        AppProperties.Payment.Razorpay razorpay = requireRazorpayConfiguration();
        if (razorpay.webhookSecret() == null || razorpay.webhookSecret().isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Razorpay webhook is not configured");
        }
        if (!constantTimeEquals(hmacSha256(rawBody, razorpay.webhookSecret()), signature)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid Razorpay webhook signature");
        }

        try {
            JsonNode payload = jsonMapper.readTree(rawBody);
            String event = payload.at("/event").asString("");
            if ("payment.captured".equals(event) || "payment.authorized".equals(event)) {
                String orderId = payload.at("/payload/payment/entity/order_id").asString("");
                String paymentId = payload.at("/payload/payment/entity/id").asString("");
                if (!orderId.isBlank() && !paymentId.isBlank()) {
                    payments.confirmGatewayPayment(orderId, paymentId);
                }
            }
            return ApiResponse.ok("Webhook accepted");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid Razorpay webhook payload");
        }
    }

    private AppProperties.Payment.Razorpay requireRazorpayConfiguration() {
        AppProperties.Payment payment = properties.payment();
        AppProperties.Payment.Razorpay razorpay = payment == null ? null : payment.razorpay();
        if (razorpay == null || razorpay.keySecret() == null || razorpay.keySecret().isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Razorpay is not configured");
        }
        return razorpay;
    }

    private static String hmacSha256(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify Razorpay signature", exception);
        }
    }

    private static boolean constantTimeEquals(String first, String second) {
        if (first == null || second == null) return false;
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8)
        );
    }
}
