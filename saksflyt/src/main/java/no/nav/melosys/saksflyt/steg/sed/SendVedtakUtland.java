package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.TRYGDEMYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;


@Component
public class SendVedtakUtland extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(SendVedtakUtland.class);

    private final SedSomBrevService sedSomBrevService;
    private final UtpekingService utpekingService;
    private final ProsessinstansService prosessinstansService;

    public SendVedtakUtland(EessiService eessiService,
                            BehandlingsresultatService behandlingsresultatService,
                            SedSomBrevService sedSomBrevService,
                            UtpekingService utpekingService, ProsessinstansService prosessinstansService) {
        super(eessiService, behandlingsresultatService);
        this.sedSomBrevService = sedSomBrevService;
        this.utpekingService = utpekingService;
        this.prosessinstansService = prosessinstansService;
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
            var bucer = eessiService.hentTilknyttedeBucer(behandling.getFagsak().getGsakSaksnummer(), Collections.emptyList());
            if (bucer.stream().anyMatch(buc -> buc.getBucType().equals(BucType.LA_BUC_02.name()) && buc.erÅpen())) {
                if (behandling.erNyVurdering()) {
                    annullerSedForNyVurderingMedSendtVedtak(behandling);
                }
                eessiService.sendGodkjenningArbeidFlereLand(behandling.getId(), prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED));
            } else {
                log.info("Sender ikke godkjenning av utpeking da behandling {} ikke er tilknyttet en åpen LA_BUC_02", behandling.getId());
            }
        } else if (behandlingsresultat.erUtpeking()) {
            utpekingService.oppdaterSendtUtland(behandlingsresultat.hentValidertUtpekingsperiode());
            SendUtlandStatus status = sendSedA003(prosessinstans);
            log.info("SendUtlandStatus for behandling {}: {}", behandling.getId(), status);
        } else if (skalSendesUtland(behandlingsresultat)) {
            super.sendUtland(avklarBucType(behandling), prosessinstans);
        } else if (behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar()) {
            finnOgLukkTilhørendeBUC(behandlingsresultat);
        }
    }

    private SendUtlandStatus sendSedA003(Prosessinstans prosessinstans) {
        log.info("Sender A003 for utpeking til {}, i behandling {}",
            prosessinstans.getData(ProsessDataKey.UTPEKT_LAND), prosessinstans.getBehandling().getId());
        return sendUtland(BucType.LA_BUC_02, prosessinstans);
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) {
        var behandling = prosessinstans.getBehandling();
        if (prosessinstans.getData(ProsessDataKey.UTPEKT_LAND) != null) {
            Land_iso2 utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Land_iso2.class);
            String journalpostID = sedSomBrevService
                .lagJournalpostForSendingAvSedSomBrev(SedType.A003, utpektLand, behandling);
            prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostID);
            prosessinstans.setData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, utpektLand);
        } else {
            DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
                .medProduserbartDokument(ATTEST_A1)
                .medAvsenderID(hentSaksbehandler(prosessinstans))
                .medBegrunnelseKode(hentBegrunnelsekodeTilForkortetPeriode(prosessinstans))
                .build();
            prosessinstansService.opprettProsessinstansSendBrev(behandling, brevbestilling, Mottaker.av(TRYGDEMYNDIGHET));
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

    public void annullerSedForNyVurderingMedSendtVedtak(Behandling behandling) {
        if (harSendtVedtak(behandling.getFagsak().getGsakSaksnummer())) {
            log.info("Invaliderer sendt vedtak SED for behandling %d  med rina saksnummer %d"
                .formatted(behandling.getId(), behandling.getFagsak().getGsakSaksnummer()));
            eessiService.sendInvalideringSed(behandling.getId(), "");
        }
    }

    private boolean harSendtVedtak(long rinasaksnummer) {
        var sedTypeList = List.of(SedType.A004, SedType.A012);
        return eessiService.hentTilknyttedeBucer(rinasaksnummer, Collections.emptyList())
            .stream()
            .filter(BucInformasjon::erÅpen)
            .flatMap(b -> b.getSeder().stream())
            .anyMatch(s -> sedTypeList.contains(SedType.valueOf(s.getSedType())));
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
