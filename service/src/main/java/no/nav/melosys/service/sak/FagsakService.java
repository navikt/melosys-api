package no.nav.melosys.service.sak;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.metrics.MetrikkerNavn.SAKER_OPPRETTET;

@Service
public class FagsakService {
    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);
    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;
    private final BehandlingService behandlingService;
    private final KontaktopplysningService kontaktopplysningService;
    private final OppgaveService oppgaveService;
    private final PersondataFasade persondataFasade;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovligeKombinasjonerService lovligeKombinasjonerService;

    private final Counter sakerOpprettet = Metrics.counter(SAKER_OPPRETTET);

    public FagsakService(FagsakRepository fagsakRepository, BehandlingService behandlingService,
                         KontaktopplysningService kontaktopplysningService, @Lazy OppgaveService oppgaveService,
                         PersondataFasade persondataFasade, BehandlingsresultatService behandlingsresultatService,
                         @Lazy LovligeKombinasjonerService lovligeKombinasjonerService) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.kontaktopplysningService = kontaktopplysningService;
        this.oppgaveService = oppgaveService;
        this.persondataFasade = persondataFasade;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovligeKombinasjonerService = lovligeKombinasjonerService;
    }

    public Fagsak hentFagsak(String saksnummer) {
        return finnFagsakFraSaksnummer(saksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Det finnes ingen fagsak med saksnummer: " + saksnummer));
    }

    public Optional<Fagsak> finnFagsakFraSaksnummer(String saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    public Fagsak hentFagsakFraArkivsakID(Long arkivsakID) {
        return finnFagsakFraArkivsakID(arkivsakID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke fagsak for arkivsakID " + arkivsakID));
    }

    public Optional<Fagsak> finnFagsakFraArkivsakID(Long arkivsakID) {
        return fagsakRepository.findByGsakSaksnummer(arkivsakID);
    }

    public List<Fagsak> hentFagsakerMedAktør(Aktoersroller rolleType, String ident) {
        String aktørID = persondataFasade.hentAktørIdForIdent(ident);
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    public List<Fagsak> hentFagsakerMedOrgnr(Aktoersroller rolleType, String orgnr) {
        return fagsakRepository.findByRolleAndOrgnr(rolleType, orgnr);
    }

    @Transactional
    public void lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
    }

    @Transactional
    public void oppdaterMyndigheterForEuEos(String saksnummer, Collection<String> ider) {
        Fagsak fagsak = hentFagsak(saksnummer);
        fagsak.getAktører().removeIf(aktoer -> !ider.contains(aktoer.getInstitusjonId())
            && aktoer.getRolle() == Aktoersroller.TRYGDEMYNDIGHET);

        Collection<Aktoer> nyeMyndigheter = ider.stream()
            .map(id -> lagMyndighetAktørForEuEos(fagsak, id))
            .toList();

        fagsak.getAktører().addAll(nyeMyndigheter);
        fagsakRepository.save(fagsak);
    }

    @Transactional
    public void oppdaterMyndighetForTrygdeavtale(String saksnummer, Land_iso2 landkode) {
        Fagsak fagsak = hentFagsak(saksnummer);

        fagsak.getAktører().removeIf(aktoer -> aktoer.getTrygdemyndighetLand() != landkode
            && aktoer.getRolle() == Aktoersroller.TRYGDEMYNDIGHET);

        boolean harIngenTrygdemyndigheter = fagsak.hentMyndigheter().isEmpty();
        if (harIngenTrygdemyndigheter) {
            Aktoer nyTrygdemyndighet = lagMyndighetAktørForTrygdeavtaler(fagsak, landkode);
            fagsak.getAktører().add(nyTrygdemyndighet);
        }
        fagsakRepository.save(fagsak);
    }

    private Aktoer lagMyndighetAktørForEuEos(Fagsak fagsak, String ID) {
        Aktoer aktør = new Aktoer();
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktør.setInstitusjonId(ID);
        return aktør;
    }

    private Aktoer lagMyndighetAktørForTrygdeavtaler(Fagsak fagsak, Land_iso2 landkode) {
        Aktoer aktør = new Aktoer();
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktør.setTrygdemyndighetLand(landkode);
        return aktør;
    }

    @Transactional
    public void ferdigbehandleSak(String saksnummer) {
        var fagsak = hentFagsak(saksnummer);
        var behandling = fagsak.hentAktivBehandling();
        var nyStatus = fagsak.getStatus() == Saksstatuser.OPPRETTET ? Saksstatuser.AVSLUTTET : fagsak.getStatus();

        avsluttFagsakOgBehandling(fagsak, behandling, nyStatus);
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.FERDIGBEHANDLET);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    @Transactional
    public Fagsak nyFagsakOgBehandling(OpprettSakRequest opprettSakRequest) {
        Fagsak fagsak = new Fagsak();
        String saksnummer = hentNesteSaksnummer();
        fagsak.setSaksnummer(saksnummer);

        HashSet<Aktoer> aktører = new HashSet<>();

        String aktørID = opprettSakRequest.getAktørID();
        if (aktørID != null) {
            Aktoer aktør = new Aktoer();
            aktør.setAktørId(aktørID);
            aktør.setUtenlandskPersonId(opprettSakRequest.getUtenlandskPersonId());
            aktør.setFagsak(fagsak);
            aktør.setRolle(Aktoersroller.BRUKER);
            aktører.add(aktør);
        }

        String virksomhetOrgnr = opprettSakRequest.getVirksomhetOrgnr();
        if (virksomhetOrgnr != null) {
            Aktoer virksomhet = new Aktoer();
            virksomhet.setOrgnr(virksomhetOrgnr);
            virksomhet.setFagsak(fagsak);
            virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
            aktører.add(virksomhet);
        }

        String arbeidsgiver = opprettSakRequest.getArbeidsgiver();
        if (arbeidsgiver != null) {
            Aktoer aktørArbeidsgiver = new Aktoer();
            aktørArbeidsgiver.setOrgnr(arbeidsgiver);
            aktørArbeidsgiver.setFagsak(fagsak);
            aktørArbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);
            aktører.add(aktørArbeidsgiver);
        }

        Fullmektig fullmektig = opprettSakRequest.getFullmektig();
        if (fullmektig != null) {
            Aktoer aktørFullmektig = lagAktørFullmektigMedID(fullmektig.getRepresentantID());
            aktørFullmektig.setFagsak(fagsak);
            aktørFullmektig.setRolle(Aktoersroller.REPRESENTANT);
            aktørFullmektig.setRepresenterer(fullmektig.getRepresenterer());
            aktører.add(aktørFullmektig);
        }

        Instant nå = Instant.now();

        fagsak.setType(opprettSakRequest.getSakstype());
        fagsak.setTema(opprettSakRequest.getSakstema());
        fagsak.setAktører(aktører);
        fagsak.setRegistrertDato(nå);
        fagsak.setEndretDato(nå);
        fagsak.setStatus(Saksstatuser.OPPRETTET);

        lagre(fagsak);

        List<Kontaktopplysning> kontaktopplysninger = opprettSakRequest.getKontaktopplysninger();
        if (CollectionUtils.isNotEmpty(kontaktopplysninger)) {
            kontaktopplysninger.forEach(opplysning -> kontaktopplysningService
                .lagEllerOppdaterKontaktopplysning(saksnummer, opplysning.getKontaktopplysningID().getOrgnr(),
                    opplysning.getKontaktOrgnr(), opplysning.getKontaktNavn(), opplysning.getKontaktTelefon()));
        }

        Behandlingstyper behandlingstype = opprettSakRequest.getBehandlingstype();
        Behandlingstema behandlingstema = opprettSakRequest.getBehandlingstema();
        Behandlingsaarsaktyper behandlingsårsaktype = opprettSakRequest.getBehandlingsårsaktype();
        String behandlingsårsakFritekst = opprettSakRequest.getBehandlingsårsakFritekst();
        LocalDate mottaksdato = opprettSakRequest.getMottaksdato();
        String initierendeJournalpostId = opprettSakRequest.getInitierendeJournalpostId();
        String initierendeDokumentId = opprettSakRequest.getInitierendeDokumentId();
        Behandling behandling = behandlingService.nyBehandling(fagsak,
            Behandlingsstatus.OPPRETTET, behandlingstype, behandlingstema,
            initierendeJournalpostId, initierendeDokumentId, mottaksdato,
            behandlingsårsaktype, behandlingsårsakFritekst);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        sakerOpprettet.increment();
        return fagsak;
    }

    private Aktoer lagAktørFullmektigMedID(String representantID) {
        Aktoer aktørFullmektig = new Aktoer();
        if (representantID.length() == 11) {
            aktørFullmektig.setPersonIdent(representantID);
        } else {
            aktørFullmektig.setOrgnr(representantID);
        }
        return aktørFullmektig;
    }

    private String hentNesteSaksnummer() {
        return FAGSAKID_PREFIX + fagsakRepository.hentNesteSekvensVerdi();
    }

    @Transactional
    public void oppdaterFagsakOgBehandling(String saksnummer, Sakstyper nySakstype, Sakstemaer nySakstema,
                                           Behandlingstema nyBehandlingstema, Behandlingstyper nyBehandlingstype,
                                           Behandlingsstatus nyBehandlingsstatus, LocalDate nyMottaksdato) {
        Fagsak fagsak = hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling();
        validerOppdatering(fagsak, behandling, nySakstype, nySakstema, nyBehandlingstema, nyBehandlingstype);
        if (fagsak.getType() != nySakstype || fagsak.getTema() != nySakstema) {
            log.info("Endrer sakstype for fagsak {} fra {} til {}", fagsak.getSaksnummer(), fagsak.getType(), nySakstype);
            fagsak.setType(nySakstype);
            fagsak.setTema(nySakstema);
            fagsakRepository.save(fagsak);
        }
        behandlingService.endreBehandling(behandling.getId(), nyBehandlingstype, nyBehandlingstema, nyBehandlingsstatus, nyMottaksdato);
    }

    private void validerOppdatering(Fagsak sak, Behandling behandling, Sakstyper nySakstype, Sakstemaer nySakstema,
                                    Behandlingstema nyBehandlingstema, Behandlingstyper nyBehandlingstype) {
        var fagsakHarEndring = sak.getType() != nySakstype || sak.getTema() != nySakstema;
        if (fagsakHarEndring && !sak.kanEndreTypeOgTema()) {
            throw new FunksjonellException("Sakstype og sakstema kan ikke endres for " + sak.getSaksnummer());
        }
        var behandlingHarEndring = behandling.getTema() != nyBehandlingstema || behandling.getType() != nyBehandlingstype;
        if (fagsakHarEndring || behandlingHarEndring) {
            lovligeKombinasjonerService.validerOpprettelseOgEndring(sak.getHovedpartRolle(), nySakstype, nySakstema, nyBehandlingstema, nyBehandlingstype);
        }
    }


    public void avsluttFagsakOgBehandling(Fagsak fagsak, Saksstatuser saksstatus) {
        Behandling aktivBehandling = fagsak.hentAktivBehandling();
        if (aktivBehandling == null) {
            log.warn("Forsøker å lukke behandling for fagsak {} som ikke har noen aktiv behandling", fagsak.getSaksnummer());
            oppdaterStatus(fagsak, saksstatus);
        } else {
            avsluttFagsakOgBehandling(fagsak, aktivBehandling, saksstatus);
        }
    }

    public void avsluttFagsakOgBehandling(Fagsak fagsak,
                                          Behandling behandling,
                                          Saksstatuser saksstatus) {
        if (!behandling.getFagsak().getSaksnummer().equals(fagsak.getSaksnummer())) {
            throw new FunksjonellException("Behandling " + behandling.getId()
                + " tilhører ikke fagsak " + fagsak.getSaksnummer());
        }
        oppdaterStatus(fagsak, saksstatus);
        behandlingService.avsluttBehandling(behandling.getId());
        log.info("Fagsak {} med behandling avsluttet", fagsak.getSaksnummer());
    }

    public void oppdaterStatus(Fagsak fagsak, Saksstatuser saksstatus) {
        fagsak.setStatus(saksstatus);
        fagsakRepository.save(fagsak);
    }

    public List<Fagsak> hentFagsaker(Collection<String> saksnumre) {
        return fagsakRepository.findAllBySaksnummerIn(saksnumre);
    }
}
