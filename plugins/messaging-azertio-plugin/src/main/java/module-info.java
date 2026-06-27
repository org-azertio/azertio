import org.azertio.plugins.messaging.MessagingAIIndexProvider;
import org.azertio.plugins.messaging.MessagingConfigHelpProvider;
import org.azertio.plugins.messaging.MessagingConfigProvider;
import org.azertio.plugins.messaging.MessagingMessageProvider;
import org.azertio.plugins.messaging.MessagingStepHelpProvider;
import org.azertio.plugins.messaging.MessagingStepProvider;

module org.azertio.plugins.messaging {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;
    requires jakarta.messaging;

    provides org.azertio.core.contributors.StepProvider with MessagingStepProvider;
    provides org.azertio.core.contributors.ConfigProvider with MessagingConfigProvider;
    provides org.azertio.core.messages.MessageProvider with MessagingMessageProvider;
    provides org.azertio.core.contributors.AIIndexProvider with MessagingAIIndexProvider;
    provides org.azertio.core.contributors.HelpProvider with MessagingStepHelpProvider, MessagingConfigHelpProvider;

    exports org.azertio.plugins.messaging;
    opens org.azertio.plugins.messaging to org.myjtools.jexten;

}