package no.nav.melosys.service;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FagsakService {

    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);

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

    public Fagsak hentFagsak(Long saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    public void lagre(Fagsak sak) {
        fagsakRepository.save(sak);
    }

    public Fagsak nyFagsak(String fnr) throws SikkerhetsbegrensningException {
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

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId(fnr);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(saksopplysninger);

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>(Collections.singletonList(aktoer)));
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        fagsak.setRegistrertDato(LocalDateTime.now());
        fagsak.setType(FagsakType.SØKNAD_A1);
        fagsak.setVersjon(0);
        fagsak.setStatus(FagsakStatus.OPPRETTET);

        fagsakRepository.save(fagsak);

        return fagsak;
    }

    private Saksopplysning hentPerson(String fnr) throws SikkerhetsbegrensningException {
        // TODO: Informasjonsbehov.FAMILIERELASJONER kommer i runde 2
        try {
            return tpsFasade.hentPersonMedAdresse(fnr);
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet feil ved oppslag mot TPS", integrasjonException);
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
        }
    }

}
