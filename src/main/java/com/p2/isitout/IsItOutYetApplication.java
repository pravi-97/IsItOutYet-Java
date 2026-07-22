package com.p2.isitout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@SpringBootApplication
@EnableScheduling
public class IsItOutYetApplication {

	public static void main(String[] args) {
		try {
			// Read the cert from inside the packaged JAR
			InputStream certStream = IsItOutYetApplication.class.getResourceAsStream("/cert/root.crt");
			if (certStream != null) {
				// Create a temporary physical file in the BTP container
				File tempCert = new File(System.getProperty("java.io.tmpdir"), "root.crt");
				Files.copy(certStream, tempCert.toPath(), StandardCopyOption.REPLACE_EXISTING);

				// Save the physical path to an environment variable Spring can read
				System.setProperty("DB_CERT_PATH", tempCert.getAbsolutePath());
			}
		} catch (Exception e) {
			System.err.println("Failed to extract database certificate: " + e.getMessage());
		}
		SpringApplication.run(IsItOutYetApplication.class, args);
	}

}
