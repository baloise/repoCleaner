package com.baloise.repocleaner;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

public class RepoCleaner {

	private Path repoRoot;

	public RepoCleaner(String[] args) {
		repoRoot = Paths.get("C:\\Users\\Public\\dev\\git\\parsysinterface");
	}

	public static void main(String[] args) throws Exception {
		new RepoCleaner(args).clean(".project", ".cvsignore", ".settings", "*.iml", ".fbwarnings", "bin", "classes", "target");
	}

	private void clean(String ... gitIgnoreRegEx) throws IOException {
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
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				   @Override
				   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					   Files.delete(file);
					   return FileVisitResult.CONTINUE;
				   }

				   @Override
				   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					   Files.delete(dir);
					   return FileVisitResult.CONTINUE;
				   }

			   });
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
