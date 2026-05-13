import org.azertio.core.contributors.RepositoryFactory;
import org.azertio.persistence.DefaultRepositoryFactory;

module org.azertio.persistence {
	requires org.azertio.core;
	requires java.sql;
	requires com.github.f4b6a3.ulid;
	requires org.jooq;
	requires org.myjtools.jexten;
	requires com.zaxxer.hikari;
	requires flyway.core;
	requires org.myjtools.imconfig;
	requires org.jspecify;

	exports org.azertio.persistence;

	opens org.azertio.persistence;
	opens org.azertio.persistence.migration.hsqldb;
	opens org.azertio.persistence.migration.postgresql;
	exports org.azertio.persistence.plan;
	opens org.azertio.persistence.plan;
	exports org.azertio.persistence.execution;
	opens org.azertio.persistence.execution;
	exports org.azertio.persistence.attachment;
	opens org.azertio.persistence.attachment;

	requires minio;
	requires okhttp3;

	provides RepositoryFactory
			with DefaultRepositoryFactory;
}
