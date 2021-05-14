package no.nav.melosys.tjenester.gui.saksflyt;

import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakDto;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakSvarDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/anmodningsperioder")
@Api(tags = {"saksflyt", "anmodningsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningUnntakTjeneste {
    private final AnmodningUnntakService anmodningUnntakService;
    private final TilgangService tilgangService;

    @Autowired
    public AnmodningUnntakTjeneste(AnmodningUnntakService anmodningUnntakService, TilgangService tilgangService) {
        this.anmodningUnntakService = anmodningUnntakService;
        this.tilgangService = tilgangService;
    }

    @PostMapping("{behandlingID}/bestill")
    @ApiOperation(value = "Anmodning om unntak for en gitt behandling")
    public ResponseEntity<Void> anmodningOmUnntak(@PathVariable("behandlingID") long behandlingID,
                                                  @RequestBody AnmodningUnntakDto anmodningUnntakDto)
        throws ValideringException {
        tilgangService.sjekkTilgang(behandlingID);
        anmodningUnntakService.anmodningOmUnntak(behandlingID,
            anmodningUnntakDto.getMottakerinstitusjon(),
            anmodningUnntakDto.getVedlegg().stream()
                .map(v -> new DokumentReferanse(v.getJournalpostID(), v.getDokumentID()))
                .collect(Collectors.toUnmodifiableSet()),
            anmodningUnntakDto.getFritekstSed());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "svar", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Sender et svar på anmodning om unntak basert på AnmodningsperiodeSvar som er registrert på behandlingen")
    public ResponseEntity<Void> svar(@RequestBody AnmodningUnntakSvarDto anmodningUnntakSvarDto) {
        tilgangService.sjekkTilgang(anmodningUnntakSvarDto.behandlingID());
        anmodningUnntakService.anmodningOmUnntakSvar(anmodningUnntakSvarDto.behandlingID(), anmodningUnntakSvarDto.ytterligereInfo());
        return ResponseEntity.ok().build();
    }
}
