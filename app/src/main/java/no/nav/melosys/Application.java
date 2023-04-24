package no.nav.melosys;

import no.nav.melosys.service.oppgave.OppgaveMigrering;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@SpringBootApplication
@Controller
public class Application {

    private final OppgaveMigrering oppgaveMigrering;

    public Application(OppgaveMigrering oppgaveMigrering) {
        this.oppgaveMigrering = oppgaveMigrering;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping(path = { "/journalforing/**", "/sok/**" })
    public String forward() {
        return "forward:/";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void eventListenerExecute() {
        System.out.println("############# - eventListenerExecute" );
        oppgaveMigrering.go();
    }
}
