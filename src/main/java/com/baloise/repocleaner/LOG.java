package com.baloise.repocleaner;

import static java.lang.String.format;

import java.nio.file.Path;

public class LOG {
	
	private static boolean changed;

	public static boolean isChanged() {
		return changed;
	}

	public static void change(String message) {
		changed = true;
		info(message);
	}

	public static void exception(String verb, Path p, Exception e) {
		error(format("Exception while %s %s ", verb, p));
		e.printStackTrace();
	}

	public static void error(String message) {
		System.err.println(message);
	}
	
	public static void info(String message) {
		System.out.println(message);
		
	}

}
