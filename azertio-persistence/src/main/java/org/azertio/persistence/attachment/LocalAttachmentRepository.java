package org.azertio.persistence.attachment;

import org.azertio.core.AzertioException;
import org.azertio.core.persistence.AttachmentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class LocalAttachmentRepository implements AttachmentRepository {

	private final Path basePath;

	public LocalAttachmentRepository(Path basePath) {
		this.basePath = basePath;
	}

	@Override
	public void storeAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID, byte[] bytes, String contentType) {
		try {
			Path dir = basePath.resolve(executionID.toString()).resolve(executionNodeID.toString());
			Files.createDirectories(dir);
			Files.write(dir.resolve(attachmentID.toString()), bytes);
			if (contentType != null) {
				Files.writeString(dir.resolve(attachmentID + ".mime"), contentType);
			}
		} catch (IOException e) {
			throw new AzertioException(e, "Failed to store attachment {}", attachmentID);
		}
	}

	@Override
	public Optional<Attachment> retrieveAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID) {
		Path dir = basePath.resolve(executionID.toString()).resolve(executionNodeID.toString());
		Path file = dir.resolve(attachmentID.toString());
		if (Files.exists(file)) {
			try {
				byte[] bytes = Files.readAllBytes(file);
				String contentType = "application/octet-stream";
				Path mimeFile = dir.resolve(attachmentID + ".mime");
				if (Files.exists(mimeFile)) {
					contentType = Files.readString(mimeFile);
				}
				return Optional.of(new Attachment(attachmentID, bytes, contentType));
			} catch (IOException e) {
				throw new AzertioException(e, "Failed to read attachment {}", attachmentID);
			}
		} else {
			return Optional.empty();
		}
	}

	@Override
	public java.util.stream.Stream<Attachment> streamAttachments(UUID executionID, UUID executionNodeID) {
		Path dir = basePath.resolve(executionID.toString()).resolve(executionNodeID.toString());
		if (Files.exists(dir) && Files.isDirectory(dir)) {
			try {
				return Files.list(dir)
					.filter(path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".mime"))
					.map(path -> {
						try {
							byte[] bytes = Files.readAllBytes(path);
							String contentType = "application/octet-stream";
							Path mimeFile = dir.resolve(path.getFileName().toString() + ".mime");
							if (Files.exists(mimeFile)) {
								contentType = Files.readString(mimeFile);
							}
							return new Attachment(UUID.fromString(path.getFileName().toString()), bytes, contentType);
						} catch (IOException e) {
							throw new AzertioException(e, "Failed to read attachment {}", path.getFileName());
						}
					});
			} catch (IOException e) {
				throw new AzertioException(e, "Failed to list attachments for execution {} node {}", executionID, executionNodeID);
			}
		} else {
			return java.util.stream.Stream.empty();
		}
	}

	@Override
	public void deleteAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID) {
		Path dir = basePath.resolve(executionID.toString()).resolve(executionNodeID.toString());
		Path file = dir.resolve(attachmentID.toString());
		try {
			Files.deleteIfExists(file);
			Files.deleteIfExists(dir.resolve(attachmentID + ".mime"));
		} catch (IOException e) {
			throw new AzertioException(e, "Failed to delete attachment {}", attachmentID);
		}
	}

	@Override
	public void deleteAttachments(UUID executionID) {
		Path dir = basePath.resolve(executionID.toString());
		if (Files.exists(dir) && Files.isDirectory(dir)) {
			try (var list = Files.walk(dir)) {
				list.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException e) {
						throw new AzertioException(e, "Failed to delete attachment file {}", path);
					}
				});
			} catch (IOException e) {
				throw new AzertioException(e, "Failed to delete attachments for execution {}", executionID);
			}
		}
	}

}
