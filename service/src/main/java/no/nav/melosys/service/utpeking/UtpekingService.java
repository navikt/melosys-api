package no.nav.melosys.service.utpeking;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.vedtak.VedtaksfattingFasade;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class UtpekingService {

    private static final Logger log = LoggerFactory.getLogger(UtpekingService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EessiService eessiService;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final Unleash unleash;
    private final UtpekingsperiodeRepository utpekingsperiodeRepository;
    private final FerdigbehandlingKontrollService ferdigbehandlingKontrollService;
    private final ApplicationEventMulticaster melosysEventMulticaster;

    public UtpekingService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                           EessiService eessiService, LandvelgerService landvelgerService,
                           LovvalgsperiodeService lovvalgsperiodeService, OppgaveService oppgaveService,
                           ProsessinstansService prosessinstansService,
                           Unleash unleash, UtpekingsperiodeRepository utpekingsperiodeRepository,
                           FerdigbehandlingKontrollService ferdigbehandlingKontrollService,
                           ApplicationEventMulticaster melosysEventMulticaster) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.unleash = unleash;
        this.utpekingsperiodeRepository = utpekingsperiodeRepository;
        this.ferdigbehandlingKontrollService = ferdigbehandlingKontrollService;
        this.melosysEventMulticaster = melosysEventMulticaster;
    }

    public Collection<Utpekingsperiode> hentUtpekingsperioder(long behandlingID) {
        return utpekingsperiodeRepository.findByBehandlingsresultat_Id(behandlingID);
    }

    @Transactional
    public Collection<Utpekingsperiode> lagreUtpekingsperioder(long behandlingID, Collection<Utpekingsperiode> utpekingsperioder) {
        List<Utpekingsperiode> eksisterende = utpekingsperiodeRepository.findByBehandlingsresultat_Id(behandlingID);

        for (Utpekingsperiode utpekingsperiode : eksisterende) {
            if (utpekingsperiode.getSendtUtland() != null) {
                throw new FunksjonellException("Kan ikke oppdatere utpekingsperiode etter at A003 er sendt!");
            }
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        utpekingsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat);
        utpekingsperiodeRepository.flush();
        utpekingsperioder.forEach(a -> a.setBehandlingsresultat(behandlingsresultat));
        return utpekingsperiodeRepository.saveAll(utpekingsperioder);
    }

    @Transactional
    public void utpekLovvalgsland(Fagsak fagsak,
                                  Set<String> mottakerinstitusjoner,
                                  String ytterligereInformasjonSed,
                                  String fritekstBrev) {
        Behandling behandling = fagsak.hentAktivBehandling();
        if (behandling.getTema() != Behandlingstema.ARBEID_FLERE_LAND) {
            throw new FunksjonellException("Utpeking kan ikke skje for en behandling med tema " + behandling.getTema());
        }
        long behandlingID = fagsak.hentAktivBehandling().getId();

        if (log.isInfoEnabled()) {
            log.info("Utpeking av annet land for sak: {}, behandling: {}, mottakerinstitusjoner: {}",
                fagsak.getSaksnummer(), behandlingID, String.join(", ", mottakerinstitusjoner));
        }

        mottakerinstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            mottakerinstitusjoner,
            landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID),
            BucType.LA_BUC_02
        );

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        Utpekingsperiode utpekingsperiode = behandlingsresultat.hentValidertUtpekingsperiode();
        validerUtpekingsperiode(utpekingsperiode);

        opprettLovvalgsperiode(behandlingID, utpekingsperiode);
        ferdigbehandlingKontrollService.kontroller(behandlingID, behandlingsresultat.getType());
        oppdaterBehandlingsresultat(behandlingsresultat);
        prosessinstansService.opprettProsessinstansUtpekAnnetLand(
            behandling, utpekingsperiode.getLovvalgsland(), mottakerinstitusjoner, ytterligereInformasjonSed, fritekstBrev
        );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    private void oppdaterBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        behandlingsresultat.setType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);
        behandlingsresultat.settVedtakMetadata(Vedtakstyper.FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(
            VedtaksfattingFasade.FRIST_KLAGE_UKER));
        behandlingsresultatService.lagre(behandlingsresultat);

        melosysEventMulticaster.multicastEvent(new VedtakMetadataLagretEvent(behandlingsresultat.getId()));
    }

    private void opprettLovvalgsperiode(long behandlingID, Utpekingsperiode utpekingsperiode) {
        lovvalgsperiodeService
            .lagreLovvalgsperioder(behandlingID, Collections.singleton(Lovvalgsperiode.av(utpekingsperiode)));
    }

    @Transactional
    public void avvisUtpeking(long behandlingID, UtpekingAvvis utpekingAvvis) {
        validerAvslagUtpeking(utpekingAvvis);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandling " + behandlingID + " er ikke aktiv!");
        }

        if (unleash.isEnabled("melosys.eessi.handlingssjekk_sed")) {
            SedDokument sedDokument = behandling.hentSedDokument();
            String rinaSaksnummer = sedDokument.getRinaSaksnummer();

            if (sedDokument.getSedType().equals(SedType.A004) && !eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer, SedType.A004)) {
                throw new FunksjonellException("Kan ikke opprette SedType A004 på rinaSaknummer: " + rinaSaksnummer);
            }
        }

        if (behandling.erBeslutningLovvalgAnnetLand()) {
            behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.IKKE_GODKJENT);
        } else if (behandling.erNorgeUtpekt()) {
            SedDokument sedDokument = behandling.hentSedDokument();

            if (sedDokument.getSedType().equals(SedType.A003) && sedDokument.getLovvalgslandKode().equals(Landkoder.NO)) {
                behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.UTPEKING_NORGE_AVVIST);
            }

            behandlingsresultatService.oppdaterUtfallUtpeking(behandlingID, Utfallregistreringunntak.IKKE_GODKJENT);
        } else {
            throw new FunksjonellException("Kan ikke avvise utpeking for en behandling med tema " + behandling.getTema());
        }


        prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, utpekingAvvis);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerAvslagUtpeking(UtpekingAvvis utpekingAvvis) {
        if (StringUtils.isEmpty(utpekingAvvis.getBegrunnelse())) {
            throw new FunksjonellException("Du må oppgi en begrunnelse for å kunne avslå en utpeking");
        }
        if (utpekingAvvis.isEtterspørInformasjon() == null) {
            throw new FunksjonellException("Du må oppgi om forespørsel om mer informasjon vil bli sendt");
        }
    }

    private void validerUtpekingsperiode(Utpekingsperiode utpekingsperiode) {
        if (utpekingsperiode.getTom() == null) {
            throw new FunksjonellException("Utpekingsperioden mangler sluttdato");
        }
    }

    @Transactional
    public void oppdaterSendtUtland(Utpekingsperiode utpekingsperiode) {

        if (utpekingsperiode.getId() == null) {
            throw new TekniskException("Forsøk på å oppdatere en ikke-persistert utpekingsperiode");
        } else if (utpekingsperiode.getSendtUtland() != null) {
            throw new FunksjonellException("Utpekingsperiode " + utpekingsperiode.getId() + " er allerede markert som sendtUtland");
        }

        utpekingsperiode.setSendtUtland(LocalDate.now());
        utpekingsperiodeRepository.save(utpekingsperiode);
    }
}
