package org.smartrplace.tools.alarmconfig.codes.parser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.smartrplace.tools.alarmconfig.codes.parser.CodesParser.FinalAnalysis;

public class ClassRenderer {

	private final Stream<FinalAnalysis> nodes;
	private final Writer writer;
	private final static String INDENT = "    ";
	
	public ClassRenderer(final Stream<FinalAnalysis> nodes, final Writer writer) {
		this.nodes = nodes;
		this.writer = writer;
	}
	
	void render() throws IOException {
		writer.write("package org.smartrplace.apps.alarmingconfig.release;\n"
				+ "\n"
				+ "public enum FinalAnalysis {\n\n");
		final AtomicBoolean isFirst = new AtomicBoolean(true);
		try {
			nodes.forEach(node -> {
				try {
					if (isFirst.get())
						isFirst.set(false);
					else
						writer.write(",\n");
					writer.write(INDENT);
					writer.write(node.code.replace('.', '_'));
					writer.write("(\"");
					writer.write(node.code);
					writer.write("\", \"");
					writer.write(node.category);
					writer.write("\", \"");
					writer.write(node.description);
					writer.write("\")");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		writer.write(";\n\n");
		writer.write(INDENT);
		writer.write("private final String code;\n"
				+ "	private final String category;\n"
				+ "	private final String description;\n"
				+ "	\n"
				+ "	private FinalAnalysis(String code, String category, String description) {\n"
				+ "		this.code=code;\n"
				+ "		this.category=category;\n"
				+ "		this.description=description;\n"
				+ "	}\n"
				+ "	\n"
				+ "	public String code() { \n"
				+ "		return code;\n"
				+ "	}\n"
				+ "	\n"
				+ "	public String description() {\n"
				+ "		return description;\n"
				+ "	}\n"
				+ "	\n"
				+ "	public String category() {\n"
				+ "		return category;\n"
				+ "	}\n"
				+ "	\n"
				+ "\n"
				+ "}".replace("\t", INDENT));
		
	}
	
	
}
