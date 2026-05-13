package org.azertio.persistence.attachment;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.azertio.core.AzertioException;
import org.azertio.core.persistence.AttachmentRepository;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MinioAttachmentRepository implements AttachmentRepository {

	private static final String BUCKET = "azertio-attachments";

	private final MinioClient minioClient;

	public MinioAttachmentRepository(String url, String accessKey, String secretKey) {
		this.minioClient = MinioClient.builder()
			.endpoint(url)
			.credentials(accessKey, secretKey)
			.build();
		ensureBucket();
	}

	@Override
	public void storeAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID, byte[] bytes, String contentType) {
		try {
			minioClient.putObject(PutObjectArgs.builder()
				.bucket(BUCKET)
				.object(executionID + "/" + executionNodeID + "/" + attachmentID)
				.stream(new ByteArrayInputStream(bytes), bytes.length, -1)
				.contentType(contentType != null ? contentType : "application/octet-stream")
				.build());
		} catch (Exception e) {
			throw new AzertioException(e, "Failed to store attachment {} in Minio", attachmentID);
		}
	}


	@Override
	public Optional<Attachment> retrieveAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID) {
		try (var response = minioClient.getObject(
			GetObjectArgs.builder()
					.bucket(BUCKET)
					.object(executionID + "/" + executionNodeID + "/" + attachmentID)
					.build()
		)) {
			return Optional.of(new Attachment(
					attachmentID,
					response.readAllBytes(),
					response.headers().get("Content-Type")
			));
		} catch (Exception e) {
			if (e instanceof ErrorResponseException errorResponseException
					&& "NoSuchKey".equals(errorResponseException.errorResponse().code())) {
				return Optional.empty();
			}
			throw new AzertioException(e, "Failed to retrieve attachment {} from Minio", attachmentID);
		}
	}


	@Override
	public Stream<Attachment> streamAttachments(UUID executionID, UUID executionNodeID) {

		String prefix = executionID + "/" + executionNodeID + "/";

		Iterable<Result<Item>> results = minioClient.listObjects(
			ListObjectsArgs.builder()
				.bucket(BUCKET)
				.prefix(prefix)
				.recursive(true)
				.build()
		);

		return StreamSupport.stream(results.spliterator(), false)
				.map(result -> {
					try {
						Item item = result.get();
						UUID attachmentID = UUID.fromString(
								item.objectName().substring(prefix.length())
						);
						try (var response = minioClient.getObject(
							GetObjectArgs.builder()
									.bucket(BUCKET)
									.object(item.objectName())
									.build()
						)) {
							byte[] data = response.readAllBytes();
							String contentType = response.headers().get("Content-Type");
							return new Attachment(attachmentID, data, contentType);
						}

					} catch (Exception e) {
						throw new AzertioException(e, "Failed to stream attachment from Minio");
					}
				});
	}


	@Override
	public void deleteAttachment(UUID executionID, UUID executionNodeID, UUID attachmentID) {
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
				.bucket(BUCKET)
				.object(executionID + "/" + executionNodeID + "/" + attachmentID)
				.build());
		} catch (Exception e) {
			throw new AzertioException(e, "Failed to delete attachment {} from Minio", attachmentID);
		}
	}

	@Override
	public void deleteAttachments(UUID executionID) {
		String prefix = executionID + "/";
		try {
			Iterable<Result<Item>> results = minioClient.listObjects(
				ListObjectsArgs.builder()
					.bucket(BUCKET)
					.prefix(prefix)
					.recursive(true)
					.build()
			);
			results.forEach(result -> {
				try {
					Item item = result.get();
					minioClient.removeObject(RemoveObjectArgs.builder()
						.bucket(BUCKET)
						.object(item.objectName())
						.build());
				} catch (Exception e) {
					throw new AzertioException(e, "Failed to delete attachment from Minio");
				}
			});
		} catch (Exception e) {
			throw new AzertioException(e, "Failed to delete attachments for execution {} from Minio", executionID);
		}
	}


	private void ensureBucket() {
		try {
			boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
			if (!exists) {
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
			}
		} catch (Exception e) {
			throw new AzertioException(e, "Failed to ensure Minio bucket '{}'", BUCKET);
		}
	}

}
