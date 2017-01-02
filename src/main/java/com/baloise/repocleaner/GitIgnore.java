package com.baloise.repocleaner;

import static java.lang.String.join;
import static java.nio.file.Files.lines;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Arrays.stream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitIgnore {

	private Path gitIgnore;

	public GitIgnore(Path repoRoot) {
		gitIgnore = repoRoot.resolve(".gitignore");
	}

	public Set<String> list() {
		try {
			return lines(gitIgnore).collect(Collectors.<String>toSet());
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.<String>emptySet();
		}
	}

	public void add(Stream<String> ignores) {
		Set<String> existing = new TreeSet<>(list());
		ignores.forEach(existing::add);
		try (BufferedWriter writer = newBufferedWriter(gitIgnore, StandardOpenOption.CREATE)){
			writer.write(join("\n", existing));
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public void add(String ...ignores ) {
		add(stream(ignores));
	}
	
	@Override
	public String toString() {
		return gitIgnore.toString();
	}

}
