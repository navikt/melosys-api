package no.nav.melosys.service.sak;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
@RequestMapping("/admin/fagsaker")
@Tags({
    @Tag(name = "fagsak"),
    @Tag(name = "admin")
})
public class FagsakAdminController {
    private final HenleggelseService henleggelseService;
    private final FagsakRepository fagsakRepository;
    private final AktoerService aktoerService;
    private final Aksesskontroll aksesskontroll;

    public FagsakAdminController(HenleggelseService henleggelseService,
                                FagsakRepository fagsakRepository,
                                AktoerService aktoerService,
                                Aksesskontroll aksesskontroll) {
        this.henleggelseService = henleggelseService;
        this.fagsakRepository = fagsakRepository;
        this.aktoerService = aktoerService;
        this.aksesskontroll = aksesskontroll;
    }

    @PutMapping("/{behandlingID}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable long behandlingID) {
        henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingID);

        return ResponseEntity.noContent().build();
    }

    //Endre aktørID til en annen eksisterende aktørid.
    @PutMapping("/{saksnummer}/endreAktoerId/{aktoerid}")
    public ResponseEntity<Void> endreAktoerId(@PathVariable String saksnummer, @PathVariable String aktoerid) {
        if (aktoerid == null || aktoerid.length() != 13) {
            throw new IllegalArgumentException("Aktør ID kan ikke være null og må være 19 tegn lang " + aktoerid);
        }

        Fagsak fagsak = fagsakRepository.findById(saksnummer)
                .orElseThrow(() -> new IllegalArgumentException("Finner ikke fagsak med saksnummer: " + saksnummer));

        Aktoer eksisterendeBrukerAktor = fagsak.getAktører().stream()
                .filter(aktoer -> aktoer.getRolle() == Aktoersroller.BRUKER)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Finner ikke BRUKER aktør for " + fagsak.getSaksnummer()));

        String gammelAktørId = eksisterendeBrukerAktor.getAktørId();

        aksesskontroll.auditAutoriserAktørID(
                aktoerid,
                "Endring av aktør ID for sak " + saksnummer + " fra " + gammelAktørId + " til " + aktoerid
        );

        aktoerService.endreAktørIdForBruker(fagsak, aktoerid);

        return ResponseEntity.ok().build();
    }

}
