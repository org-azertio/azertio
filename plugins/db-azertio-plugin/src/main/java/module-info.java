import org.azertio.plugins.db.DbAIIndexProvider;
import org.azertio.plugins.db.DbConfigProvider;
import org.azertio.plugins.db.DbMessageProvider;
import org.azertio.plugins.db.DbStepProvider;

module org.azertio.plugins.db {

	requires org.azertio.core;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires java.sql;
	requires org.jooq;
requires org.apache.poi.ooxml;
	requires org.apache.poi.poi;
	requires org.apache.commons.csv;

	provides org.azertio.core.contributors.StepProvider with DbStepProvider;
	provides org.azertio.core.contributors.ConfigProvider with DbConfigProvider;
	provides org.azertio.core.messages.MessageProvider with DbMessageProvider;
	provides org.azertio.core.contributors.AIIndexProvider with DbAIIndexProvider;

	exports org.azertio.plugins.db;
	opens org.azertio.plugins.db to org.myjtools.jexten;

}