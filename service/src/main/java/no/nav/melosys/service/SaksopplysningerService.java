package no.nav.melosys.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SaksopplysningerService {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningerService.class);

    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallÅr}")
    private Integer arbeidsforholdhistorikkAntallÅr;

    @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}")
    private Integer inntektshistorikkAntallMåneder;

    private TpsFasade tpsFasade;

    private AaregFasade aaregFasade;

    private EregFasade eregFasade;

    private MedlFasade medlFasade;

    private InntektFasade inntektFasade;

    @Autowired
    public SaksopplysningerService(TpsFasade tpsFasade, AaregFasade aaregFasade, EregFasade eregFasade, MedlFasade medlFasade, InntektFasade inntektFasade) {
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.eregFasade = eregFasade;
        this.medlFasade = medlFasade;
        this.inntektFasade = inntektFasade;
    }

    public ArbeidsforholdDokument hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws SikkerhetsbegrensningException {
        Saksopplysning saksopplysning = aaregFasade.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        return (ArbeidsforholdDokument) saksopplysning.getDokument();
    }

    public Set<Saksopplysning> hentSaksopplysninger(String fnr) throws SikkerhetsbegrensningException {
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

        saksopplysninger.forEach(x -> x.setRegistrertDato(LocalDateTime.now()));

        return saksopplysninger;
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
            return medlFasade.hentPeriodeListe(fnr);
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet feil ved oppslag mot MEDL", integrasjonException);
            return null;
        }
    }

    private Saksopplysning hentArbeidsforhold(String fnr) throws SikkerhetsbegrensningException {
        final LocalDate tom  = LocalDate.now();
        final LocalDate fom = tom.minusYears(arbeidsforholdhistorikkAntallÅr);
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


}
