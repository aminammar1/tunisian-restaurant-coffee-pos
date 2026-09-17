package tn.cafe.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendPosApplication {

	public static void main(String[] args) {
		// A generic shell DEBUG variable is common in developer terminals, but Spring
		// Boot treats it as debug=true and emits a huge condition report.  Keep logs
		// quiet unless a developer deliberately asks for -Ddebug=true.
		if (System.getProperty("debug") == null) System.setProperty("debug", "false");
		SpringApplication app = new SpringApplication(BackendPosApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.setLogStartupInfo(false);
		app.run(args);
	}

}
