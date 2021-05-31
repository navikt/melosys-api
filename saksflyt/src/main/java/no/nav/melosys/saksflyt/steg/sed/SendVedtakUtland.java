package no.nav.melosys.saksflyt.steg.sed;

import java.util.List;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;


@Component
public class SendVedtakUtland extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(SendVedtakUtland.class);

    private final BehandlingService behandlingService;
    private final BrevBestiller brevBestiller;
    private final SedSomBrevService sedSomBrevService;
    private final UtpekingService utpekingService;
    private final Unleash unleash;

    @Autowired
    public SendVedtakUtland(@Qualifier("system") EessiService eessiService,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            BrevBestiller brevBestiller,
                            SedSomBrevService sedSomBrevService,
                            UtpekingService utpekingService, Unleash unleash) {
        super(eessiService, behandlingsresultatService);
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
        this.sedSomBrevService = sedSomBrevService;
        this.utpekingService = utpekingService;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_VEDTAK_UTLAND;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final var behandling = prosessinstans.getBehandling();
        final var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        if (behandling.erNorgeUtpekt()) {
            eessiService.sendGodkjenningArbeidFlereLand(behandling.getId(), prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED));
        } else if (behandlingsresultat.erUtpeking()) {
            utpekingService.oppdaterSendtUtland(behandlingsresultat.hentValidertUtpekingsperiode());
            SendUtlandStatus status = sendSedA003(prosessinstans);
            log.info("SendUtlandStatus for behandling {}: {}", behandling.getId(), status);
        } else if (skalSendesUtland(behandlingsresultat)) {
            super.sendUtland(avklarBucType(behandling), prosessinstans);
        } else if (unleash.isEnabled("melosys.labuc01.lukk_etter_vedtak") && behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar()) {
            finnOgLukkTilhørendeBUC(behandlingsresultat);
        }
    }

    private SendUtlandStatus sendSedA003(Prosessinstans prosessinstans) {
        log.info("Sender A003 for utpeking til {}, i behandling {}",
            prosessinstans.getData(ProsessDataKey.UTPEKT_LAND), prosessinstans.getBehandling().getId());
        return sendUtland(BucType.LA_BUC_02, prosessinstans);
    }

    private DoksysBrevbestilling lagBrevBestilling(Prosessinstans prosessinstans) {
        Long behandlingID = prosessinstans.getBehandling().getId();
        var behandling = behandlingService.hentBehandling(behandlingID);
        return new DoksysBrevbestilling.Builder().medProduserbartDokument(ATTEST_A1)
            .medAvsenderNavn(hentSaksbehandler(prosessinstans))
            .medMottakere(Mottaker.av(MYNDIGHET))
            .medBehandling(behandling)
            .medBegrunnelseKode(hentBegrunnelsekodeTilForkortetPeriode(prosessinstans))
            .build();
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) {
        var behandling = prosessinstans.getBehandling();
        if (prosessinstans.getData(ProsessDataKey.UTPEKT_LAND) != null) {
            Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
            String journalpostID = sedSomBrevService
                .lagJournalpostForSendingAvSedSomBrev(SedType.A003, utpektLand, behandling);
            prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostID);
            prosessinstans.setData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, utpektLand);
        } else {
            brevBestiller.bestill(lagBrevBestilling(prosessinstans));
        }
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.utlandSkalVarslesOmVedtak();
    }

    private BucType avklarBucType(Behandling behandling) {
        return behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
            .getLovvalgsperioder().stream().findFirst()
            .map(Lovvalgsperiode::getBestemmelse)
            .map(BucType::fraBestemmelse)
            .orElseThrow(() -> new TekniskException("Finner ikke lovvalgsbestemmelse for behandling " + behandling.getId()));
    }

    private void finnOgLukkTilhørendeBUC(Behandlingsresultat behandlingsresultat) {
        eessiService.hentTilknyttedeBucer(behandlingsresultat.getBehandling().getFagsak().getGsakSaksnummer(), List.of())
            .stream()
            .filter(buc -> BucType.LA_BUC_01.name().equalsIgnoreCase(buc.getBucType()))
            .findFirst()
            .map(BucInformasjon::getId)
            .ifPresent(eessiService::lukkBuc);
    }
}
