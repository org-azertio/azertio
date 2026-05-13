package org.myjtools.openbbt.core.test.execution;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.Clock;
import org.myjtools.openbbt.core.backend.Benchmark;
import org.myjtools.openbbt.core.execution.ExecutionNodeStats;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TestBenchmark {

    // --- markStarted ---

    @Test
    void markStarted_firstCallReturns1() {
        var benchmark = new Benchmark(1, 1, fixedClock());
        assertThat(benchmark.markStarted()).isEqualTo(1);
    }

    @Test
    void markStarted_subsequentCallsIncrement() {
        var benchmark = new Benchmark(3, 1, fixedClock());
        assertThat(benchmark.markStarted()).isEqualTo(1);
        assertThat(benchmark.markStarted()).isEqualTo(2);
        assertThat(benchmark.markStarted()).isEqualTo(3);
    }

    // --- numThreads passthrough ---

    @Test
    void statistics_numThreadsIsPassedThrough() {
        var benchmark = new Benchmark(1, 8, fixedClock());
        benchmark.markFinished(benchmark.markStarted(), false);
        assertThat(benchmark.statistics().numThreads()).isEqualTo(8);
    }

    // --- single execution ---

    @Test
    void statistics_singleExecution_allTimeFieldsEqualThatTime() {
        ExecutionNodeStats stats = singleExecution(200, false).statistics();

        assertThat(stats.numExecutions()).isEqualTo(1);
        assertThat(stats.min()).isEqualTo(200);
        assertThat(stats.max()).isEqualTo(200);
        assertThat(stats.mean()).isEqualTo(200);
        assertThat(stats.p50()).isEqualTo(200);
        assertThat(stats.p95()).isEqualTo(200);
        assertThat(stats.p99()).isEqualTo(200);
    }

    @Test
    void statistics_singleExecution_correctThroughput() {
        // 1 execution in 500ms → throughput = 1 / 0.5s = 2.0
        ExecutionNodeStats stats = singleExecution(500, false).statistics();
        assertThat(stats.throughput()).isCloseTo(2.0, within(0.001));
    }

    // --- multiple executions: min / max / mean ---

    // dataset: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
    // sum=550, mean=55, p50=skip(5)→60, p95=skip(9)→100, p99=skip(9)→100

    @Test
    void statistics_tenExecutions_correctMin() {
        assertThat(tenExecutions().statistics().min()).isEqualTo(10);
    }

    @Test
    void statistics_tenExecutions_correctMax() {
        assertThat(tenExecutions().statistics().max()).isEqualTo(100);
    }

    @Test
    void statistics_tenExecutions_correctMean() {
        assertThat(tenExecutions().statistics().mean()).isEqualTo(55);
    }

    @Test
    void statistics_tenExecutions_correctNumExecutions() {
        assertThat(tenExecutions().statistics().numExecutions()).isEqualTo(10);
    }

    // --- percentiles ---

    @Test
    void statistics_tenExecutions_correctP50() {
        // sorted[10..100], skip(10/2=5) → 6th element = 60
        assertThat(tenExecutions().statistics().p50()).isEqualTo(60);
    }

    @Test
    void statistics_tenExecutions_correctP95() {
        // skip((long)(10*0.95)=9) → 10th element = 100
        assertThat(tenExecutions().statistics().p95()).isEqualTo(100);
    }

    @Test
    void statistics_tenExecutions_correctP99() {
        // skip((long)(10*0.99)=9) → 10th element = 100
        assertThat(tenExecutions().statistics().p99()).isEqualTo(100);
    }

    // --- throughput ---

    @Test
    void statistics_tenExecutions_correctThroughput() {
        // totalTime=550ms → throughput = 10 / 0.55 ≈ 18.18
        ExecutionNodeStats stats = tenExecutions().statistics();
        assertThat(stats.throughput()).isCloseTo(10.0 / 0.55, within(0.001));
    }

    // --- error rate ---

    @Test
    void statistics_noErrors_errorRateIsZero() {
        assertThat(singleExecution(100, false).statistics().errorRate()).isEqualTo(0.0);
    }

    @Test
    void statistics_allErrors_errorRateIsOne() {
        var time = new AtomicReference<>(Instant.EPOCH);
        var benchmark = new Benchmark(5, 1, time::get);
        for (int i = 0; i < 5; i++) {
            int n = benchmark.markStarted();
            time.set(time.get().plusMillis(100));
            benchmark.markFinished(n, true);
        }
        assertThat(benchmark.statistics().errorRate()).isEqualTo(1.0);
    }

    @Test
    void statistics_halfErrors_errorRateIsHalf() {
        var time = new AtomicReference<>(Instant.EPOCH);
        var benchmark = new Benchmark(4, 1, time::get);
        for (int i = 0; i < 4; i++) {
            int n = benchmark.markStarted();
            time.set(time.get().plusMillis(100));
            benchmark.markFinished(n, i % 2 == 0);
        }
        assertThat(benchmark.statistics().errorRate()).isEqualTo(0.5);
    }

    @Test
    void statistics_errorMarksDoNotAffectTimings() {
        var time = new AtomicReference<>(Instant.EPOCH);
        var benchmark = new Benchmark(2, 1, time::get);

        int n1 = benchmark.markStarted();
        time.set(time.get().plusMillis(100));
        benchmark.markFinished(n1, true);

        int n2 = benchmark.markStarted();
        time.set(time.get().plusMillis(200));
        benchmark.markFinished(n2, false);

        ExecutionNodeStats stats = benchmark.statistics();
        System.out.println(stats);
        assertThat(stats.min()).isEqualTo(100);
        assertThat(stats.max()).isEqualTo(200);
        assertThat(stats.errorRate()).isEqualTo(0.5);
    }

    // --- helpers ---

    private Clock fixedClock() {
        return () -> Instant.EPOCH;
    }

    private Benchmark singleExecution(int timeMs, boolean error) {
        var time = new AtomicReference<>(Instant.EPOCH);
        var benchmark = new Benchmark(1, 1, time::get);
        int n = benchmark.markStarted();
        time.set(time.get().plusMillis(timeMs));
        benchmark.markFinished(n, error);
        return benchmark;
    }

    private Benchmark tenExecutions() {
        var time = new AtomicReference<>(Instant.EPOCH);
        var benchmark = new Benchmark(10, 1, time::get);
        for (int t = 10; t <= 100; t += 10) {
            int n = benchmark.markStarted();
            time.set(time.get().plusMillis(t));
            benchmark.markFinished(n, false);
        }
        return benchmark;
    }
}