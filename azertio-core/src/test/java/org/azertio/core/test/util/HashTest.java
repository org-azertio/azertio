package org.azertio.core.test.util;

import org.junit.jupiter.api.Test;
import org.azertio.core.util.Hash;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class HashTest {

	@Test
	void testHashString() {
		var hash1 = Hash.of("test");
		var hash2 = Hash.of("test");
		var hash3 = Hash.of("different");
		assert hash1.equals(hash2);
		assert !hash1.equals(hash3);
	}

	@Test
	void testHashFile() {
		var hash1 = Hash.of("src/test/resources/files/file_a.txt");
		var hash2 = Hash.of("src/test/resources/files/file_a.txt");
		var hash3 = Hash.of("src/test/resources/files/file_b.txt");
		assert hash1.equals(hash2);
		assert !hash1.equals(hash3);
	}

	@Test
	void testHashSinglePath() {
		var path = Path.of("src/test/resources/files/file_a.txt");
		var hash = Hash.of(path);
		assertThat(hash).isNotBlank();
		assertThat(hash).isEqualTo(Hash.of(List.of(path)));
	}

	@Test
	void testHashPathCollection() {
		var files1 = List.of(
			Path.of("src/test/resources/files/file_a.txt"),
			Path.of("src/test/resources/files/file_b.txt")
		);
		var files2 = List.of(
			Path.of("src/test/resources/files/file_a.txt")
		);
		var hash1 = Hash.of(files1);
		var hash2 = Hash.of(files1);
		var hash3 = Hash.of(files2);
		assert hash1.equals(hash2);
		assert !hash1.equals(hash3);
	}
}
