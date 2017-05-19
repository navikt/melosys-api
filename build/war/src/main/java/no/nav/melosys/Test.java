package no.nav.melosys;

import no.nav.melosys.domain.Person;
import no.nav.melosys.service.EksempelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.SimpleDateFormat;

@SpringBootApplication
public class Test implements CommandLineRunner {

    @Autowired
    EksempelService service;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void run(String... strings) throws Exception {

    }

    public void demo() throws Exception {

        System.out.println("\n1.findAll()...");
        for (Person person : service.findAll()) {
            System.out.println("test " + person);
        }

        System.out.println("\n2.findByEmail(String email)...");
        for (Person person : service.findByEmail("222@test.no")) {
            System.out.println(person);
        }

        System.out.println("\n3.findByDate(Date date)...");
        for (Person person : service.findByDate(sdf.parse("2017-05-11"))) {
            System.out.println(person);
        }

        System.out.println("Done!");

        System.exit(0);
    }

/*<!--        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-undertow</artifactId>
        </dependency>-->*/
}
