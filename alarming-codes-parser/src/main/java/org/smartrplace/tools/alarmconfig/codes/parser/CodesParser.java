package org.smartrplace.tools.alarmconfig.codes.parser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Parses Incidents file from Smartrplace Wiki: https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation/Incidents_Operation
 * This file needs to be copied to data/incidents.md in this project. 
 */
public class CodesParser {
	
	public static void main(String[] args) throws IOException {
		
		try (final InputStream stream = new BufferedInputStream(Files.newInputStream(Path.of("./data/incidents.md"), StandardOpenOption.READ));
				final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			
			try (final OutputStream out = new BufferedOutputStream(Files.newOutputStream(Path.of("./data/FinalAnalysis.java"), StandardOpenOption.CREATE));
					final Writer writer = new OutputStreamWriter(out)) {
				final Stream<FinalAnalysis> codes = new CodesParser(reader).parse().stream();
				final ClassRenderer renderer = new ClassRenderer(codes, writer);
				renderer.render();
			}
		}
	}

	private final Document doc;
	// state
	private String currentCategory = null;
	private String currentPendingCode = null;
	private int currentCodeLevel = -1;
	private final List<FinalAnalysis> codes = new ArrayList<>();
	
	
	public CodesParser(Reader input) throws IOException {
		final MutableDataSet options = new MutableDataSet();
		final Parser parser = Parser.builder(options).build();
		this.doc = parser.parseReader(input);
	}
	
	public List<FinalAnalysis> parse() {
		final NodeVisitor visitor = new NodeVisitor(
	            new VisitHandler<>(Heading.class, this::visitHeader)
	            //new VisitHandler<>(Text.class, t -> System.out.println("NEW text "+  t.getChars()) )
	    );
		visitor.visit(doc);
		//System.out.println("   ALL codes " + codes.stream().map(c -> c.code).collect(Collectors.toList()));
		return this.codes;
	}

	
	private void visitHeader(Heading heading)  {
		final int level = heading.getLevel();
		if (level < 2)
			return;
		final String header = parseHeading(heading.getChars().normalizeEndWithEOL());
		if (level == 2) {
			this.currentCategory = header;
			return;
		}
		if (currentPendingCode == null && header.startsWith("PN")) {
			currentCodeLevel = level;
			currentPendingCode = header;
		} else {
			if (level == currentCodeLevel) {
				final boolean isDuplicate = this.codes.stream().filter(c -> c.code.equals(currentPendingCode)).findAny().isPresent();
				if (isDuplicate)
					System.out.println("Duplicate code " + currentCodeLevel + ": " + header);
				else
					codes.add(new FinalAnalysis(currentPendingCode, currentCategory, header));
			}
			currentPendingCode = null;
			currentCodeLevel = -1;
		}
		
	}
	
	
	private static String parseHeading(String h) {
		int start = 0;
		for (int idx=0; idx<h.length();idx++) {
			final char c = h.charAt(idx);
			if (c != '#')
				break;
			start = idx+1;
		}

		return h.substring(start).trim().replace("\"", "\\\"");
	}

	
	static class FinalAnalysis {
		
		public FinalAnalysis(String code, String category, String description) {
			this.code = code;
			this.category = category;
			this.description = description;
		}
		final String code;
		final String category;
		final String description;
		
		
	}
	
}
