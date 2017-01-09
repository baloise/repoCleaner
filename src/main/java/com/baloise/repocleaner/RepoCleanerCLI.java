package com.baloise.repocleaner;

import static java.lang.String.format;

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

	@Parameter(names = { "--help", "-help", "-h" }, description = "display usage", help = true)
	private boolean help;
	
	@Parameter(names = { "-files", "-f" }, description = "comma seperated file or directory names to remove")
	private String files = ".classpath,.cvsignore,.project,.settings,*.iml,.fbwarnings,bin,classes,target";

	static RepoCleanerCLI cli = new RepoCleanerCLI();
	public static void main(String[] args) {
		JCommander jCommander = new JCommander(cli, args);
		jCommander.setProgramName("repoCleaner");
		if (cli.help || cli.repoURLs.isEmpty()) {
			jCommander.usage();
			return;
		}
		cli.repoURLs.stream().forEach(RepoCleanerCLI::clean);
	}

	private static void clean(URL repo) {
		try {
			Path tmp = Files.createTempDirectory("repoCleaner"+repo.getPath().toString().replaceAll("\\W+", "-"));
			GitHelper gitHelper = new GitHelper(tmp);
			gitHelper.cloneReop(repo);
			List<String> branches = gitHelper.getUnmergedBranches();
			List<String> done = new ArrayList<>(branches.size());			
			branches.stream().forEach((branch) -> {
				try {
					done.add(branch);
					if(branch.contains("->")) {
						LOG.info("skipping " +branch);
						return;
					}
					LOG.info(format("switching to %s. (%s/%s)",branch, done.size(), branches.size()));
					gitHelper.switchBranch(branch.replaceFirst("origin/", ""));
					LOG.info("cleaning "+branch);
					new RepoCleaner(tmp).clean(cli.files.split(","));
					LOG.info("committing "+branch);
					gitHelper.commit("CLEAN "+cli.files);
				} catch (IOException e) {
					LOG.exception("cleaning", tmp, e);
				}
				
			});
			if(branches.contains("origin/master")) {
				gitHelper.switchBranch("master");
			}
			Desktop.getDesktop().open(tmp.toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
