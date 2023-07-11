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
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Utfallregistreringunntak.GODKJENT;
import static no.nav.melosys.service.vedtak.VedtaksfattingFasade.FRIST_KLAGE_UKER;

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
    private final FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public EosVedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                            OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                            EessiService eessiService, LandvelgerService landvelgerService,
                            AvklartefaktaService avklartefaktaService, ApplicationEventMulticaster melosysEventMulticaster,
                            FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade, SaksbehandlingRegler saksbehandlingRegler) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.melosysEventMulticaster = melosysEventMulticaster;
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    public void fattVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultattype, Vedtakstyper vedtakstype) throws ValideringException {
        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultat(behandlingsresultattype)
            .medVedtakstype(vedtakstype)
            .build();
        fattVedtak(behandling, request);
    }

    public void fattVedtak(Behandling behandling, FattVedtakRequest request) throws ValideringException {
        long behandlingID = behandling.getId();

        log.info("Fatter vedtak for (EU_EØS) sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());

        if (behandlingsresultat.erInnvilgelse()) {
            var kontrollerSomSkalIgnoreres = request.isKopiTilArbeidsgiver()
                ? null
                : Collections.singleton(Kontroll_begrunnelser.OPPHØRT_ARBEIDSGIVER);

            Collection<Kontrollfeil> kontrollfeil = ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                behandling,
                behandlingsresultat,
                Sakstyper.EU_EOS,
                request.getBehandlingsresultatTypeKode(),
                kontrollerSomSkalIgnoreres
            );

            if (!kontrollfeil.isEmpty()) {
                throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                    kontrollfeil.stream().map(Kontrollfeil::tilDto).toList());
            }
        }

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK);

        if (saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling.getFagsak().getType(), behandling.getTema())) {
            behandlingsresultat.setFastsattAvLand(Land_iso2.NO);
            prosessinstansService.opprettProsessinstansIverksettIkkeYreksaktiv(behandling);
        } else {
            oppdaterBehandlingsresultat(behandlingsresultat, request.getVedtakstype(), request.getFritekst(), request.getNyVurderingBakgrunn());
            Set<String> mottakerinstitusjoner = avklarMottakerInstitusjoner(behandling, request.getMottakerinstitusjoner(), behandlingsresultat);
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(behandling, request.getBehandlingsresultatTypeKode(),
                request.getFritekst(), request.getFritekstSed(), mottakerinstitusjoner, request.isKopiTilArbeidsgiver());
        }

        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
    public void endreVedtaksperiode(Behandling behandling, Endretperiode endretperiode, String fritekst, String fritekstSed) {
        final long behandlingID = behandling.getId();
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (behandling.getType().equals(Behandlingstyper.ENDRET_PERIODE)) {
            behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        }
        if (!behandlingsresultat.hentLovvalgsperiode().erArtikkel12()) {
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
                                             String nyVurderingBakgrunn) {
        final Behandling behandling = behandlingsresultat.getBehandling();
        if (behandling.erNorgeUtpekt()) {
            behandlingsresultatService.oppdaterUtfallUtpeking(behandling.getId(), GODKJENT);
        }

        behandlingsresultat.setNyVurderingBakgrunn(nyVurderingBakgrunn);
        behandlingsresultat.settVedtakMetadata(vedtakstype, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(behandlingresultatBegrunnelseFritekst);
        behandlingsresultat.setFastsattAvLand(Land_iso2.NO);
        behandlingsresultatService.lagre(behandlingsresultat);

        melosysEventMulticaster.multicastEvent(new VedtakMetadataLagretEvent(behandling.getId()));
    }

    private Set<String> avklarMottakerInstitusjoner(Behandling behandling,
                                                    Set<String> mottakerinstitusjoner,
                                                    Behandlingsresultat behandlingsresultat) {
        if (saksbehandlingRegler.harTomFlyt(behandling)) {
            return Collections.emptySet();
        }

        Collection<Land_iso2> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        if (!behandling.erNorgeUtpekt() && skalSedSendes(behandlingsresultat, landkoder)) {
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

    private static boolean skalSedSendes(Behandlingsresultat behandlingsresultat, Collection<Land_iso2> landkoder) {
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
