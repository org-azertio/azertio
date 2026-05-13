package org.myjtools.openbbt.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

@CommandLine.Command(
    name = "list-contributors",
    description = "List all registered contributors organized by type"
)
public final class ListContributorsCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--json"},
        description = "Output as JSON array",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        OpenBBTRuntime runtime = new OpenBBTRuntime(getContext().configuration());
        Map<String, Map<String, List<String>>> contributors = runtime.getContributors();

        if (json) {
            JsonArray result = new JsonArray();
            contributors.forEach((module, byType) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("plugin", module);
                JsonArray types = new JsonArray();
                byType.forEach((type, impls) -> {
                    JsonObject typeObj = new JsonObject();
                    typeObj.addProperty("type", type);
                    JsonArray implsArr = new JsonArray();
                    impls.forEach(implsArr::add);
                    typeObj.add("implementations", implsArr);
                    types.add(typeObj);
                });
                obj.add("contributors", types);
                result.add(obj);
            });
            System.out.println(result);
        } else {
            contributors.forEach((module, byType) -> {
                System.out.println(module);
                byType.forEach((type, impls) ->
                    impls.forEach(impl -> System.out.println("  [" + type + "] " + impl))
                );
            });
        }
    }
}