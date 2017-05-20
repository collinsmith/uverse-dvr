package com.gmail.collinsmith70.dvr;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
  private static final boolean DBG = true;

  private static final int BUFFER_SIZE = 8192;

  private static final Options OPTIONS = new Options();
  static {
    OPTIONS.addOption("h", "help", false, "Prints this message");
    OPTIONS.addOption("v", "verbose", false, "Outputs additional messages for debugging purposes");
    OPTIONS.addOption("s", "start", true, "The starting file number (in hexadecimal)");
    OPTIONS.addOption("i", true, "The increment on the starting value (in hexadecimal) (Default: 40000000)");
    OPTIONS.addOption(Option.builder("n")
        .hasArg(true)
        .desc("The number of files to generate")
        .build());
  }

  public static void main(String... args) throws ParseException, IOException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cli = parser.parse(OPTIONS, args);

    List<String> argsList = cli.getArgList();
    if (cli.hasOption("h") || argsList.isEmpty()) {
      printHelp();
      System.exit(0);
    } else if (argsList.size() > 2) {

    }

    boolean verbose = cli.hasOption("v");

    Path source = Paths.get(argsList.get(0));
    if (!Files.exists(source)) {
      System.out.printf("Failed to locate \"%s\"%n", source);
      System.out.println("Type \"help\" for more details on usage.");
      System.exit(0);
    }

    Path targetDir;
    if (argsList.size() > 1) {
      targetDir = Paths.get(argsList.get(1));
    } else {
      targetDir = Paths.get(System.getProperty("user.dir"));
    }

    if (!Files.isDirectory(targetDir)) {
      System.out.printf("Failed to locate \"%s\"%n", targetDir);
      System.out.println("Type \"help\" for more details on usage.");
      System.exit(0);
    }

    long start;
    if (!cli.hasOption("start")) {
      start = 0L;
    } else {
      String value = cli.getOptionValue("start");
      if (value.startsWith("0x")) {
        value = value.substring(2);
      }

      start = Long.parseLong(value, 16);
    }

    if (DBG && verbose) {
      System.out.printf("start=%016x%n", start);
    }

    if (!cli.hasOption("n")) {
      System.out.println("The number of files to generate must be specified!");
      System.out.println("Type \"help\" for more details on usage.");
      System.exit(0);
    }

    String value = cli.getOptionValue("i", "40000000");
    if (value.startsWith("0x")) {
      value = value.substring(2);
    }

    long i = Long.parseLong(value, 16);
    long n = Long.parseLong(cli.getOptionValue("n"));

    ProgressBar progressBar = new ProgressBar();
    long nread = 0;
    final long LENGTH = Files.size(source) * n;
    for (int j = 0; j < n; j++) {
      start += i;
      String name = String.format("%016x", start);
      Path target = targetDir.resolve(name + ".slc");

      if (verbose) {
        System.out.println("writing " + target + "...");
        FileInputStream fin = new FileInputStream(source.toFile());
        FileOutputStream fout = new FileOutputStream(target.toFile());
        byte[] buf = new byte[BUFFER_SIZE];
        int z;
        while ((z = fin.read(buf)) > 0) {
          progressBar.update(nread, LENGTH);
          fout.write(buf, 0, z);
          nread += z;
        }

        System.out.println();
      } else {
        Files.copy(source, target);
      }
    }

    if (verbose) {
      progressBar.update(LENGTH, LENGTH);
    }
  }

  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("dvr <slc> <output>", OPTIONS);
  }

}
