package com.baloise.repocleaner;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class RepoCleaner {

	private Path repoRoot;

	public RepoCleaner(Path repoRoot) {
		this.repoRoot = repoRoot;
	}
	
	public void clean(String ... gitIgnoreRegEx) throws IOException {
		LOG.setChanged(false);
		Files.walk(repoRoot).filter(matcher(gitIgnoreRegEx)).
		forEach(RepoCleaner.this::delete);
		
		Files.walk(repoRoot).filter(p -> p.getFileName().toString().equals("pom.xml")).
		forEach(p -> {
			GitIgnore ignore = new GitIgnore(p.getParent());
			ignore.add(stream(gitIgnoreRegEx).map(s -> "/"+s));
			new POMCleaner(p).clean();
		}
		);
		if(LOG.isChanged()) {
			LOG.info("done");
		} else {
			LOG.info("repo was already clean");
		}
	}
	
	private void delete(Path directory) {
		LOG.change("removing "+ directory);
		try {
			Files.walkFileTree(directory, new DeletingVisitor());
		} catch (IOException e) {
			LOG.exception("removing", directory, e);
		}
	}

	public static Predicate<Path> matcher(String ... gitIgnoreRegEx) {
		final String regex = stream(gitIgnoreRegEx).map(RepoCleaner::gitIgnoreToJava).collect(joining("|"));
		return new Predicate<Path>() {
			@Override
			public boolean test(Path path) {
				return path.getFileName().toString().matches(regex);
			}
		};		
	}
	
	public static String gitIgnoreToJava(String regEx) {
		return regEx.replace(".", "\\.").replace("*", ".*");
	}
	

}
