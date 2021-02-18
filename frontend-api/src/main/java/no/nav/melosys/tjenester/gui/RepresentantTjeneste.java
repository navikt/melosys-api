package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.tjenester.gui.dto.RepresentantDataDto;
import no.nav.melosys.tjenester.gui.dto.RepresentantDto;
import no.nav.melosys.tjenester.gui.dto.ValgtRepresentantDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Protected
@RestController
@Api(tags = {"representant"})
@RequestMapping("/representant")
public class RepresentantTjeneste {

    private final RepresentantService representantService;

    public RepresentantTjeneste(RepresentantService representantService) {
        this.representantService = representantService;
    }

    @GetMapping("/liste")
    public ResponseEntity<List<RepresentantDto>> hentRepresentantListe(){
        return ResponseEntity.ok(
            representantService.hentRepresentantListe()
                .stream()
                .map(RepresentantDto::av)
                .collect(Collectors.toList())
        );
    }
    @GetMapping("/{representantnummer}")
    public ResponseEntity<RepresentantDataDto> hentRepresentant(@PathVariable("representantnummer") String representantnummer){
        return ResponseEntity.ok(
            RepresentantDataDto.av(
                representantService.hentRepresentant(representantnummer)
            )
        );
    }

    @PostMapping("/valgt/{behandlingID}")
    public ResponseEntity<ValgtRepresentantDto> lagreValgtRepresentant(@PathVariable("behandlingID") long behandlingID,
                                                                       @RequestBody ValgtRepresentantDto valgtRepresentantDto) throws FunksjonellException {
        return ResponseEntity.ok(
            ValgtRepresentantDto.av(
                representantService.oppdaterValgtRepresentant(behandlingID, valgtRepresentantDto.til())
            )
        );
    }

    @GetMapping("/valgt/{behandlingID}")
    public ResponseEntity<ValgtRepresentantDto> hentValgtRepresentant(@PathVariable("behandlingID") long behandlingID) throws IkkeFunnetException {
        return ResponseEntity.ok(
            ValgtRepresentantDto.av(
                representantService.hentValgtRepresentant(behandlingID)
            )
        );
    }
}
