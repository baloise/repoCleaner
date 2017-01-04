package com.baloise.repocleaner;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GitHelper {

	private Path tmp;

	public GitHelper(Path tmp) {
		this.tmp = tmp;
	}

	public void cloneReop(URL repo) throws IOException {
		nativeGit("clone", repo.toString(), ".");
	}

	public void commit(String message) throws IOException {
		nativeGit("add", "-A");
		nativeGit("commit", "-m", message);
	}

	public List<String> getBranches() throws IOException {
		return nativeGit(true, "branch", "-r").stream().map(String::trim).collect(toList());
	}

	public void switchBranch(String name) throws IOException {
		nativeGit("checkout", name);
	}

	private List<String> nativeGit(String... cmd) throws IOException {
		return nativeGit(false, cmd);
	}
	private List<String> nativeGit(boolean returnOutput, String... cmd) throws IOException {
		List<String> cmds = new ArrayList<>(cmd.length + 2);
		cmds.add("git");
		cmds.addAll(asList(cmd));
		LOG.info(cmds);
		try {
			ProcessBuilder pb = new ProcessBuilder(cmds).directory(tmp.toFile());
			if(!returnOutput) {
				pb.inheritIO();
			}
		    Process process = pb.start();
			process.waitFor();
			return getOutputLines(process);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public static List<String> getOutputLines(Process pr) {
		LinkedList<String> list = new LinkedList<String>();
		String line;
		InputStreamReader tempReader = new InputStreamReader(new BufferedInputStream(pr.getInputStream()));
		BufferedReader oreader = new BufferedReader(tempReader);
		try {
			while ((line = oreader.readLine()) != null) {
				list.add(line);
			}
		} catch (IOException e) {
			System.out.println("Fehler beim Lesen der Ausgabe: \n" + e);
		}
		return list;
	}
}
