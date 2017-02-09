package com.baloise.repocleaner;

import static java.lang.String.format;
import static java.nio.file.Files.createTempDirectory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class RepoCleanerCLI {

	@Parameter(description = "<the git URLs to clone and clean including credentials. file:// URLs will not be cloned>")
	private List<URL> repoURLs = new ArrayList<>();

	@Parameter(names = { "--help", "-help", "-h" }, description = "display usage", help = true)
	private boolean help;
	
	@Parameter(names = { "--workingCopy", "-wc" }, description = "working copy only. If no  URL is given use current directory. Do not switch branches. No commit.")
	private boolean workingCopy;
	
	@Parameter(names = { "--workingCopyCommit", "-wcc" }, description = "working copy only. If no  URL is given use current directory. Do not switch branches but commit.")
	private boolean workingCopyCommit;
	
	@Parameter(names = { "-files", "-f" }, description = "comma seperated file or directory names to remove")
	private String files = ".classpath,.cvsignore,.project,.settings,*.iml,.fbwarnings,bin,classes,target";

	static RepoCleanerCLI cli = new RepoCleanerCLI();
	public static void main(String[] args) throws IOException {
		JCommander jCommander = new JCommander(cli, args);
		jCommander.setProgramName("repoCleaner");
		if(cli.workingCopyCommit) cli.workingCopy = true;
		if(cli.workingCopy) {
			if(cli.repoURLs.isEmpty()) cli.repoURLs.add(new File(".").toURI().toURL());
			cli.repoURLs.stream()
			.map(RepoCleanerCLI::path)
			.forEach(p-> {
				try {
					LOG.info("cleaning "+p);
					new RepoCleaner(p).clean(cli.files.split(","));
					if(cli.workingCopyCommit) {
						LOG.info("committing");
						new GitHelper(p).commit("CLEAN "+cli.files);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} else if (cli.help || cli.repoURLs.isEmpty()) {
			jCommander.usage();
			return;
		} else {
			cli.repoURLs.stream().forEach(RepoCleanerCLI::clean);
		}
	}

	private static void clean(URL repo) {
		try {
			Path local = repo.getProtocol().equalsIgnoreCase("file") ? path(repo) : createTempDirectory("repoCleaner"+repo.getPath().toString().replaceAll("\\W+", "-"));
			GitHelper gitHelper = new GitHelper(local);
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
					new RepoCleaner(local).clean(cli.files.split(","));
					LOG.info("committing "+branch);
					gitHelper.commit("CLEAN "+cli.files);
				} catch (IOException e) {
					LOG.exception("cleaning", local, e);
				}
				
			});
			if(branches.contains("origin/master")) {
				gitHelper.switchBranch("master");
			}
			Desktop.getDesktop().open(local.toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	protected static Path path(URL repo) {
		return new File(repo.getPath()).toPath();
	}
}
