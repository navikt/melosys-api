package no.nav.melosys.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.felles.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FagsakService {

    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);

    private static final String FAGSAKID_PREFIX = "MEL-";

    private FagsakRepository fagsakRepository;

    private TpsFasade tpsFasade;

    private AaregFasade aaregFasade;

    private EregFasade eregFasade;

    private MedlFasade medlFasade;

    private InntektFasade inntektFasade;

    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}")
    private Integer arbeidsforholdhistorikkAntallMåneder;

    @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}")
    private Integer inntektshistorikkAntallMåneder;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository, TpsFasade tpsFasade, AaregFasade aaregFasade, EregFasade eregFasade, MedlFasade medlFasade, InntektFasade inntektFasade) {
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.eregFasade = eregFasade;
        this.medlFasade = medlFasade;
        this.inntektFasade = inntektFasade;
    }

    public List<Fagsak> hentFagsaker(RolleType rolleType, String aktørID) {
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    // FIXME: Den metoden er bare for å hjelpe frontend midlertidig. Må slettes.
    public Iterable<Fagsak> hentAlle() {
        return fagsakRepository.findAll();
    }

    public Fagsak hentFagsak(String saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    @Transactional
    public Fagsak lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
        return sak;
    }

    public Fagsak nyFagsak(String fnr) throws SikkerhetsbegrensningException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(hentNesteSaksnummer());
        Behandling behandling = new Behandling();

        // FIXME: Når EESSI2-485 er ferdig må IntegrasjonsExceptions kastes videre
        Optional<Saksopplysning> personSaksopplysning = Optional.ofNullable(hentPerson(fnr));
        Optional<Saksopplysning> medlemskapSaksopplysning = Optional.ofNullable(hentMedlemskap(fnr));
        Optional<Saksopplysning> arbeidsforholdSaksopplysning = Optional.ofNullable(hentArbeidsforhold(fnr));
        Optional<Saksopplysning> inntektSaksopplysning = Optional.ofNullable(hentInntekt(fnr));

        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        personSaksopplysning.ifPresent(saksopplysninger::add);
        medlemskapSaksopplysning.ifPresent(saksopplysninger::add);
        arbeidsforholdSaksopplysning.ifPresent(saksopplysninger::add);
        inntektSaksopplysning.ifPresent(saksopplysninger::add);

        Set<String> orgnumre = new HashSet<>();

        arbeidsforholdSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraArbeidsforhold(saksopplysning)));
        inntektSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraInntekt(saksopplysning)));

        if (!orgnumre.isEmpty()) {
            saksopplysninger.addAll(hentOrganisasjoner(orgnumre));
        }

        saksopplysninger.forEach(x -> x.setBehandling(behandling));
        saksopplysninger.forEach(x -> x.setRegistrertDato(LocalDateTime.now()));

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId(fnr);
        aktoer.setEksternId(fnr);
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(RolleType.BRUKER);

        LocalDateTime dato = LocalDateTime.now();

        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(dato);
        behandling.setEndretDato(dato);
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandling.setType(BehandlingType.SØKNAD);

        fagsak.setAktører(new HashSet<>(Collections.singletonList(aktoer)));
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        fagsak.setRegistrertDato(dato);
        fagsak.setEndretDato(dato);
        fagsak.setType(FagsakType.EU_EØS);
        fagsak.setStatus(FagsakStatus.OPPRETTET);

        return lagre(fagsak);
    }

    public ArbeidsforholdDokument hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws SikkerhetsbegrensningException {
        Saksopplysning saksopplysning = aaregFasade.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        return (ArbeidsforholdDokument) saksopplysning.getDokument();
    }

    private Saksopplysning hentPerson(String fnr) throws SikkerhetsbegrensningException {
        // TODO: Informasjonsbehov.FAMILIERELASJONER kommer i runde 2
        try {
            return tpsFasade.hentPersonMedAdresse(fnr);
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet feil ved oppslag mot TPS", integrasjonException);
            return null;
        } catch (IkkeFunnetException e) {
            log.error("Person med id " + fnr + " finnes ikke");
            return null;
        }
    }

    private Saksopplysning hentMedlemskap(String fnr) throws SikkerhetsbegrensningException {
        try {
            return medlFasade.getPeriodeListe(fnr);
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet feil ved oppslag mot MEDL", integrasjonException);
            return null;
        }
    }

    private Saksopplysning hentArbeidsforhold(String fnr) throws SikkerhetsbegrensningException {
        final LocalDate tom  = LocalDate.now();
        final LocalDate fom = tom.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        try {
            return aaregFasade.finnArbeidsforholdPrArbeidstaker(fnr, AaregFasade.REGELVERK_A_ORDNINGEN, fom, tom);
        } catch (IntegrasjonException | TekniskException exception) {
            log.error("Uventet feil ved oppslag mot AAREG", exception);
            return null;
        }
    }

    private Saksopplysning hentInntekt(String fnr) throws SikkerhetsbegrensningException {
        final YearMonth tom = YearMonth.now();
        final YearMonth fom = tom.minusMonths(inntektshistorikkAntallMåneder);
        try {
            return inntektFasade.hentInntektListe(fnr, fom, tom);
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet feil ved oppslag mot Inntekt", integrasjonException);
            return null;
        }
    }

    private List<Saksopplysning> hentOrganisasjoner(Set<String> orgnumre) throws SikkerhetsbegrensningException {
        List<Saksopplysning> saksopplysninger = new ArrayList<>();

        for (String orgnr : orgnumre) {
            Saksopplysning saksopplysning = hentOrganisasjon(orgnr);
            if (saksopplysning != null) {
                saksopplysninger.add(saksopplysning);
            }
        }
        return saksopplysninger;
    }

    private static Set<String> hentOrgnumreFraArbeidsforhold(Saksopplysning saksopplysning) {
        return ((ArbeidsforholdDokument) saksopplysning.getDokument()).getArbeidsforhold().stream()
                .flatMap(arbeidsforhold -> Stream.of(arbeidsforhold.getArbeidsgiverID(), arbeidsforhold.getOpplysningspliktigID()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Set<String> hentOrgnumreFraInntekt(Saksopplysning saksopplysning) {
        return ((InntektDokument) saksopplysning.getDokument()).getArbeidsInntektMaanedListe().stream()
                .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
                .filter(Objects::nonNull)
                .map(ArbeidsInntektInformasjon::getInntektListe)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Inntekt::getVirksomhetID)
                .collect(Collectors.toSet());
    }

    private Saksopplysning hentOrganisasjon(String orgnr) throws SikkerhetsbegrensningException {
        try {
            return eregFasade.hentOrganisasjon(orgnr);
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet feil ved oppslag mot EREG", integrasjonException);
            return null;
        } catch (IkkeFunnetException e) {
            log.error("Organisasjon med orgnr " + orgnr + " finnes ikke");
            return null;
        }
    }

    private String hentNesteSaksnummer() {
        Long sekvensVerdi = fagsakRepository.hentNesteSekvensVerdi();
        if (sekvensVerdi == null) {
            throw new RuntimeException("Henting av neste SekvensVerdi fra sekvensen feilet.");
        } else {
            return FAGSAKID_PREFIX + Long.toString(sekvensVerdi);
        }
    }
}
