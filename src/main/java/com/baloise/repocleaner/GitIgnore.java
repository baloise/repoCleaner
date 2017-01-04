package com.baloise.repocleaner;

import static java.lang.String.join;
import static java.nio.file.Files.lines;
import static java.util.Arrays.stream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
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
		if (gitIgnore.toFile().exists()) {
			try (Stream<String> lines = lines(gitIgnore)) {
				return lines.collect(Collectors.<String>toSet());
			} 
			catch (IOException e) {
				LOG.exception("reading", gitIgnore, e);
			}
		}
		return Collections.<String>emptySet();
	}

	public void add(Stream<String> ignores) {
		Set<String> existing = new TreeSet<>(list());
		final int originalSize = existing.size();
		ignores.forEach(existing::add);
		if (existing.size() > originalSize) {
			LOG.change("writing " + gitIgnore);
			try (FileOutputStream fos = new FileOutputStream(gitIgnore.toFile())) {
				fos.write(join("\n", existing).getBytes());
			} catch (IOException e) {
				LOG.exception("writing", gitIgnore, e);
			}
		}
	}

	public void add(String... ignores) {
		add(stream(ignores));
	}

	@Override
	public String toString() {
		return gitIgnore.toString();
	}

}
