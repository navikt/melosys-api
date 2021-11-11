package no.nav.melosys.tjenester.gui;

import java.util.*;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.LagreMedfolgendeFamilieDto;
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
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService,
                                BehandlingService behandlingService,
                                Aksesskontroll aksesskontroll,
                                BehandlingsgrunnlagService behandlingsgrunnlagService,
                                AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                AvklarteVirksomheterService avklarteVirksomheterService,
                                LovvalgsperiodeService lovvalgsperiodeService) {
        this.trygdeavtaleService = trygdeavtaleService;
        this.behandlingService = behandlingService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
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

        // TODO: Flytt dette ut til en egen klasse - Gjør dette i en egen PR
        LagreMedfolgendeFamilieDto lagreMedfolgendeFamilieDto = lagMedfolgendeFamilieDto(trygdeAvtaleDataForVedtakDto);

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingsId, lagreMedfolgendeFamilieDto.til());

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(trygdeAvtaleDataForVedtakDto.virksomheter(), behandlingsId);

        SoeknadFtrl behandlingsgrunnlagdata = hentBehandlingsgrunnlagdata(behandlingsId);

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(trygdeAvtaleDataForVedtakDto, behandlingsgrunnlagdata);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingsId, List.of(lovvalgsperiode));
    }

    private LagreMedfolgendeFamilieDto lagMedfolgendeFamilieDto(TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {
        var familie = new ArrayList<>(trygdeAvtaleDataForVedtakDto.barn());
        familie.add(trygdeAvtaleDataForVedtakDto.ektefelle());
        return new LagreMedfolgendeFamilieDto(Set.copyOf(familie));
    }

    private Lovvalgsperiode lagLovvalgsperiode(TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto, SoeknadFtrl behandlingsgrunnlagdata) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(behandlingsgrunnlagdata.periode.getFom());
        lovvalgsperiode.setTom(behandlingsgrunnlagdata.periode.getTom());

        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        Landkoder lovvalgsland = Landkoder.valueOf(behandlingsgrunnlagdata.soeknadsland.landkoder.stream().findFirst()
            .orElseThrow(() -> new TekniskException("Forventet ett land i behandlingsgrunnlagdata soeknadsland.landkoder")));
        lovvalgsperiode.setLovvalgsland(lovvalgsland);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.valueOf(trygdeAvtaleDataForVedtakDto.bestemmelse()));
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL); // Skal bli renamet til FULL_DEKNING av fag
        return lovvalgsperiode;
    }

    private SoeknadFtrl hentBehandlingsgrunnlagdata(long behandlingsId) {
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingsId);
        return (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();
    }
}
