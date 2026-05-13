package org.azertio.core.persistence;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface AttachmentRepository extends Repository {

	record Attachment(UUID attachmentID, byte[] bytes, String contentType) {
		@Override
		public int hashCode() {
			return attachmentID.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			Attachment other = (Attachment) obj;
			return attachmentID.equals(other.attachmentID);
		}

		@Override
		public String toString() {
			return attachmentID.toString();
		}
	}


	void storeAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID, byte[] bytes, String contentType);

	void deleteAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID);

	void deleteAttachments(UUID executionID);

	Optional<Attachment> retrieveAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID);

	Stream<Attachment> streamAttachments(UUID executionID, UUID executionNodeID);

}
