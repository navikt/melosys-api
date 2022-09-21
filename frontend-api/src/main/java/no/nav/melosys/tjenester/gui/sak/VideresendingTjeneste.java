package no.nav.melosys.tjenester.gui.sak;

import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.VideresendSoknadService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.VideresendDto;
import no.nav.security.token.support.core.api.Protected;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/fagsaker/{saksnr}/henlegg-videresend")
@Api(tags = {"fagsaker", "videresending"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VideresendingTjeneste {
    private final Aksesskontroll aksesskontroll;
    private final FagsakService fagsakService;
    private final VideresendSoknadService videresendSoknadService;

    public VideresendingTjeneste(Aksesskontroll aksesskontroll, FagsakService fagsakService,
                                 VideresendSoknadService videresendSoknadService) {
        this.aksesskontroll = aksesskontroll;
        this.fagsakService = fagsakService;
        this.videresendSoknadService = videresendSoknadService;
    }

    @PostMapping
    @ApiOperation(value = "Videresender søknad for en gitt behandling")
    public ResponseEntity<Void> videresend(@PathVariable("saksnr") String saksnummer,
                                           @RequestBody VideresendDto videresendDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);

        if (CollectionUtils.isEmpty(videresendDto.getVedlegg())) {
            throw new FunksjonellException("Kan ikke videresende søknad uten vedlegg!");
        }

        videresendSoknadService.videresend(saksnummer, videresendDto.getMottakerinstitusjon(),
                                           videresendDto.getFritekst(), videresendDto.getVedlegg().stream().map(
                v -> new DokumentReferanse(v.journalpostID(), v.dokumentID())).collect(Collectors.toUnmodifiableSet()));
        return ResponseEntity.noContent().build();
    }
}
