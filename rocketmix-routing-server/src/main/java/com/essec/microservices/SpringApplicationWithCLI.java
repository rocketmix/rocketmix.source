package com.essec.microservices;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;

import com.essec.microservices.InstallScriptGenerator.InstallScriptParameters;


public class SpringApplicationWithCLI {

	private static Options options = new Options();
	private static HelpFormatter formatter = new HelpFormatter();
	
	
	public static void run(Class<?> springBootApplicationClazz, String... args) {
		init();
		// create the parser
		CommandLineParser parser = new DefaultParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("help")) {
				formatter.printHelp(" ", options);
				return;
			}
			if (line.hasOption("install")) {
				InstallScriptParameters params = buildInstallerParams(line);
				InstallScriptGenerator scriptGenerator = new InstallScriptGenerator();
				scriptGenerator.generateAll(params);
				return;
			}
			SpringApplication.run(springBootApplicationClazz, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp(" ", options);
			
		}
		
	}


	public static void init() {
		options = new Options();
		options.addOption("?", "help", false, "Display this help");
		options.addOption(null, "companyName", true, "Override de company name display on the API Portal");
		options.addOption(null, "logoURL", true, "Set a logo displayed on the API Portal instead of the rocket logo. Should be a transparent PNG. URL must be absolute. Ex : http://www.acme.com/static/logo.png");
		options.addOption(null, "port", true, "Change HTTP port (default: 8080)");
		options.addOption(null, "managementServerURL", true, "Set URL (with network port) of the management server (default: http://127.0.0.1:8761, used when management server is executed on the same machine)");
		options.addOption(Option.builder().argName("install").desc("Generate install scripts to deploy/undeploy as Linux SystemV service. You need to provide which Linux user/group will be used to run the service. You can combine this option with other options").longOpt("install").hasArgs().optionalArg(true).numberOfArgs(2).argName("user:group").valueSeparator(':').build());
	}
	
	private static InstallScriptParameters buildInstallerParams(CommandLine line) {
		if (!line.hasOption("install")) {
			throw new RuntimeException("Unable to read --install params");
		}
		InstallScriptParameters result = new InstallScriptGenerator.InstallScriptParameters();
		String[] userParams = line.getOptionValues("install");
		if (userParams == null || (userParams != null && userParams.length != 2)) {
			String username = System.getProperty("user.name");
			result.setUser(username);
			result.setGroup(username);
		}
		if (userParams != null &&  userParams.length == 2) {
			result.setUser(userParams[0]);
			result.setGroup(userParams[1]);
		}
		if (line.hasOption("companyName")) {
			result.setCompanyName(line.getOptionValue("companyName"));
		}
		if (line.hasOption("logoURL")) {
			result.setLogoURL(line.getOptionValue("logoURL"));
		}
		if (line.hasOption("managementServerURL")) {
			result.setManagementServerURL(line.getOptionValue("managementServerURL"));
		}
		if (line.hasOption("port")) {
			result.setServerPort(Integer.parseInt(line.getOptionValue("port")));
		}
		return result;
	}
	

}
