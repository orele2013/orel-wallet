package com.orel.wallet.security

import com.orel.wallet.domain.PaymentToken
import org.junit.Assert.*
import org.junit.Test

class OpaqueTokenCodecTest {
    @Test fun tokenReferencesRoundTripWithoutBankCredentials() {
        val token = PaymentToken("card-token", "demo-provider", "ref_demo_opaque_123", 100, 200)
        assertEquals(token, OpaqueTokenCodec.decode(OpaqueTokenCodec.encode(token)))
    }
    @Test fun bankNumbersAndMalformedTokensAreRejected() {
        for (reference in listOf("4111111111111111", "123", "", "raw PAN 4111111111111111")) {
            try {
                OpaqueTokenCodec.encode(PaymentToken("card-token", "issuer", reference, 100))
                fail("Raw card data must not be serialized")
            } catch (_: IllegalArgumentException) { }
        }
        try {
            OpaqueTokenCodec.encode(PaymentToken("token", "issuer", "ref_demo_opaque_123", 200, 100))
            fail("Expired-before-created token must not be serialized")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun trailingPayloadCannotBeSilentlyAccepted() {
        val encoded = OpaqueTokenCodec.encode(PaymentToken("token", "issuer", "ref_demo_opaque_123", 100))
        try { OpaqueTokenCodec.decode(encoded + byteArrayOf(1)); fail("Trailing data rejected") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun tokenIdentifiersCannotSmuggleBankNumbersIntoStorage() {
        for (token in listOf(
            PaymentToken("4111111111111111", "issuer", "ref_demo_opaque_123", 100),
            PaymentToken("token", "issuer_4111111111111111", "ref_demo_opaque_123", 100)
        )) {
            try { OpaqueTokenCodec.encode(token); fail("Identifier must not contain bank number") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
