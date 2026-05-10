import org.myjtools.openbbt.plugins.db.DbAIIndexProvider;
import org.myjtools.openbbt.plugins.db.DbConfigProvider;
import org.myjtools.openbbt.plugins.db.DbMessageProvider;
import org.myjtools.openbbt.plugins.db.DbStepProvider;

module org.myjtools.openbbt.plugins.db {

	requires org.myjtools.openbbt.core;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires java.sql;
	requires org.jooq;
requires org.apache.poi.ooxml;
	requires org.apache.poi.poi;
	requires org.apache.commons.csv;

	provides org.myjtools.openbbt.core.contributors.StepProvider with DbStepProvider;
	provides org.myjtools.openbbt.core.contributors.ConfigProvider with DbConfigProvider;
	provides org.myjtools.openbbt.core.messages.MessageProvider with DbMessageProvider;
	provides org.myjtools.openbbt.core.contributors.AIIndexProvider with DbAIIndexProvider;

	exports org.myjtools.openbbt.plugins.db;
	opens org.myjtools.openbbt.plugins.db to org.myjtools.jexten;

}