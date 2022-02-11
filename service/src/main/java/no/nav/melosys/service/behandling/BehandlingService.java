package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toList;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.ARBEID_FLERE_LAND;
import static no.nav.melosys.metrics.MetrikkerNavn.*;

@Service
public class BehandlingService {

    private static final Logger log = LoggerFactory.getLogger(BehandlingService.class);

    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final OppgaveService oppgaveService;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final Counter behandlingerAvsluttet = Metrics.counter(BEHANDLINGER_AVSLUTTET);
    private static final String FINNER_IKKE_BEHANDLING = "Finner ikke behandling med id ";

    static {
        Arrays.stream(Behandlingstema.values()).forEach(
            b -> Metrics.counter(BEHANDLINGSTEMAER_OPPRETTET, TAG_TEMA, b.getKode())
        );
        Arrays.stream(Behandlingstyper.values()).forEach(
            b -> Metrics.counter(BEHANDLINGSTYPER_OPPRETTET, TAG_TYPE, b.getKode())
        );
    }

    @Autowired
    public BehandlingService(BehandlingRepository behandlingRepository,
                             BehandlingsresultatRepository behandlingsresultatRepository,
                             TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository,
                             BehandlingsresultatService behandlingsresultatService,
                             BehandlingsgrunnlagService behandlingsgrunnlagService,
                             @Lazy OppgaveService oppgaveService,
                             ApplicationEventPublisher applicationEventPublisher) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.oppgaveService = oppgaveService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Behandling hentBehandling(long behandlingId) {
        return Optional.ofNullable(behandlingRepository.findWithSaksopplysningerById(behandlingId))
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
    }

    public Behandling hentBehandlingUtenSaksopplysninger(long behandlingId) {
        return behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
    }

    @Transactional
    public Behandling nyBehandling(Fagsak fagsak,
                                   Behandlingsstatus behandlingsstatus,
                                   Behandlingstyper behandlingstype,
                                   Behandlingstema behandlingstema,
                                   String initierendeJournalpostId,
                                   String initierendeDokumentId) {
        Instant nå = Instant.now();

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(nå);
        behandling.setEndretDato(nå);
        behandling.setBehandlingsfrist(hentBehandlingsfristForBehandlingstema(behandlingstema));

        behandling.setStatus(behandlingsstatus);
        behandling.setType(behandlingstype);
        behandling.setTema(behandlingstema);
        behandling.setInitierendeJournalpostId(initierendeJournalpostId);
        behandling.setInitierendeDokumentId(initierendeDokumentId);
        behandlingRepository.save(behandling);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.UDEFINERT);
        behandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        behandlingsresultatRepository.save(behandlingsresultat);

