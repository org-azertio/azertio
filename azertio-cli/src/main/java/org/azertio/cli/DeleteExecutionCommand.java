package org.azertio.cli;

import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.core.persistence.TestExecutionRepository;
import picocli.CommandLine;

import java.util.UUID;

@CommandLine.Command(
    name = "delete-execution",
    description = "Delete an execution and all its data"
)
public final class DeleteExecutionCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--execution-id"},
        description = "UUID of the execution to delete",
        required = true
    )
    UUID executionId;

    @Override
    protected void execute() {
        AzertioRuntime runtime = AzertioRuntime.repositoryOnly(getContext().configuration());
        TestExecutionRepository executionRepo = runtime.getRepository(TestExecutionRepository.class);
        AttachmentRepository attachmentRepo = runtime.getRepository(AttachmentRepository.class);
        if (attachmentRepo != null) {
            attachmentRepo.deleteAttachments(executionId);
        }
        executionRepo.deleteExecution(executionId);
        System.out.println("Execution " + executionId + " deleted.");
    }
}
