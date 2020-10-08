package no.nav.melosys.saksflyt.kontroll;

import java.util.concurrent.ThreadPoolExecutor;

import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping("/internal")
public class SaksflytStatusTjeneste {

    private final ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor;

    public SaksflytStatusTjeneste(ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor) {
        this.saksflytThreadPoolTaskExecutor = saksflytThreadPoolTaskExecutor;
    }

    @GetMapping("/isAlive")
    public ResponseEntity<Void> sjekkSaksflyt() {
        ThreadPoolExecutor executor = saksflytThreadPoolTaskExecutor.getThreadPoolExecutor();
        return (executor.isShutdown() || executor.isTerminated() || executor.isTerminating())
            ? ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            : ResponseEntity.ok().build();
    }
}
