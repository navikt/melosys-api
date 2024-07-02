package no.nav.melosys.tjenester.gui.saksflyt;

import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakDto;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakSvarDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/anmodningsperioder")
@Api(tags = {"saksflyt", "anmodningsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningUnntakController {

    private final AnmodningUnntakService anmodningUnntakService;
    private final Aksesskontroll aksesskontroll;

    public AnmodningUnntakController(AnmodningUnntakService anmodningUnntakService, Aksesskontroll aksesskontroll) {
        this.anmodningUnntakService = anmodningUnntakService;
        this.aksesskontroll = aksesskontroll;
    }

    @PostMapping("{behandlingID}/bestill")
    @ApiOperation(value = "Anmodning om unntak for en gitt behandling")
    public ResponseEntity<Void> anmodningOmUnntak(@PathVariable("behandlingID") long behandlingID,
                                                  @RequestBody AnmodningUnntakDto anmodningUnntakDto)
        throws ValideringException {
        aksesskontroll.autoriserSkriv(behandlingID);
        anmodningUnntakService.anmodningOmUnntak(behandlingID,
            anmodningUnntakDto.getMottakerinstitusjon(),
            anmodningUnntakDto.getVedlegg().stream()
                .map(v -> new DokumentReferanse(v.journalpostID(), v.dokumentID()))
                .collect(Collectors.toUnmodifiableSet()),
            anmodningUnntakDto.getFritekstSed());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/svar")
    @ApiOperation(value = "Sender et svar på anmodning om unntak basert på AnmodningsperiodeSvar som er registrert på behandlingen")
    public ResponseEntity<Void> svar(@PathVariable("behandlingID") long behandlingID, @RequestBody AnmodningUnntakSvarDto anmodningUnntakSvarDto) {
        aksesskontroll.autoriserSkriv(behandlingID);
        anmodningUnntakService.anmodningOmUnntakSvar(behandlingID, anmodningUnntakSvarDto.ytterligereInfo());
        return ResponseEntity.noContent().build();
    }
}
