package com.example.backend.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.backend.exception.BusinessException;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WechatPayGatewayTests {

    @Test
    void successfulTransactionBecomesVerifiedCallbackInYuan() {
        Transaction transaction = transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);

        VerifiedPaymentCallback callback =
                WechatPayGateway.toVerifiedCallback(transaction, "body", "app-1", "mch-1");

        assertEquals("PAY1", callback.paymentNo());
        assertEquals("WX1", callback.providerTransactionNo());
        assertEquals(new BigDecimal("12.34"), callback.amount());
        assertEquals("CNY", callback.currency());
        assertEquals(true, callback.paid());
        assertEquals(71, callback.callbackDigest().length());
    }

    @Test
    void nonSuccessTransactionIsVerifiedButNotPostedAsPaid() {
        Transaction transaction = transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.USERPAYING);

        VerifiedPaymentCallback callback =
                WechatPayGateway.toVerifiedCallback(transaction, "body", "app-1", "mch-1");

        assertFalse(callback.paid());
    }

    @Test
    void mismatchedMerchantIsRejected() {
        Transaction transaction = transaction();
        transaction.setMchid("other-mch");

        assertThrows(
                BusinessException.class,
                () -> WechatPayGateway.toVerifiedCallback(transaction, "body", "app-1", "mch-1"));
    }

    @Test
    void notPayQueryRemainsPendingWithoutTransactionId() {
        Transaction transaction = transaction();
        transaction.setTransactionId(null);
        transaction.setTradeState(Transaction.TradeStateEnum.NOTPAY);

        PaymentQueryResult result = WechatPayGateway.toQueryResult(transaction, "app-1", "mch-1");

        assertEquals(ChannelPaymentStatus.PENDING, result.status());
    }

    @Test
    void successfulQueryRequiresTransactionId() {
        Transaction transaction = transaction();
        transaction.setTransactionId(null);
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);

        assertThrows(
                BusinessException.class,
                () -> WechatPayGateway.toQueryResult(transaction, "app-1", "mch-1"));
    }

    private Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.setAppid("app-1");
        transaction.setMchid("mch-1");
        transaction.setOutTradeNo("PAY1");
        transaction.setTransactionId("WX1");
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(1234);
        amount.setCurrency("CNY");
        transaction.setAmount(amount);
        return transaction;
    }
}
