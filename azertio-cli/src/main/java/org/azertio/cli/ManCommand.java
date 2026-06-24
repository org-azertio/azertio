package org.azertio.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.contributors.HelpProvider;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

@CommandLine.Command(
    name = "man",
    description = "Show available help topics or the content of a specific topic"
)
public final class ManCommand extends AbstractCommand {

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        paramLabel = "TOPIC",
        description = "Help topic ID (omit to list all topics)"
    )
    String topic;

    @CommandLine.Option(
        names = {"--json"},
        description = "Output as JSON",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        AzertioRuntime runtime = new AzertioRuntime(getContext().configuration());
        List<HelpProvider> providers = runtime.getExtensions(HelpProvider.class).toList();

        if (topic == null) {
            listTopics(providers);
        } else {
            showTopic(providers, topic);
        }
    }

    private void listTopics(List<HelpProvider> providers) {
        if (json) {
            JsonArray arr = new JsonArray();
            for (HelpProvider p : providers) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", p.id());
                obj.addProperty("displayName", p.displayName());
                arr.add(obj);
            }
            out().println(arr);
        } else {
            if (providers.isEmpty()) {
                out().println("No help topics available.");
            } else {
                out().println("Available topics:");
                out().println();
                for (HelpProvider p : providers) {
                    out().printf("  %-30s %s%n", p.id(), p.displayName());
                }
            }
        }
    }

    private void showTopic(List<HelpProvider> providers, String id) {
        Optional<HelpProvider> match = providers.stream()
            .filter(p -> p.id().equals(id))
            .findFirst();

        if (match.isEmpty()) {
            err().println("Unknown topic: " + id);
            exitCode = 1;
            return;
        }

        HelpProvider provider = match.get();
        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", provider.id());
            obj.addProperty("displayName", provider.displayName());
            obj.addProperty("content", provider.help());
            out().println(obj);
        } else {
            out().println(provider.help());
        }
    }
}