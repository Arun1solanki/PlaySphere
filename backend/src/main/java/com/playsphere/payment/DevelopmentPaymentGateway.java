package com.playsphere.payment;import com.playsphere.common.Ids;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(prefix="app.payment",name="provider",havingValue="development",matchIfMissing=true)
public class DevelopmentPaymentGateway implements PaymentGateway{public GatewayOrder createOrder(Payment p){return new GatewayOrder("DEVELOPMENT","dev_order_"+Ids.uuid(),true,"dev_payment_"+Ids.uuid());}public void refund(Payment p,java.math.BigDecimal amount){}}
