package org.azertio.core.test.backend;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.messages.MessageAdapter;
import org.azertio.core.messages.MessageProvider;

@Extension(scope = Scope.SINGLETON)
public class TestStepProviderMessageProvider extends MessageAdapter implements MessageProvider  {


	public TestStepProviderMessageProvider() {
		super(TestStepProvider.class.getSimpleName());
	}

}
