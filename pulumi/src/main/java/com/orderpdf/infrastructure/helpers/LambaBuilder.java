package com.orderpdf.infrastructure.helpers;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LambaBuilder {

    public static String Build() {
        try {
            System.out.println("Building JAR...");

            String mvnCommand = System.getProperty("os.name").toLowerCase().contains("windows") ? "mvn.cmd" : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvnCommand, "clean", "package", "-f", "../order-app/pom.xml");
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Maven build failed with exit code: " + exitCode);
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            String sourceJar = "../order-app/target/order-app-1.0-SNAPSHOT.jar";
            String uniqueJar = "../order-app/target/order-app-" + timestamp + ".jar";

            Files.copy(
                    Paths.get(sourceJar),
                    Paths.get(uniqueJar),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println("JAR build completed successfully!");
            System.out.println("Created unique JAR: " + uniqueJar);

            return uniqueJar;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build JAR: " + e.getMessage(), e);
        }
    }
}
