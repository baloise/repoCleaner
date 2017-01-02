package com.baloise.repocleaner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.junit.Test;

public class RepoCleanerTest {


	@Test
	public void predicate() throws Exception {
		Predicate<Path> pred = RepoCleaner.matcher(".project", ".cvsignore", ".settings", "*.iml");
		assertTrue(pred.test(Paths.get(".project")));
		assertTrue(pred.test(Paths.get(".settings")));
		assertTrue(pred.test(Paths.get("test.iml")));
		assertFalse(pred.test(Paths.get("test.iml2")));
	}
}
