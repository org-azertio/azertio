package org.azertio.persistence.test;

import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioException;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.persistence.DefaultRepositoryFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRepositoryFactoryTest {

    private DefaultRepositoryFactory factory(Map<String, String> params) throws Exception {
        DefaultRepositoryFactory f = new DefaultRepositoryFactory();
        Field field = DefaultRepositoryFactory.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(f, Config.ofMap(params));
        return f;
    }

    @Test
    void createRepository_transient_testPlanRepository() throws Exception {
        TestPlanRepository repo = factory(Map.of(
            AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
        )).createRepository(TestPlanRepository.class);
        assertThat(repo).isNotNull();
    }

    @Test
    void createRepository_transient_attachmentRepository() throws Exception {
        AttachmentRepository repo = factory(Map.of(
            AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
        )).createRepository(AttachmentRepository.class);
        assertThat(repo).isNotNull();
    }

    @Test
    void createRepository_file_testPlanRepository(@TempDir Path tempDir) throws Exception {
        TestPlanRepository repo = factory(Map.of(
            AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE,
            AzertioConfig.ENV_PATH, tempDir.toString(),
            AzertioConfig.PERSISTENCE_FILE, "db/azertio.db"
        )).createRepository(TestPlanRepository.class);
        assertThat(repo).isNotNull();
    }

    @Test
    void createRepository_file_attachmentRepository(@TempDir Path tempDir) throws Exception {
        AttachmentRepository repo = factory(Map.of(
            AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE,
            AzertioConfig.ENV_PATH, tempDir.toString()
        )).createRepository(AttachmentRepository.class);
        assertThat(repo).isNotNull();
    }

    @Test
    void createRepository_unsupportedMode_throwsException() throws Exception {
        assertThatThrownBy(() ->
            factory(Map.of(
                AzertioConfig.PERSISTENCE_MODE, "unsupported-mode"
            )).createRepository(TestPlanRepository.class)
        ).isInstanceOf(AzertioException.class);
    }
}