package org.azertio.cli.test;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;

import java.util.Optional;
import java.util.UUID;

@Extension
public class TestSuiteAssembler implements SuiteAssembler {

    @Inject
    TestPlanRepository repository;

    @Override
    public Optional<UUID> assembleSuite(TestSuite testSuite) {
        UUID suite    = node(NodeType.TEST_SUITE,   testSuite.name());
        UUID feature  = node(NodeType.TEST_FEATURE, "feature");
        UUID testCase = node(NodeType.TEST_CASE,    "test case");
        repository.attachChildNodeLast(suite, feature);
        repository.attachChildNodeLast(feature, testCase);
        return Optional.of(suite);
    }

    private UUID node(NodeType type, String name) {
        return repository.persistNode(new TestPlanNode(type).name(name));
    }
}
