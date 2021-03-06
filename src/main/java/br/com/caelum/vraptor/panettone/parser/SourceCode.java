package br.com.caelum.vraptor.panettone.parser;

import static br.com.caelum.vraptor.panettone.parser.Regexes.RULECHUNK_START_REGEX;
import static br.com.caelum.vraptor.panettone.parser.Tokens.RULECHUNK_END;
import static br.com.caelum.vraptor.panettone.parser.Tokens.RULECHUNK_START;
import static br.com.caelum.vraptor.panettone.parser.Tokens.SCRIPTLET_END;
import static br.com.caelum.vraptor.panettone.parser.Tokens.SCRIPTLET_START;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.caelum.vraptor.panettone.parser.rule.Rules;

public class SourceCode {

	private String source;
	private final String immutableSource;
	private Map<Integer, TextChunk> extractedChunks;
	private int counter = 0;
	private List<Integer> lines;

	public SourceCode(String source) {
		this.source = source;
		immutableSource = source;
		
		countLines();
		extractedChunks = new HashMap<>();
	}
	
	private void countLines() {
		lines = new ArrayList<>();
		lines.add(0);
		for(int i = 0; i < immutableSource.length(); i++) {
			if(immutableSource.charAt(i) == '\n') lines.add(i);
		}
	}

	public String getSource() {
		return source;
	}

	public void transform(TextChunk chunk, Rules aRule) {
		addChunk(chunk);
		source = source.replace(chunk.getText(), 
				RULECHUNK_START + " " + aRule.name() + " " + counter + " " + RULECHUNK_END);
	}
	
	public TextChunk getTextChunk(int number) {
		return extractedChunks.get(number);
	}

	
	public void transformHtmlAndScriptlet() {
		StringBuilder newSourceCode = new StringBuilder();
		
		String[] lines = source.split(RULECHUNK_START_REGEX);
		
		for(String line : lines) {
			if(line.trim().isEmpty()) continue;
			
			if(line.trim().endsWith(RULECHUNK_END)) {
				newSourceCode.append(RULECHUNK_START + " " + line.trim());
			} 
			else if(line.trim().contains(RULECHUNK_END)) {
				String firstPart = line.substring(0, line.indexOf(RULECHUNK_END)).trim();
				newSourceCode.append(RULECHUNK_START + " " + firstPart + " " + RULECHUNK_END);

				String secondPart = line.substring(line.indexOf(RULECHUNK_END)+4);
				htmlOrScriptlet(newSourceCode, secondPart);

			}
			else {
				htmlOrScriptlet(newSourceCode, line);
			}
		}

		source = newSourceCode.toString();
	}

	private void htmlOrScriptlet(StringBuilder newSourceCode, String chunk) {
		
		if(chunk.trim().startsWith(SCRIPTLET_START)) {
			
			String justScriptlet = chunk.trim().substring(2);
			int endOfTheScriptlet = justScriptlet.indexOf(SCRIPTLET_END); // be careful, an "%>" would break it
			justScriptlet = justScriptlet.substring(0, endOfTheScriptlet);

			addChunk(new TextChunk(justScriptlet, lineNumberFor(immutableSource.indexOf(justScriptlet))));
			newSourceCode.append(RULECHUNK_START + " " + Rules.scriptletRuleName() + " " + counter + " " + RULECHUNK_END);
			
			if(chunk.length() > chunk.indexOf(SCRIPTLET_END) +2) {
				String theRestOfTheChunk = chunk.substring(chunk.indexOf(SCRIPTLET_END)+2);
				htmlOrScriptlet(newSourceCode, theRestOfTheChunk);
			}
		} else {

			String justHTML = chunk;
			int startOfScriptlet = justHTML.indexOf(SCRIPTLET_START);
			justHTML = justHTML.substring(0, startOfScriptlet == -1 ? justHTML.length() : startOfScriptlet);
			
			addChunk(new TextChunk(justHTML, lineNumberFor(immutableSource.indexOf(justHTML))));
			newSourceCode.append(RULECHUNK_START + " " + Rules.htmlRuleName() + " " + counter + " " + RULECHUNK_END);
			
			if(startOfScriptlet > -1) {
				String theRestOfTheChunk = chunk.substring(chunk.indexOf(SCRIPTLET_START));
				htmlOrScriptlet(newSourceCode, theRestOfTheChunk);
			}
		}
	}
	
	private void addChunk(TextChunk chunk) {
		counter++;
		extractedChunks.put(counter, chunk);
	}

	public int lineNumberFor(int start) {
		for(int i = 0; i < lines.size(); i++) {
			if(lines.get(i) > start) return i;
			if(lines.get(i) == start) return i+1;
		}
		
		return lines.size();
	}
}
