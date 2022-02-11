package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VedtakMetadataLagretEvent;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Utfallregistreringunntak.GODKJENT;
import static no.nav.melosys.service.vedtak.VedtakServiceFasade.FRIST_KLAGE_UKER;

@Service
public class EosVedtakService {
    private static final Logger log = LoggerFactory.getLogger(EosVedtakService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final LandvelgerService landvelgerService;
    private final AvklartefaktaService avklartefaktaService;
    private final ApplicationEventMulticaster melosysEventMulticaster;
    private final VedtakKontrollService vedtakKontrollService;

    @Autowired
    public EosVedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                            OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                            EessiService eessiService, LandvelgerService landvelgerService,
                            AvklartefaktaService avklartefaktaService, ApplicationEventMulticaster melosysEventMulticaster,
                            VedtakKontrollService vedtakKontrollService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.melosysEventMulticaster = melosysEventMulticaster;
        this.vedtakKontrollService = vedtakKontrollService;
    }

    public void fattVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultattype, Vedtakstyper vedtakstype) throws ValideringException {
        FattEosVedtakRequest request = new FattEosVedtakRequest.Builder()
            .medBehandlingsresultat(behandlingsresultattype)
            .medVedtakstype(vedtakstype)
            .build();
        fattVedtak(behandling, request);
    }

    public void fattVedtak(Behandling behandling, FattEosVedtakRequest request) throws ValideringException {
        long behandlingID = behandling.getId();

        log.info("Fatter vedtak for (EU_EØS) sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());

        if (behandlingsresultat.erInnvilgelse()) {
            vedtakKontrollService.kontrollerInnvilgelse(behandling, behandlingsresultat, request.getVedtakstype(), Sakstyper.EU_EOS);
        }

        oppdaterBehandlingsresultat(behandlingsresultat, request.getVedtakstype(), request.getFritekst(), request.getRevurderBegrunnelse());
        Set<String> mottakerinstitusjoner = validerOgAvklarMottakerInstitusjoner(behandling, request.getMottakerinstitusjoner(), behandlingsresultat);

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK);
        prosessinstansService.opprettProsessinstansIverksettVedtakEos(behandling, request.getBehandlingsresultatTypeKode(),
            request.getFritekst(), request.getFritekstSed(), mottakerinstitusjoner, request.getRevurderBegrunnelse());
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    public void endreVedtaksperiode(Behandling behandling, Endretperiode endretperiode, String fritekst, String fritekstSed) {
        final long behandlingID = behandling.getId();
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (!behandlingsresultat.hentValidertLovvalgsperiode().erArtikkel12()) {
            throw new FunksjonellException("Behandling av forkortet periode gjelder kun art. 12.");
        }
        if (prosessinstansService.harAktivProsessinstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en aktiv prosess for behandling " + behandling);
        }
        avklartefaktaService.leggTilBegrunnelse(behandlingID, Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiode.getKode());
        oppdaterBehandlingsresultat(behandlingsresultat, Vedtakstyper.ENDRINGSVEDTAK, fritekst, null);
        prosessinstansService.opprettProsessinstansForkortPeriode(
            behandling,
            fritekst,
            fritekstSed
        );
        log.info("Endrer vedtaksperiode for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(),
            behandlingID);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void oppdaterBehandlingsresultat(Behandlingsresultat behandlingsresultat,
                                             Vedtakstyper vedtakstype,
                                             String behandlingresultatBegrunnelseFritekst,
                                             String revurderBegrunnelse) {
        final Behandling behandling = behandlingsresultat.getBehandling();
        if (behandling.erNorgeUtpekt()) {
            behandlingsresultatService.oppdaterUtfallUtpeking(behandling.getId(), GODKJENT);
        }

        behandlingsresultat.settVedtakMetadata(vedtakstype, revurderBegrunnelse, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(behandlingresultatBegrunnelseFritekst);
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);
        behandlingsresultatService.lagre(behandlingsresultat);

        melosysEventMulticaster.multicastEvent(new VedtakMetadataLagretEvent(behandling.getId()));
    }

    private Set<String> validerOgAvklarMottakerInstitusjoner(Behandling behandling,
                                                             Set<String> mottakerinstitusjoner,
                                                             Behandlingsresultat behandlingsresultat) {
        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        if (mottakereTrenges(behandling) && skalSedSendes(behandlingsresultat, landkoder)) {
            mottakerinstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                mottakerinstitusjoner,
                landkoder,
                avklarBucType(behandlingsresultat)
            );
        } else {
            mottakerinstitusjoner = Collections.emptySet();
        }
        return mottakerinstitusjoner;
    }

    private static boolean mottakereTrenges(Behandling behandling) {
        return !behandling.erNorgeUtpekt();
    }

    private static boolean skalSedSendes(Behandlingsresultat behandlingsresultat, Collection<Landkoder> landkoder) {
        if (behandlingsresultat.erAvslag()) {
            return false;
        }
        if (landkoder.isEmpty()) {
            return false;
        }
        return !behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar();
    }

    private static BucType avklarBucType(Behandlingsresultat behandlingsresultat) {
        return BucType.fraBestemmelse(
            behandlingsresultat.hentValidertPeriodeOmLovvalg().getBestemmelse()
        );
    }
}
