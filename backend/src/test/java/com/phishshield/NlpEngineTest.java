package com.phishshield;

import com.phishshield.nlp.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NlpEngineTest {

    @Test
    @DisplayName("Test Normalizer handles typos and Hinglish phrases")
    void testNormalizer() {
        String input = "mera 5000 rs. kat gaya aur opt share ho gya";
        String normalized = NlpNormalizer.normalize(input);

        assertTrue(normalized.contains("debited"));
        assertTrue(normalized.contains("otp"));
        assertTrue(normalized.contains("₹"));

        String typoInput = "this is froud and phising with fake apkk";
        String normalizedTypo = NlpNormalizer.normalize(typoInput);
        assertTrue(normalizedTypo.contains("fraud"));
        assertTrue(normalizedTypo.contains("phishing"));
        assertTrue(normalizedTypo.contains("apk"));
    }

    @Test
    @DisplayName("Test Entity Extractor captures amounts, channels, and institutions")
    void testEntityExtractor() {
        String input = "₹12,500 debited via UPI to merchant on SBI account";
        ExtractedEntities entities = EntityExtractor.extract(input);

        assertNotNull(entities.getAmount());
        assertEquals(new BigDecimal("12500"), entities.getAmount());
        assertNotNull(entities.getInstitution());
        assertTrue(entities.getInstitution().contains("SBI"));
        assertEquals("DEBIT", entities.getDirection());
    }

    @Test
    @DisplayName("Test Intent Classifier across core cybersecurity & fraud domains")
    void testIntentClassification() {
        // Unknown Debit
        IntentClassifier.ClassificationResult res1 = IntentClassifier.classify(
                NlpNormalizer.normalize("₹5,000 debited from my account without permission"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.UNKNOWN_DEBIT, res1.getIntent());
        assertTrue(res1.getConfidence() >= 0.90);

        // Stolen Phone
        IntentClassifier.ClassificationResult res2 = IntentClassifier.classify(
                NlpNormalizer.normalize("My mobile phone was stolen on the bus"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.STOLEN_PHONE, res2.getIntent());

        // SIM Swap
        IntentClassifier.ClassificationResult res3 = IntentClassifier.classify(
                NlpNormalizer.normalize("My sim card is deactivated and suddenly showing no network"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.SIM_SWAP, res3.getIntent());

        // Fake Bank APK
        IntentClassifier.ClassificationResult res4 = IntentClassifier.classify(
                NlpNormalizer.normalize("I installed an apk from a telegram message"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.FAKE_BANK_APP, res4.getIntent());

        // OTP Disclosure
        IntentClassifier.ClassificationResult res5 = IntentClassifier.classify(
                NlpNormalizer.normalize("I shared my OTP with someone posing as customer care"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.OTP_DISCLOSURE, res5.getIntent());

        // QR Code
        IntentClassifier.ClassificationResult res6 = IntentClassifier.classify(
                NlpNormalizer.normalize("Buyer told me to scan a QR code to receive payment"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.SUSPICIOUS_QR, res6.getIntent());

        // Transaction Summary
        IntentClassifier.ClassificationResult res7 = IntentClassifier.classify(
                NlpNormalizer.normalize("Show my spending summary and total expenses"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.TRANSACTION_SUMMARY, res7.getIntent());
    }

    @Test
    @DisplayName("Test Multi-turn Context slot tracking for payment channel answers")
    void testContextSlotTracking() {
        DialogueContext context = new DialogueContext();
        context.setLastIntent(NlpIntent.UNKNOWN_DEBIT);
        context.setAwaitingSlot("payment_channel");

        ExtractedEntities turn2Entities = new ExtractedEntities();
        turn2Entities.setPaymentChannel("UPI");

        IntentClassifier.ClassificationResult followUpRes = IntentClassifier.classify("UPI", context, turn2Entities);
        assertTrue(followUpRes.isFollowUp());
        // Answering "UPI" to payment channel question transitions to UPI-specific containment
        assertEquals(NlpIntent.UPI_FRAUD, followUpRes.getIntent());
    }

    @Test
    @DisplayName("Test Informational questions avoid false positive incident classification")
    void testInformationalQuestionsVsIncidents() {
        IntentClassifier.ClassificationResult res = IntentClassifier.classify(
                NlpNormalizer.normalize("What is phishing?"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.GENERAL_CYBER_SAFETY, res.getIntent());

        IntentClassifier.ClassificationResult res2 = IntentClassifier.classify(
                NlpNormalizer.normalize("What is UPI?"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.GENERAL_CYBER_SAFETY, res2.getIntent());
    }

    @Test
    @DisplayName("Test Unrelated queries are recognized and bounded")
    void testUnrelatedQueryRecognition() {
        IntentClassifier.ClassificationResult res = IntentClassifier.classify(
                NlpNormalizer.normalize("Tell me a recipe for chocolate cake"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.UNRELATED_QUERY, res.getIntent());
        assertFalse(res.isFollowUp());
    }

    @Test
    @DisplayName("Test Assistant Identity queries - 'tell me about u', 'who are you'")
    void testAssistantIdentityClassification() {
        IntentClassifier.ClassificationResult res1 = IntentClassifier.classify(
                NlpNormalizer.normalize("tell me about u"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.ASSISTANT_IDENTITY, res1.getIntent());

        IntentClassifier.ClassificationResult res2 = IntentClassifier.classify(
                NlpNormalizer.normalize("who are you"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.ASSISTANT_IDENTITY, res2.getIntent());
    }

    @Test
    @DisplayName("Test Fake Shopping Scam queries - 'ordered an item from fake webpage'")
    void testFakeShoppingScamClassification() {
        IntentClassifier.ClassificationResult res = IntentClassifier.classify(
                NlpNormalizer.normalize("i think i have opened a fake webpage of a shopping site and i have ordered an item from it"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.FAKE_SHOPPING_SCAM, res.getIntent());
    }

    @Test
    @DisplayName("Test General Cyber Safety queries - 'how can i be safe from any attacks or online frauds'")
    void testGeneralCyberSafetyClassification() {
        IntentClassifier.ClassificationResult res = IntentClassifier.classify(
                NlpNormalizer.normalize("how can i be safe from any attacks or online frauds"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.GENERAL_CYBER_SAFETY, res.getIntent());
    }

    @Test
    @DisplayName("Test OTP disclosure with informal slang - 'bro i gave my otp to someone'")
    void testOtpDisclosureSlangClassification() {
        IntentClassifier.ClassificationResult res = IntentClassifier.classify(
                NlpNormalizer.normalize("bro i gave my otp to someone"),
                new DialogueContext(),
                new ExtractedEntities()
        );
        assertEquals(NlpIntent.OTP_DISCLOSURE, res.getIntent());
    }

    @Test
    @DisplayName("Test Multi-turn shopping scam slot tracking for payment method follow-up")
    void testShoppingPaymentSlotTracking() {
        DialogueContext context = new DialogueContext();
        context.setLastIntent(NlpIntent.FAKE_SHOPPING_SCAM);
        context.setAwaitingSlot("shopping_payment_details");

        ExtractedEntities entities = new ExtractedEntities();
        entities.setPaymentChannel("UPI");

        IntentClassifier.ClassificationResult followUpRes = IntentClassifier.classify("upi", context, entities);
        assertTrue(followUpRes.isFollowUp());
        assertEquals(NlpIntent.FAKE_SHOPPING_SCAM, followUpRes.getIntent());
    }
}
