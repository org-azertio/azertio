module org.azertio.plugins.markdownplan {
	requires org.commonmark;
	requires org.commonmark.ext.gfm.tables;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten;
	requires org.azertio.core;
	exports org.azertio.plugins.markdownplan;
	provides org.azertio.core.contributors.SuiteAssembler with org.azertio.plugins.markdownplan.MarkdownSuiteAssembler;
	opens org.azertio.plugins.markdownplan to org.myjtools.jexten;
}