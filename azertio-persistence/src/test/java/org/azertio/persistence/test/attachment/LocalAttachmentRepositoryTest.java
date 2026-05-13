package org.azertio.persistence.test.attachment;

import org.junit.jupiter.api.io.TempDir;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.persistence.attachment.LocalAttachmentRepository;
import java.nio.file.Path;

class LocalAttachmentRepositoryTest extends AbstractAttachmentRepositoryTest {

	@TempDir
	private Path tempDir;

	@Override
	protected AttachmentRepository repository() {
		return new LocalAttachmentRepository(tempDir);
	}

}
