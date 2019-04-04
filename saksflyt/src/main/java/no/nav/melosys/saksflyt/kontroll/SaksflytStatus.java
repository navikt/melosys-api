package no.nav.melosys.saksflyt.kontroll;

import no.nav.melosys.saksflyt.impl.InitSaksflyt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class SaksflytStatus {

    private final InitSaksflyt initSaksflyt;

    @Autowired
    public SaksflytStatus(InitSaksflyt initSaksflyt) {
        this.initSaksflyt = initSaksflyt;
    }

    @GetMapping("/saksflyt")
    public ResponseEntity sjekkSaksflyt() {
        return initSaksflyt.saksflytLever()
            ? ResponseEntity.ok().build()
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
