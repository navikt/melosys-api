package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.service.representant.dto.RepresentantDataDto;
import no.nav.melosys.service.representant.dto.RepresentantDto;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.ValgtRepresentantDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Api(tags = {"representant"})
@RequestMapping("/representant")
public class RepresentantTjeneste {

    private final RepresentantService representantService;
    private final Aksesskontroll aksesskontroll;

    public RepresentantTjeneste(RepresentantService representantService, Aksesskontroll aksesskontroll) {
        this.representantService = representantService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/liste")
    public ResponseEntity<List<RepresentantDto>> hentRepresentantListe(){
        return ResponseEntity.ok(
            representantService.hentRepresentantListe()
        );
    }
    @GetMapping("/{representantnummer}")
    public ResponseEntity<RepresentantDataDto> hentRepresentant(@PathVariable("representantnummer") String representantnummer){
        return ResponseEntity.ok(representantService.hentRepresentant(representantnummer));
    }

    @PostMapping("/valgt/{behandlingID}")
    public ResponseEntity<ValgtRepresentantDto> lagreValgtRepresentant(@PathVariable("behandlingID") long behandlingID,
                                                                       @RequestBody ValgtRepresentantDto valgtRepresentantDto) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            ValgtRepresentantDto.av(
                representantService.oppdaterValgtRepresentant(behandlingID, valgtRepresentantDto.til())
            )
        );
    }

    @GetMapping("/valgt/{behandlingID}")
    public ResponseEntity<ValgtRepresentantDto> hentValgtRepresentant(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return ResponseEntity.ok(
            ValgtRepresentantDto.av(
                representantService.hentValgtRepresentant(behandlingID)
            )
        );
    }
}
