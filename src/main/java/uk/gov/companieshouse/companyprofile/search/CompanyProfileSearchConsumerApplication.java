package uk.gov.companieshouse.companyprofile.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompanyProfileSearchConsumerApplication {

    public static final String NAMESPACE = "company-profile-delta-consumer";

    public static void main(String[] args) {
        SpringApplication.run(CompanyProfileSearchConsumerApplication.class, args);
    }
}
