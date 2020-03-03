package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_FORKORT_PERIODE;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_VALIDERING;

/**
 * Legger til avklarte fakta med informasjon om endringsbegrunnelse.
 *
 * Transisjoner:
 * IV_FORKORT_PERIODE -> IV_VALIDERING eller FEILET_MASKINELT hvis feil
 */
@Component
public class ForkortPeriode extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ForkortPeriode.class);

    private final AvklartefaktaService avklartefakteService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EessiService eessiService;
    private final LandvelgerService landvelgerService;

    @Autowired
    public ForkortPeriode(AvklartefaktaService avklartefaktaService, BehandlingsresultatService behandlingsresultatService, @Qualifier("system") EessiService eessiService, LandvelgerService landvelgerService) {
        this.avklartefakteService = avklartefaktaService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        log.info("ForkortPeriode initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_FORKORT_PERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        Endretperiode endretperiode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class);

        avklartefakteService.leggTilBegrunnelse(behandling.getId(), Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiode.getKode());
        avklarMottakerInstitusjoner(prosessinstans, behandling);

        prosessinstans.setSteg(IV_VALIDERING);
        log.info("Oppdatert avklarteFakta for prosessinstans {}.", prosessinstans.getId());
    }

    private void avklarMottakerInstitusjoner(Prosessinstans prosessinstans, Behandling behandling) throws MelosysException {
        Fagsak fagsak = behandling.getFagsak();

        Collection<Landkoder> utlMyndighetLand = landvelgerService.hentUtenlandskTrygdemyndighetsland(prosessinstans.getBehandling().getId());
        BucType bucType = BucType.fraBestemmelse(behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).hentValidertLovvalgsperiode().getBestemmelse());

        if (eessiService.landErEessiReady(bucType.name(), utlMyndighetLand)) {

            List<BucInformasjon> tilknyttedeBucer = eessiService.hentTilknyttedeBucer(fagsak.getGsakSaksnummer(), Collections.emptyList())
                .stream().filter(bi -> bucType.name().equals(bi.getBucType())).collect(Collectors.toList());

            if (tilknyttedeBucer.isEmpty()) {
                throw new TekniskException(utlMyndighetLand.stream().map(Landkoder::getBeskrivelse).collect(Collectors.joining(", "))
                    + " er EESSI-ready, men har ingen tidligere buc tilknyttet seg");
            }

            prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, tilknyttedeBucer.iterator().next().getMottakerinstitusjoner());
        }
    }
}
