package org.azertio.persistence.test.attachment;

import org.junit.jupiter.api.Test;
import org.azertio.core.persistence.AttachmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractAttachmentRepositoryTest {

	protected abstract AttachmentRepository repository();

	// ── storeAttachment ────────────────────────────────────────────────────────

	@Test
	void storeAttachment_withContentType() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		assertDoesNotThrow(() ->
			repository().storeAttachment(executionID, nodeID, attachmentID, "hello attachment".getBytes(), "text/plain")
		);
	}

	@Test
	void storeAttachment_withoutContentType() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		assertDoesNotThrow(() ->
			repository().storeAttachment(executionID, nodeID, attachmentID, new byte[]{1, 2, 3, 4}, null)
		);
	}

	@Test
	void storeAttachment_binaryContent() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();
		byte[] content = new byte[1024];
		for (int i = 0; i < content.length; i++) {
			content[i] = (byte) (i % 256);
		}

		assertDoesNotThrow(() ->
			repository().storeAttachment(executionID, nodeID, attachmentID, content, "application/octet-stream")
		);
	}

	@Test
	void storeAttachment_emptyContent() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		assertDoesNotThrow(() ->
			repository().storeAttachment(executionID, nodeID, attachmentID, new byte[0], "text/plain")
		);
	}

	@Test
	void storeAttachment_multipleAttachmentsForSameExecution() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();

		assertDoesNotThrow(() -> {
			repository().storeAttachment(executionID, nodeID, UUID.randomUUID(), "first".getBytes(), "text/plain");
			repository().storeAttachment(executionID, nodeID, UUID.randomUUID(), "second".getBytes(), "text/plain");
		});
	}

	@Test
	void storeAttachment_sameAttachmentDifferentExecutions() {
		UUID attachmentID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();

		assertDoesNotThrow(() -> {
			repository().storeAttachment(UUID.randomUUID(), nodeID, attachmentID, "first".getBytes(), "text/plain");
			repository().storeAttachment(UUID.randomUUID(), nodeID, attachmentID, "second".getBytes(), "text/plain");
		});
	}

	// ── retrieveAttachment ─────────────────────────────────────────────────────

	@Test
	void retrieveAttachment_existingAttachment_returnsContent() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();
		byte[] content = "retrieve me".getBytes();

		repository().storeAttachment(executionID, nodeID, attachmentID, content, "text/plain");

		Optional<AttachmentRepository.Attachment> result = repository().retrieveAttachment(executionID, nodeID, attachmentID);

		assertTrue(result.isPresent());
		assertEquals(attachmentID, result.get().attachmentID());
		assertArrayEquals(content, result.get().bytes());
	}

	@Test
	void retrieveAttachment_existingAttachment_returnsContentType() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeID, attachmentID, "data".getBytes(), "image/png");

		Optional<AttachmentRepository.Attachment> result = repository().retrieveAttachment(executionID, nodeID, attachmentID);

		assertTrue(result.isPresent());
		assertEquals("image/png", result.get().contentType());
	}

	@Test
	void retrieveAttachment_missingAttachment_returnsEmpty() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		Optional<AttachmentRepository.Attachment> result = repository().retrieveAttachment(executionID, nodeID, attachmentID);

		assertTrue(result.isEmpty());
	}

	@Test
	void retrieveAttachment_wrongNode_returnsEmpty() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeID, attachmentID, "data".getBytes(), "text/plain");

		Optional<AttachmentRepository.Attachment> result = repository().retrieveAttachment(executionID, UUID.randomUUID(), attachmentID);

		assertTrue(result.isEmpty());
	}

	// ── streamAttachments ──────────────────────────────────────────────────────

	@Test
	void streamAttachments_returnsAllStoredAttachments() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeID, id1, "a".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeID, id2, "b".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeID, id3, "c".getBytes(), "text/plain");

		List<UUID> ids;
		try (Stream<AttachmentRepository.Attachment> stream = repository().streamAttachments(executionID, nodeID)) {
			ids = stream.map(AttachmentRepository.Attachment::attachmentID).toList();
		}

		assertEquals(3, ids.size());
		assertTrue(ids.containsAll(List.of(id1, id2, id3)));
	}

	@Test
	void streamAttachments_emptyNode_returnsEmptyStream() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();

		long count;
		try (Stream<AttachmentRepository.Attachment> stream = repository().streamAttachments(executionID, nodeID)) {
			count = stream.count();
		}

		assertEquals(0, count);
	}

	@Test
	void streamAttachments_isolatedByNode() {
		UUID executionID = UUID.randomUUID();
		UUID nodeA = UUID.randomUUID();
		UUID nodeB = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeA, UUID.randomUUID(), "a".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeA, UUID.randomUUID(), "b".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeB, UUID.randomUUID(), "c".getBytes(), "text/plain");

		long countA;
		long countB;
		try (Stream<AttachmentRepository.Attachment> streamA = repository().streamAttachments(executionID, nodeA)) {
			countA = streamA.count();
		}
		try (Stream<AttachmentRepository.Attachment> streamB = repository().streamAttachments(executionID, nodeB)) {
			countB = streamB.count();
		}

		assertEquals(2, countA);
		assertEquals(1, countB);
	}

	// ── deleteAttachment ───────────────────────────────────────────────────────

	@Test
	void deleteAttachment_removesStoredAttachment() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeID, attachmentID, "to delete".getBytes(), "text/plain");
		repository().deleteAttachment(executionID, nodeID, attachmentID);

		Optional<AttachmentRepository.Attachment> result = repository().retrieveAttachment(executionID, nodeID, attachmentID);
		assertTrue(result.isEmpty());
	}

	@Test
	void deleteAttachment_onlyRemovesTargetAttachment() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID toDelete = UUID.randomUUID();
		UUID toKeep = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeID, toDelete, "gone".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeID, toKeep, "kept".getBytes(), "text/plain");
		repository().deleteAttachment(executionID, nodeID, toDelete);

		assertTrue(repository().retrieveAttachment(executionID, nodeID, toDelete).isEmpty());
		assertTrue(repository().retrieveAttachment(executionID, nodeID, toKeep).isPresent());
	}

	@Test
	void deleteAttachment_nonExistingAttachment_doesNotThrow() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		assertDoesNotThrow(() -> repository().deleteAttachment(executionID, nodeID, attachmentID));
	}

	// ── deleteAttachments ──────────────────────────────────────────────────────

	@Test
	void deleteAttachments_removesAllAttachmentsForExecution() {
		UUID executionID = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeID, id1, "a".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeID, id2, "b".getBytes(), "text/plain");
		repository().deleteAttachments(executionID);

		assertTrue(repository().retrieveAttachment(executionID, nodeID, id1).isEmpty());
		assertTrue(repository().retrieveAttachment(executionID, nodeID, id2).isEmpty());
	}

	@Test
	void deleteAttachments_doesNotAffectOtherExecutions() {
		UUID executionToDelete = UUID.randomUUID();
		UUID executionToKeep = UUID.randomUUID();
		UUID nodeID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		repository().storeAttachment(executionToDelete, nodeID, UUID.randomUUID(), "gone".getBytes(), "text/plain");
		repository().storeAttachment(executionToKeep, nodeID, attachmentID, "kept".getBytes(), "text/plain");
		repository().deleteAttachments(executionToDelete);

		assertTrue(repository().retrieveAttachment(executionToKeep, nodeID, attachmentID).isPresent());
	}

	@Test
	void deleteAttachments_nonExistingExecution_doesNotThrow() {
		assertDoesNotThrow(() -> repository().deleteAttachments(UUID.randomUUID()));
	}

	@Test
	void deleteAttachments_acrossMultipleNodes() {
		UUID executionID = UUID.randomUUID();
		UUID nodeA = UUID.randomUUID();
		UUID nodeB = UUID.randomUUID();
		UUID idA = UUID.randomUUID();
		UUID idB = UUID.randomUUID();

		repository().storeAttachment(executionID, nodeA, idA, "a".getBytes(), "text/plain");
		repository().storeAttachment(executionID, nodeB, idB, "b".getBytes(), "text/plain");
		repository().deleteAttachments(executionID);

		assertTrue(repository().retrieveAttachment(executionID, nodeA, idA).isEmpty());
		assertTrue(repository().retrieveAttachment(executionID, nodeB, idB).isEmpty());
	}

}