        Metrics.counter(BEHANDLINGSTEMAER_OPPRETTET, TAG_TEMA, behandlingstema.getKode()).increment();
        Metrics.counter(BEHANDLINGSTYPER_OPPRETTET, TAG_TYPE, behandlingstype.getKode()).increment();
        return behandling;
    }

    @Transactional
    public void endreBehandling(long behandlingID, Sakstyper ignoredSakstype, Behandlingstyper type, Behandlingstema tema, Behandlingsstatus status, LocalDate behandlingsfrist) {
        // TODO: Endre sakstype (MELOSYS-4899 for EØS <-> trygdeavtale)
        var behandling = hentBehandlingUtenSaksopplysninger(behandlingID);
        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres");
        }

        if (saksbehandlerKanEndreStatus(behandling, status)) {
            endreStatus(behandling, status);
        }
        if (saksbehandlerKanEndreType(behandling, type)) {
            endreType(behandling, type);
        }
        if (saksbehandlerKanEndreTema(behandling, tema)) {
            endreTema(behandling, tema);
        }
        if (saksbehandlerKanEndreFrist(behandling, behandlingsfrist)) {
            endreBehandlingsfrist(behandling, behandlingsfrist);
        }
    }

    /**
     * @deprecated Erstattes av endreBestilling
     */
    @Deprecated
    @Transactional
    public void endreBehandlingstemaTilBehandling(long behandlingID, Behandlingstema nyttTema) {
        Behandling behandling = hentBehandlingUtenSaksopplysninger(behandlingID);
        var behandlingsgrunnlag = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (MuligeBehandlingsverdier.hentMuligeBehandlingstema(behandling, behandlingsgrunnlag).contains(nyttTema)) {
            behandling.setTema(nyttTema);

            tilbakestillBehandlingsgrunnlag(behandling);
            applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
            if (nyttTema != ARBEID_FLERE_LAND) {
                behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand = false;
                behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandling.getBehandlingsgrunnlag());
            }
        } else {
            throw new FunksjonellException("Ikke mulig å endre behandlingstema");
        }
    }

    public void avsluttBehandling(long behandlingId) {
        Behandling behandling = hentBehandlingUtenSaksopplysninger(behandlingId);
        if (behandling.erAvsluttet()) {
            throw new FunksjonellException("Behandling " + behandlingId + " er allerede avsluttet!");
        }

        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);
        behandlingerAvsluttet.increment();
        applicationEventPublisher.publishEvent(new BehandlingEndretStatusEvent(AVSLUTTET, behandling));
    }

    /**
     * Knytt medlemsperioder fra MEDL til behandlingen.
     */
    @Transactional
    public void knyttMedlemsperioder(long behandlingID, List<Long> periodeIder) {
        Behandling behandling = hentBehandlingUtenSaksopplysninger(behandlingID);

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Medlemsperioder kan ikke lagres på behandling med status " + behandling.getStatus());
        }
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = periodeIder.stream()
            .map(pid -> new TidligereMedlemsperiode(behandlingID, pid)).collect(toList());
        tidligereMedlemsperiodeRepository.deleteById_BehandlingId(behandlingID);
        tidligereMedlemsperiodeRepository.saveAll(tidligereMedlemsperioder);
    }

    @Deprecated
    public void lagre(Behandling behandling) {
        behandlingRepository.save(behandling);
    }

    public void endreStatus(long behandlingID, Behandlingsstatus status) {
        Behandling behandling = hentBehandlingUtenSaksopplysninger(behandlingID);
        endreStatus(behandling, status);
    }

    public void endreStatus(Behandling behandling, Behandlingsstatus status) {
        if (behandling.getStatus() == status) {
            return;
        }

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres. Status var: " + behandling.getStatus());
        }

        log.info("Oppdaterer status for behandling {} fra {} til {}", behandling.getId(), behandling.getStatus(), status);
        behandling.setStatus(status);
        if (behandling.erVenterForDokumentasjon()) {
            behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Period.ofWeeks(2)));
        }

        behandlingRepository.save(behandling);

        if (List.of(AVVENT_FAGLIG_AVKLARING, AVVENT_DOK_PART, AVVENT_DOK_UTL).contains(status)) {
            oppgaveService.oppdaterOppgaveMedSaksnummer(
                behandling.getFagsak().getSaksnummer(),
                OppgaveOppdatering.builder().beskrivelse(status.getBeskrivelse()).build()
            );
        } else if (status == Behandlingsstatus.AVSLUTTET) {
            oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
        }
        applicationEventPublisher.publishEvent(new BehandlingEndretStatusEvent(status, behandling));
    }

    public void endreType(Behandling behandling, Behandlingstyper type) {
        log.info("Endrer behandlingstypen for behandling {} fra {} til {}", behandling.getId(), behandling.getType(), type);
        behandling.setType(type);
        behandlingRepository.save(behandling);
        tilbakestillBehandlingsgrunnlag(behandling);
        applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
    }

    public void endreTema(Behandling behandling, Behandlingstema tema) {
        log.info("Endrer behandlingstema for behandling {} fra {} til {}", behandling.getId(), behandling.getTema(), tema);
        behandling.setTema(tema);
        behandlingRepository.save(behandling);
        tilbakestillBehandlingsgrunnlag(behandling);
        applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
    }

    public void endreBehandlingsfrist(Behandling behandling, LocalDate behandlingsfrist) {
        log.info("Endrer behandlingsfrist for behandling {} fra {} til {}", behandling.getId(), behandling.getBehandlingsfrist(), behandlingsfrist);
        behandling.setBehandlingsfrist(behandlingsfrist);
        behandlingRepository.save(behandling);
        applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
    }

    public List<Long> hentMedlemsperioder(long behandlingID) {
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandlingID);
        return tidligereMedlemsperioder.stream()
            .map(TidligereMedlemsperiode::getId)
            .map(TidligereMedlemsperiodeId::getPeriodeId)
            .collect(toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Behandling replikerBehandlingOgBehandlingsresultat(Behandling tidligsteInaktiveBehandling,
                                                              Behandlingsstatus behandlingsstatus,
                                                              Behandlingstyper behandlingstype) {
        Behandling behandlingsreplika;
        try {
            behandlingsreplika = replikerBehandling(tidligsteInaktiveBehandling, behandlingsstatus, behandlingstype);
            behandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new TekniskException(String.format("Klarte ikke replikere behandling %s for fagsak %s",
                tidligsteInaktiveBehandling.getId(), tidligsteInaktiveBehandling.getFagsak().getSaksnummer()), e);
        }

        return behandlingsreplika;
    }

    Behandling replikerBehandling(Behandling tidligsteInaktiveBehandling, Behandlingsstatus behandlingsstatus, Behandlingstyper behandlingstype)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Behandling behandlingsreplika = (Behandling) BeanUtils.cloneBean(tidligsteInaktiveBehandling);
        behandlingsreplika.setId(null);
        behandlingsreplika.setType(behandlingstype);
        behandlingsreplika.setStatus(behandlingsstatus);
        behandlingsreplika.setOpprinneligBehandling(tidligsteInaktiveBehandling);
        behandlingsreplika.setBehandlingsgrunnlag(replikerBehandlingsgrunnlag(behandlingsreplika, tidligsteInaktiveBehandling.getBehandlingsgrunnlag()));
        behandlingsreplika.setBehandlingsnotater(Collections.emptySet());
        behandlingsreplika.setBehandlingsfrist(hentBehandlingsfristForBehandlingstema(tidligsteInaktiveBehandling.getTema()));

        behandlingsreplika.setSaksopplysninger(new HashSet<>());
        for (Saksopplysning saksopplysning : tidligsteInaktiveBehandling.getSaksopplysninger()) {
            Saksopplysning saksopplysningsreplika = (Saksopplysning) BeanUtils.cloneBean(saksopplysning);
            saksopplysningsreplika.setBehandling(behandlingsreplika);
            saksopplysningsreplika.setKilder(replikerKilder(saksopplysningsreplika, saksopplysning.getKilder()));
            saksopplysningsreplika.setId(null);
            behandlingsreplika.getSaksopplysninger().add(saksopplysningsreplika);
        }
        behandlingRepository.save(behandlingsreplika);
        return behandlingsreplika;
    }

    private Set<SaksopplysningKilde> replikerKilder(Saksopplysning saksopplysningreplika, Set<SaksopplysningKilde> kilder)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Set<SaksopplysningKilde> kildereplikas = new HashSet<>();
        for (var kilde : kilder) {
            var kildereplika = (SaksopplysningKilde) BeanUtils.cloneBean(kilde);
            kildereplika.setId(null);
            kildereplika.setSaksopplysning(saksopplysningreplika);
            kildereplikas.add(kildereplika);
        }

        return kildereplikas;
    }

    private Behandlingsgrunnlag replikerBehandlingsgrunnlag(Behandling behandlingsreplika, Behandlingsgrunnlag opprinneligBehandlingsgrunnlag)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (opprinneligBehandlingsgrunnlag == null) {
            return null;
        }

        Behandlingsgrunnlag replikertBehandlingsgrunnlag = (Behandlingsgrunnlag) BeanUtils.cloneBean(opprinneligBehandlingsgrunnlag);
        replikertBehandlingsgrunnlag.setId(null);
        replikertBehandlingsgrunnlag.setBehandling(behandlingsreplika);
        return replikertBehandlingsgrunnlag;
    }

    @Transactional(readOnly = true)
    public Collection<Behandling> hentBehandlingerMedstatus(Behandlingsstatus behandlingsstatus) {
        return behandlingRepository.findAllByStatus(behandlingsstatus);
    }

    public void endreBehandlingsstatusFraOpprettetTilUnderBehandling(Behandling aktivBehandling) {
        if (aktivBehandling.getStatus() == Behandlingsstatus.OPPRETTET) {
            aktivBehandling.setStatus(UNDER_BEHANDLING);
            behandlingRepository.save(aktivBehandling);
        }
    }

    public void oppdaterStatusOgSvarfrist(Behandling behandling, Behandlingsstatus behandlingsstatus, Instant svarfristDato) {
        behandling.setStatus(behandlingsstatus);
        behandling.setDokumentasjonSvarfristDato(svarfristDato);
        behandlingRepository.save(behandling);
    }

    /**
     * @deprecated Erstattes av endreBestilling
     */
    @Deprecated
    @Transactional
    public void endreBehandlingsfrist(long behandlingId, LocalDate behandlingsfrist) {
        Behandling behandling = hentBehandlingUtenSaksopplysninger(behandlingId);
        behandling.setBehandlingsfrist(behandlingsfrist);
        behandlingRepository.save(behandling);

        applicationEventPublisher.publishEvent(new BehandlingsfristEndretEvent(behandlingId, behandlingsfrist));
    }

    private void tilbakestillBehandlingsgrunnlag(Behandling behandling) {
        behandlingsresultatService.tømBehandlingsresultat(behandling.getId());
        if (behandling.getTema() != ARBEID_FLERE_LAND) {
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand = false;
            behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandling.getBehandlingsgrunnlag());
        }
    }

    public Set<Behandlingsstatus> hentMuligeStatuser(long behandlingID) {
        var behandling = hentBehandlingUtenSaksopplysninger(behandlingID);
        return MuligeBehandlingsverdier.hentMuligeStatuser(behandling);
    }

    public Set<Behandlingstema> hentMuligeBehandlingstema(long behandlingID) {
        var behandling = hentBehandlingUtenSaksopplysninger(behandlingID);
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        return MuligeBehandlingsverdier.hentMuligeBehandlingstema(behandling, behandlingsresultat);
    }

    public Set<Behandlingstyper> hentMuligeTyper(long behandlingID) {
        var behandling = hentBehandlingUtenSaksopplysninger(behandlingID);
        return MuligeBehandlingsverdier.hentMuligeTyper(behandling);
    }


    private boolean saksbehandlerKanEndreStatus(Behandling behandling, Behandlingsstatus status) {
        if (status == null || status == behandling.getStatus()) return false;
        MuligeBehandlingsverdier.validerNyStatusMulig(behandling, status);
        return true;
    }

    private boolean saksbehandlerKanEndreType(Behandling behandling, Behandlingstyper type) {
        if (type == null || type == behandling.getType()) return false;
        MuligeBehandlingsverdier.validerNyTypeMulig(behandling, type);
        return true;
    }

    private boolean saksbehandlerKanEndreTema(Behandling behandling, Behandlingstema tema) {
        if (tema == null || tema == behandling.getTema()) return false;
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        MuligeBehandlingsverdier.validerNyttTemaMulig(behandling, behandlingsresultat, tema);
        return behandlingsresultat.erIkkeArtikkel16MedSendtAnmodningOmUnntak();
    }

    private boolean saksbehandlerKanEndreFrist(Behandling behandling, LocalDate behandlingsfrist) {
        return behandlingsfrist != null && !behandlingsfrist.equals(behandling.getBehandlingsfrist());
    }

    private LocalDate hentBehandlingsfristForBehandlingstema(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_FLERE_LAND, ARBEID_ETT_LAND_ØVRIG, IKKE_YRKESAKTIV, ARBEID_I_UTLANDET, ARBEID_NORGE_BOSATT_ANNET_LAND, YRKESAKTIV -> LocalDate.now().plusDays(30);
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE -> LocalDate.now().plusWeeks(2);
            case BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND -> LocalDate.now().plusWeeks(4);
            case ANMODNING_OM_UNNTAK_HOVEDREGEL, ØVRIGE_SED_UFM, ØVRIGE_SED_MED, TRYGDETID -> LocalDate.now().plusWeeks(8);
        };
    }
}
