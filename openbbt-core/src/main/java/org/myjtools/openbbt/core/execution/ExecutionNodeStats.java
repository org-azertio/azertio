package org.myjtools.openbbt.core.execution;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExecutionNodeStats {
    private int numExecutions;
    private int numThreads;
    private int min;
    private int max;
    private int mean;
    private int p50;
    private int p95;
    private int p99;
    private double throughput;
    private double errorRate;

    @Override
    public String toString() {
        return """
             | Executions |  Threads  |    Min    |    Max    |   Mean    |    P50    |    P95    |    P99    | Throughput | Error Rate |
             ----------------------------------------------------------------------------------------------------------------------------
             | %10d | %9d | %9d | %9d | %9d | %9d | %9d | %9d | %10.2f | %9.2f%% |
             """.formatted(
             numExecutions, numThreads, min, max, mean, p50, p95, p99, throughput, errorRate * 100);
    }
}