package uk.gov.companieshouse.companyprofile.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static final String NAMESPACE = "company-profile-search-consumer";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
