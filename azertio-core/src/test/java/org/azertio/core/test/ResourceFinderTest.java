package org.azertio.core.test;

import org.junit.jupiter.api.Test;
import org.azertio.core.Resource;
import org.azertio.core.ResourceFinder;
import java.io.IOException;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceFinderTest {

	@Test
	void testFindResources() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithOrPattern() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*.{txt,yml}");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/file_c.yml"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithRecursiveGlob() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithSubdirGlob() {
		// **/subdir/*.txt only matches files inside the subdir directory
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/subdir/*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithAllFilesGlob() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/file_c.yml"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testResourceSetHash() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet1 = resourceFinder.findResources("**/*.txt");
		var resourceSet2 = resourceFinder.findResources("**/*.txt");
		var resourceSet3 = resourceFinder.findResources("**/*.yml");
		assertThat(resourceSet1.hash()).isEqualTo(resourceSet2.hash());
		assertThat(resourceSet1.hash()).isNotEqualTo(resourceSet3.hash());
	}

	@Test
	void testResourceSetFilter_withRecursiveGlob_matchesAllTxtFiles() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.filter("**/*.txt").map(Resource::relativePath).toList())
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testResourceSetFilter_withSubdirGlob_matchesOnlySubdirFiles() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.filter("**/subdir/*.txt").map(Resource::relativePath).toList())
			.containsExactly(
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResources_withSimpleGlobPattern_matchesWithoutDoubleAsterisk() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testResourceSetFilter_withPredicate_matchesByExtension() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.filter(r -> "yml".equals(r.extension())).map(Resource::relativePath).toList())
			.containsExactly(
				Path.of("src/test/resources/files/file_c.yml")
			);
	}

	@Test
	void testResolveString() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		assertThat(resourceFinder.resolve("file_a.txt"))
			.isEqualTo(Path.of("src/test/resources/files/file_a.txt"));
	}

	@Test
	void testResolvePath() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		assertThat(resourceFinder.resolve(Path.of("file_a.txt")))
			.isEqualTo(Path.of("src/test/resources/files/file_a.txt"));
	}

	@Test
	void testReadAsString() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		assertThat(resourceFinder.readAsString("file_a.txt")).isEqualTo("ffgdsfsdfagaffgasfd");
	}

	@Test
	void testResourceInputStream() throws IOException {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resource = resourceFinder.findResources("file_a.txt").resources().get(0);
		try (var input = resource.open()) {
			assertThat(input).isNotNull();
			assertThat(new String(input.readAllBytes())).isEqualTo("ffgdsfsdfagaffgasfd");
		}
	}

}
