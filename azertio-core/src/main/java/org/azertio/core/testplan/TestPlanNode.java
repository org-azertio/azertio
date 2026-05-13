package org.azertio.core.testplan;


import java.util.UUID;
import lombok.*;
import java.util.*;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode
public class TestPlanNode {

	private UUID nodeID;
	private NodeType nodeType;
	private String name;
	private String language;
	private String identifier;
	private String source;
	private String keyword;
	private DataTable dataTable;
	private Document document;
	private String description;
	private Set<String> tags;
	private SortedMap<String,String> properties;
	private String display;
	private ValidationStatus validationStatus;
	private String validationMessage;
	private boolean hasIssues;
	private Integer testCaseCount;


	public TestPlanNode(NodeType type) {
		this.nodeType = type;
	}


	public TestPlanNode addProperties(Map<String, String> properties) {
		this.properties().putAll(properties);
		return this;
	}


	public TestPlanNode addProperty(String key, String value) {
		this.properties().put(key,value);
		return this;
	}



	public TestPlanNode addTag(String tag) {
		this.tags().add(tag);
		return this;
	}


	public TestPlanNode addTags(Collection<String> tags) {
		this.tags().addAll(tags);
		return this;
	}


	public boolean hasProperty(String property, String value) {
		return value.equals(this.properties().get(property));
	}


	public boolean hasTag(String tag) {
		return tags().contains(tag);
	}





	public SortedMap<String, String> properties() {
		if (properties == null) {
			properties = new TreeMap<>();
		}
		return properties;
	}


	public Set<String> tags() {
		if (tags == null) {
			tags = new HashSet<>();
		}
		return tags;
	}


	@Override
	public String toString() {
		if (display == null) {
			return name != null ? name : nodeType.name();
		}
		return display
			.replace("{name}", name)
			.replace("{nodeType}", nodeType.name())
			.replace("{nodeID}", nodeID != null ? nodeID.toString() : "null")
			.replace("{language}", language != null ? language : "null")
			.replace("{identifier}", identifier != null ? identifier : "null")
			.replace("{source}", source != null ? source : "null")
			.replace("{keyword}", keyword != null ? keyword : "null")
			.replace("{description}", description != null ? description : "null");
	}

}
