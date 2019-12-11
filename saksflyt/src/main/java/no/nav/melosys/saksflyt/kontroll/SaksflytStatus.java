package no.nav.melosys.saksflyt.kontroll;

import no.nav.melosys.saksflyt.impl.Saksflyt;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping("/internal")
public class SaksflytStatus {

    private final Saksflyt saksflyt;

    @Autowired
    public SaksflytStatus(Saksflyt saksflyt) {
        this.saksflyt = saksflyt;
    }

    @GetMapping("/isAlive")
    public ResponseEntity sjekkSaksflyt() {
        return saksflyt.saksflytLever()
            ? ResponseEntity.ok().build()
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
