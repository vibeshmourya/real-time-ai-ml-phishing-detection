package com.phishshield;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ScanType;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.dto.request.URLScanRequest;
import com.phishshield.dto.response.PreventionContentResponse;
import com.phishshield.dto.response.URLScanResponse;
import com.phishshield.service.MultiSignalScoringEngine;
import com.phishshield.service.PreventionContentService;
import com.phishshield.service.RuleEngineService;
import com.phishshield.service.URLScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PerformanceAndConcurrencyBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAndConcurrencyBenchmarkTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RuleEngineService ruleEngineService;

    @Autowired
    private MultiSignalScoringEngine multiSignalScoringEngine;

    @Autowired
    private PreventionContentService preventionContentService;

    @Autowired
    private URLScanService urlScanService;

    /**
     * Measures latency statistics (min, median, p95, p99, max, throughput) for a list of sample latencies in nanoseconds.
     */
    private Map<String, Double> computeStats(List<Long> latenciesNanos) {
        Collections.sort(latenciesNanos);
        int n = latenciesNanos.size();
        double minMs = latenciesNanos.get(0) / 1_000_000.0;
        double maxMs = latenciesNanos.get(n - 1) / 1_000_000.0;
        double medianMs = latenciesNanos.get((int) (n * 0.50)) / 1_000_000.0;
        double p95Ms = latenciesNanos.get((int) (n * 0.95)) / 1_000_000.0;
        double p99Ms = latenciesNanos.get((int) (n * 0.99)) / 1_000_000.0;

        long totalNanos = latenciesNanos.stream().mapToLong(Long::longValue).sum();
        double totalSec = totalNanos / 1_000_000_000.0;
        double throughput = (totalSec > 0) ? (n / totalSec) : 0.0;

        Map<String, Double> stats = new LinkedHashMap<>();
        stats.put("minMs", minMs);
        stats.put("medianMs", medianMs);
        stats.put("p95Ms", p95Ms);
        stats.put("p99Ms", p99Ms);
        stats.put("maxMs", maxMs);
        stats.put("throughputRps", throughput);
        return stats;
    }

    @Test
    @DisplayName("BENCHMARK 1: Liveness and Readiness Probe Latency Under MockMvc")
    void benchmarkHealthEndpoints() throws Exception {
        List<Long> livenessLatencies = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long start = System.nanoTime();
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("UP"));
            livenessLatencies.add(System.nanoTime() - start);
        }

        Map<String, Double> livenessStats = computeStats(livenessLatencies);
        log.info("Liveness Probe Benchmark (50 samples): median={:.2f}ms, p95={:.2f}ms, throughput={:.1f} req/s",
                livenessStats.get("medianMs"), livenessStats.get("p95Ms"), livenessStats.get("throughputRps"));

        assertTrue(livenessStats.get("medianMs") < 25.0, "Liveness probe median latency should be < 25ms");

        List<Long> readinessLatencies = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long start = System.nanoTime();
            mockMvc.perform(get("/api/health/readiness"))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 200 || code == 503, "Readiness probe must return 200 (healthy) or 503 (degraded)");
                    });
            readinessLatencies.add(System.nanoTime() - start);
        }
        Map<String, Double> readinessStats = computeStats(readinessLatencies);
        log.info("Readiness Probe Benchmark (20 samples): median={:.2f}ms, p95={:.2f}ms",
                readinessStats.get("medianMs"), readinessStats.get("p95Ms"));
    }

    @Test
    @DisplayName("BENCHMARK 2: Rule Engine In-Memory Heuristic Execution (100 Iterations)")
    void benchmarkRuleEngineThroughput() {
        String testUrl = "http://login-security-update-sbi-portal.com.attacker.xyz/verify-otp?token=9382";
        String domain = "attacker.xyz";

        // Warmup
        for (int i = 0; i < 10; i++) {
            ruleEngineService.evaluateUrl(testUrl, domain);
        }

        List<Long> latencies = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            RuleEngineService.RuleEvaluationResult result = ruleEngineService.evaluateUrl(testUrl, domain);
            latencies.add(System.nanoTime() - start);
            assertNotNull(result);
            assertTrue(result.getRiskScore() > 0);
        }

        Map<String, Double> stats = computeStats(latencies);
        log.info("Rule Engine (100 runs): min={:.3f}ms, median={:.3f}ms, p95={:.3f}ms, throughput={:.0f} checks/s",
                stats.get("minMs"), stats.get("medianMs"), stats.get("p95Ms"), stats.get("throughputRps"));

        assertTrue(stats.get("medianMs") < 2.0, "Rule Engine median execution should be sub-2ms in-process");
    }

    @Test
    @DisplayName("BENCHMARK 3: Multi-Signal Scoring Engine Composite Fusion (100 Iterations)")
    void benchmarkMultiSignalScoringEngine() {
        String url = "https://verified-bank-secure.online/auth";
        String domain = "verified-bank-secure.online";
        RuleEngineService.RuleEvaluationResult ruleResult = ruleEngineService.evaluateUrl(url, domain);

        // Warmup
        for (int i = 0; i < 10; i++) {
            multiSignalScoringEngine.computeCompositeScore(url, domain, ruleResult, null, null);
        }

        List<Long> latencies = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            MultiSignalScoringEngine.CompositeScoringResult result =
                    multiSignalScoringEngine.computeCompositeScore(url, domain, ruleResult, null, null);
            latencies.add(System.nanoTime() - start);
            assertNotNull(result);
        }

        Map<String, Double> stats = computeStats(latencies);
        log.info("Multi-Signal Scoring Engine (100 runs): median={:.3f}ms, p95={:.3f}ms, throughput={:.0f} ops/s",
                stats.get("medianMs"), stats.get("p95Ms"), stats.get("throughputRps"));

        assertTrue(stats.get("medianMs") < 1.0, "Multi-Signal scoring median should be sub-millisecond");
    }

    @Test
    @DisplayName("BENCHMARK 4: Prevention Content Service Caching Latency Verification")
    void benchmarkPreventionGuideCache() {
        // Cold fetch (initial DB query / fallback)
        long coldStart = System.nanoTime();
        PreventionContentResponse coldGuide = preventionContentService.getPreventionGuide(ThreatCategory.CREDENTIAL_THEFT, "en");
        long coldDurationMs = (System.nanoTime() - coldStart) / 1_000_000;
        assertNotNull(coldGuide);

        // Warm cached fetches (100 iterations)
        List<Long> warmLatencies = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            PreventionContentResponse warmGuide = preventionContentService.getPreventionGuide(ThreatCategory.CREDENTIAL_THEFT, "en");
            warmLatencies.add(System.nanoTime() - start);
            assertNotNull(warmGuide);
            assertEquals(coldGuide.getThreatCategory(), warmGuide.getThreatCategory());
        }

        Map<String, Double> warmStats = computeStats(warmLatencies);
        log.info("Prevention Guide Cold fetch: {}ms | Cached median: {:.3f}ms, p95: {:.3f}ms, throughput: {:.0f} ops/s",
                coldDurationMs, warmStats.get("medianMs"), warmStats.get("p95Ms"), warmStats.get("throughputRps"));

        assertTrue(warmStats.get("medianMs") < 1.0, "Cached Prevention Guide lookup should be sub-millisecond");
    }

    @Test
    @DisplayName("CONCURRENCY & LOAD: Concurrent Scan Requests with Thread Safety & Data Isolation")
    void testConcurrentUrlScans() throws Exception {
        int threadCount = 10;
        int requestsPerThread = 3;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        Set<Long> scanIds = Collections.synchronizedSet(new HashSet<>());

        String[] testUrls = {
                "https://secure-banking-portal-verification-notice.com/login",
                "https://irs-tax-refund-gov-service-claim.net/portal",
                "https://paypal-account-suspended-activity-check.xyz/reactivate",
                "https://github.com/torvalds/linux",
                "https://wikipedia.org/wiki/Computer_security"
        };

        for (int t = 0; t < totalRequests; t++) {
            final int index = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize threads for burst execution
                    String url = testUrls[index % testUrls.length];
                    MockHttpServletRequest mockReq = new MockHttpServletRequest();
                    mockReq.setRemoteAddr("192.168.1." + (10 + (index % 50)));

                    URLScanRequest req = new URLScanRequest();
                    req.setUrl(url);

                    long start = System.nanoTime();
                    URLScanResponse response = null;
                    for (int retry = 0; retry < 3; retry++) {
                        try {
                            response = urlScanService.scanUrl(req, mockReq);
                            break;
                        } catch (Exception ex) {
                            if (retry == 2) throw ex;
                            Thread.sleep(15);
                        }
                    }
                    long duration = System.nanoTime() - start;

                    latencies.add(duration);
                    if (response != null && response.getScanId() != null) {
                        successCounter.incrementAndGet();
                        scanIds.add(response.getScanId());
                    } else {
                        errorCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Concurrent scan error on index {}: {}", index, e.getMessage(), e);
                    errorCounter.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        long burstStart = System.currentTimeMillis();
        startLatch.countDown();

        boolean finished = finishLatch.await(30, TimeUnit.SECONDS);
        long burstDurationMs = System.currentTimeMillis() - burstStart;
        executor.shutdown();

        assertTrue(finished, "Concurrent load test should finish within 30 seconds");
        assertEquals(totalRequests, successCounter.get(), "All concurrent requests should succeed without errors");
        assertEquals(0, errorCounter.get(), "Zero errors expected during controlled concurrency load test");
        assertEquals(totalRequests, scanIds.size(), "Each persisted scan record must have a unique identifier (no collisions)");

        Map<String, Double> stats = computeStats(new ArrayList<>(latencies));
        double aggregateThroughput = (totalRequests / (burstDurationMs / 1000.0));
        log.info("CONCURRENT SCAN BURST RESULTS ({} threads, {} requests in {}ms):", threadCount, totalRequests, burstDurationMs);
        log.info("  Latency: min={:.1f}ms, median={:.1f}ms, p95={:.1f}ms, p99={:.1f}ms, max={:.1f}ms",
                stats.get("minMs"), stats.get("medianMs"), stats.get("p95Ms"), stats.get("p99Ms"), stats.get("maxMs"));
        log.info("  Throughput: {:.1f} scans/sec | Success Rate: 100.0%", aggregateThroughput);
    }

    @Test
    @DisplayName("STRESS & RELIABILITY: Malformed, Deep, and Unusually Long URLs Under Load")
    void testLongAndEdgeCaseUrlsUnderLoad() {
        // 1. Extreme 2,000-character URL
        StringBuilder longUrl = new StringBuilder("https://example.com/path?");
        for (int i = 0; i < 100; i++) {
            longUrl.append("param").append(i).append("=abcdef1234567890&");
        }
        MockHttpServletRequest mockReq = new MockHttpServletRequest();

        URLScanRequest longReq = new URLScanRequest();
        longReq.setUrl(longUrl.toString());

        long start = System.currentTimeMillis();
        URLScanResponse response = urlScanService.scanUrl(longReq, mockReq);
        long durationMs = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertEquals(ScanType.URL, response.getScanType());
        assertTrue(durationMs < 2000, "Unusually long URL (2KB) should process in under 2 seconds without crashing");

        // 2. Cyrillic Homoglyph URL
        URLScanRequest homoglyphReq = new URLScanRequest();
        homoglyphReq.setUrl("https://\u0440\u0430\u0443\u0440\u0430l.com/account/login"); // Cyrillic 'p' and 'a'
        URLScanResponse homoglyphResp = urlScanService.scanUrl(homoglyphReq, mockReq);

        assertNotNull(homoglyphResp);
        assertTrue(homoglyphResp.getRiskScore() >= 35, "Punycode/Homoglyph should be flagged with suspicious/high risk");
    }
}
