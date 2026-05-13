package org.azertio.core.testplan;

import lombok.*;
import java.util.function.UnaryOperator;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public final class Document implements NodeArgument {


	public static Document of(String mimeType, String content) {
		if (content == null || content.isBlank()) {
			return null;
		}
		return new Document(mimeType,content);
	}


	private String mimeType;
	private String content;

	@Override
	public NodeArgument copy(UnaryOperator<String> replacingVariablesMethod) {
		return new Document(mimeType, replacingVariablesMethod.apply(content));
	}


	public String mimeType() {
		return mimeType;
	}

}
