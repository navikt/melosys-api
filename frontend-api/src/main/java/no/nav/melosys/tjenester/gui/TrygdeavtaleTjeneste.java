package no.nav.melosys.tjenester.gui;

import java.util.Collections;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleInfoDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeAvtaleDataForVedtakDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/trygdeavtale")
@Api(tags = {"trygdeavtale"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TrygdeavtaleTjeneste {

    private final TrygdeavtaleService trygdeavtaleService;
    private final BehandlingService behandlingService;
    private final Aksesskontroll aksesskontroll;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService,
                                BehandlingService behandlingService,
                                Aksesskontroll aksesskontroll,
                                BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.trygdeavtaleService = trygdeavtaleService;
        this.behandlingService = behandlingService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @GetMapping("{behandlingID}")
    public ResponseEntity<TrygdeavtaleInfoDto> hentTrygdeavtaleInfo(@PathVariable("behandlingID") long behandlingId,
                                                                    @RequestParam(value = "virksomheter", required = false) boolean hentVirksomheter,
                                                                    @RequestParam(value = "barnEktefeller", required = false) boolean hentBarnEktefeller) {

        aksesskontroll.autoriserSkriv(behandlingId);
        var behandling = behandlingService.hentBehandling(behandlingId);
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        return ResponseEntity.ok(new TrygdeavtaleInfoDto(
            behandling.getFagsak().hentAktørID(),
            behandling.getTema().getKode(),
            behandlingsgrunnlagdata.periode,
            behandlingsgrunnlagdata.soeknadsland.landkoder,
            hentVirksomheter ? trygdeavtaleService.hentVirksomheter(behandling) : Collections.emptyMap(),
            hentBarnEktefeller ? trygdeavtaleService.hentFamiliemedlemmer(behandling) : Collections.emptyList()
        ));
    }

    @PostMapping("{behandlingID}")
    public void overforDataForVedtak(
        @PathVariable("behandlingID") long behandlingsid,
        @RequestBody TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {

        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingsid);
        SoeknadFtrl behandlingsgrunnlagdata = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();

        behandlingsgrunnlagdata.periode = new Periode(
            trygdeAvtaleDataForVedtakDto.fom(),
            trygdeAvtaleDataForVedtakDto.tom()
        );

        behandlingsgrunnlagdata.soeknadsland.landkoder.addAll(trygdeAvtaleDataForVedtakDto.land());

        // TODO:
        // behandlinger/1/medlemskapsperioder/bestemmelser - trenger ikke
        // behandlinger/1/trygdeavgift/grunnlag - trenger ikke
        // vilkaar/1 - bestemlese vilkår, kanskje lovvalgsperiode
        // avklartefakta/1/medfolgendeFamilie - begynne med denne

        // sjekk lagring av virksomheter

        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);
    }
}
