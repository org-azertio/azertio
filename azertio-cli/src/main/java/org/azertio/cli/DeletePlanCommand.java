package org.azertio.cli;

import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import picocli.CommandLine;

import java.util.UUID;

@CommandLine.Command(
    name = "delete-plan",
    description = "Delete a plan and all its executions"
)
public final class DeletePlanCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--plan-id"},
        description = "UUID of the plan to delete",
        required = true
    )
    UUID planId;

    @Override
    protected void execute() {
        AzertioRuntime runtime = AzertioRuntime.repositoryOnly(getContext().configuration());
        TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
        TestExecutionRepository executionRepo = runtime.getRepository(TestExecutionRepository.class);
        AttachmentRepository attachmentRepo = runtime.getRepository(AttachmentRepository.class);

        var plan = planRepo.getPlan(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        // Delete file-system attachments before the DB cascades remove the execution records
        if (executionRepo != null && attachmentRepo != null) {
            executionRepo.listExecutions(planId, plan.planNodeRoot(), 0, 0)
                .forEach(ex -> attachmentRepo.deleteAttachments(ex.executionID()));
        }
        // Deleting the plan cascades: executions, execution_nodes, attachment records, plan_nodes
        planRepo.deletePlan(planId);
        System.out.println("Plan " + planId + " and all its executions deleted.");
    }
}
