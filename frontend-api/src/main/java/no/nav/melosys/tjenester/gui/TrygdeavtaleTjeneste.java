package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.LagreMedfolgendeFamilieDto;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;
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
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService,
                                BehandlingService behandlingService,
                                Aksesskontroll aksesskontroll,
                                BehandlingsgrunnlagService behandlingsgrunnlagService,
                                AvklartefaktaService avklartefaktaService,
                                AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService) {
        this.trygdeavtaleService = trygdeavtaleService;
        this.behandlingService = behandlingService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
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
        @PathVariable("behandlingID") long behandlingsId,
        @RequestBody TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {

        // TODO: Flytt dette ut til en egen klasse
        oppdaterBehandlingsgrunnlag(behandlingsId, trygdeAvtaleDataForVedtakDto);

        var familie = new ArrayList<>(trygdeAvtaleDataForVedtakDto.barn());
        familie.add(trygdeAvtaleDataForVedtakDto.ektefelle());
        LagreMedfolgendeFamilieDto lagreMedfolgendeFamilieDto = new LagreMedfolgendeFamilieDto(Set.copyOf(familie));
        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingsId, lagreMedfolgendeFamilieDto.til());

        // TODO:
        // behandlinger/1/medlemskapsperioder/bestemmelser - trenger ikke
        // behandlinger/1/trygdeavgift/grunnlag - trenger ikke
        // vilkaar/1 - bestemlese vilkår, kanskje lovvalgsperiode
        // avklartefakta/1/medfolgendeFamilie - begynne med denne - Done
        // sjekk lagring av virksomheter
    }

    private void oppdaterBehandlingsgrunnlag(long behandlingsId, TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {
        // Periode og land skal alltid være det sammne - men er en sanity sjekk for nå
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingsId);
        SoeknadFtrl behandlingsgrunnlagdata = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();

        Periode periode = behandlingsgrunnlagdata.periode;
        assert periode.getFom().equals(trygdeAvtaleDataForVedtakDto.fom());
        assert periode.getTom().equals(trygdeAvtaleDataForVedtakDto.tom());

        for (var land : trygdeAvtaleDataForVedtakDto.land()) {
            if (!behandlingsgrunnlagdata.soeknadsland.landkoder.contains(land))
                throw new TekniskException("Forventet " + land + " i behandlingsgrunnlagdata soeknadsland.landkoder");
        }
        // Ser ikke ut som vi trenger og oppdagtere dette nå
        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);
    }
}
