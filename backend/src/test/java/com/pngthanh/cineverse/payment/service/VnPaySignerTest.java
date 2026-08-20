package com.pngthanh.cineverse.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VnPaySignerTest {
    @Test
    void signedDataIsSortedAndUrlEncoded() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_TxnRef", "CV 10");
        values.put("vnp_Amount", "1000000");

        assertEquals("vnp_Amount=1000000&vnp_TxnRef=CV+10", VnPaySigner.buildSignedData(values));
    }

    @Test
    void hmacChangesWhenPayloadChanges() {
        String first = VnPaySigner.hmacSha512("secret", "a=1");
        String second = VnPaySigner.hmacSha512("secret", "a=2");
        assertNotEquals(first, second);
    }
}
