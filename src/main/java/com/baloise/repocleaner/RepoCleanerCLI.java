package com.baloise.repocleaner;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class RepoCleanerCLI {

	@Parameter(description = "<the git URLs to clone and clean including credentials>")
	private List<URL> repoURLs = new ArrayList<>();

	@Parameter(names = { "--help", "-help", "/?" }, description = "display usage", help = true)
	private boolean help;
	
	@Parameter(names = { "-files", "-f" }, description = "comma seperated file or directory names to remove")
	private String files = ".project,.cvsignore,.settings,*.iml,.fbwarnings,bin,classes,target";

	static RepoCleanerCLI cli = new RepoCleanerCLI();
	public static void main(String[] args) {
		JCommander jCommander = new JCommander(cli, args);
		jCommander.setProgramName("repoCleaner");
		if (cli.help) {
			jCommander.usage();
			return;
		}
		cli.repoURLs.stream().forEach(RepoCleanerCLI::clean);
	}

	private static void clean(URL repo) {
		try {
			Path tmp = Files.createTempDirectory("repoCleaner"+repo.getPath().toString().replaceAll("\\W+", "-"));
//			Path tmp = Paths.get("C:\\Windows\\TEMP\\repoCleaner-https-vcs-balgroupit-com-git-targetcomponent-parsysinterface-git7328709501989105481");
			GitHelper gitHelper = new GitHelper(tmp);
			gitHelper.cloneReop(repo);
			
			gitHelper.getBranches().stream().forEach((branch) -> {
				try {
					gitHelper.switchBranch(branch.trim().replaceFirst("origin/", ""));
					new RepoCleaner(tmp).clean(cli.files.split(","));
					gitHelper.commit("CLEAN "+cli.files);
				} catch (IOException e) {
					LOG.exception("cleaning", tmp, e);
				}
				
			});
			
			Desktop.getDesktop().open(tmp.toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
