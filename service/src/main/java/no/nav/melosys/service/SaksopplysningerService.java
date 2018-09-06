package no.nav.melosys.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Service
public class SaksopplysningerService {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningerService.class);

    // FIXME : Injektere feltene i constructor MELOSYS-1635
    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallÅr}")
    private Integer arbeidsforholdhistorikkAntallÅr;

    @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}")
    private Integer inntektshistorikkAntallMåneder;

    @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}")
    private Integer medlemskaphistorikkAntallÅr;

    private final TpsFasade tpsFasade;

    private final AaregFasade aaregFasade;

    private final EregFasade eregFasade;

    private final MedlFasade medlFasade;

    private final InntektFasade inntektFasade;

    private final ProsessinstansRepository prosessinstansRepository;

    private final Binge binge;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SaksopplysningerService(TpsFasade tpsFasade,
                                   AaregFasade aaregFasade,
                                   EregFasade eregFasade,
                                   MedlFasade medlFasade,
                                   InntektFasade inntektFasade,
                                   ProsessinstansRepository prosessinstansRepository,
                                   Binge binge,
                                   BehandlingRepository behandlingRepository) {
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.eregFasade = eregFasade;
        this.medlFasade = medlFasade;
        this.inntektFasade = inntektFasade;
        this.prosessinstansRepository = prosessinstansRepository;
        this.binge = binge;
        this.behandlingRepository = behandlingRepository;
    }

    public ArbeidsforholdDokument hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws SikkerhetsbegrensningException {
        Saksopplysning saksopplysning = aaregFasade.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        return (ArbeidsforholdDokument) saksopplysning.getDokument();
    }

    public Set<Saksopplysning> hentSaksopplysninger(String aktørID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        String fnr = tpsFasade.hentIdentForAktørId(aktørID);

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
        final LocalDate tom  = LocalDate.now();
        final LocalDate fom = tom.minusYears(medlemskaphistorikkAntallÅr);
        try {
            return medlFasade.hentPeriodeListe(fnr, fom, tom);
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

    public void oppfriskSaksopplysning(long behandlingsid) throws IkkeFunnetException, TekniskException {
        log.info("Starter oppfrisking av behandlingsid: {} ", behandlingsid);

        Optional<Prosessinstans> aktivProsessinstans = prosessinstansRepository.findByStegIsNotNullAndBehandling_Id(behandlingsid);
        Behandling behandling = behandlingRepository.findOne(behandlingsid);
        if (behandling == null) {
            log.error("Behandling ikke funnet med behandlingsid {}", behandlingsid);
            throw new IkkeFunnetException("Behandling ikke funnet med behandlingsid: " + behandlingsid);
        }

        String aktør_Id = behandling.getFagsak().getBruker().getAktørId();

        SoeknadDokument søknadDokument;
        Optional<SaksopplysningDokument> opt = hentDokument(behandling, SaksopplysningType.SØKNAD);
        if (opt.isPresent()) {
            søknadDokument = (SoeknadDokument) opt.get();
        } else {
            throw new TekniskException("Oppfrisking feilet på grunn av manglende søknad opplysning");
        }

        LocalDateTime nå = LocalDateTime.now();

        if (!aktivProsessinstans.isPresent()) {
            behandling.getSaksopplysninger().removeIf(saksopplysning -> saksopplysning.getType() != SaksopplysningType.SØKNAD);

            Prosessinstans nyprosessinstans = new Prosessinstans();
            nyprosessinstans.setBehandling(behandling);
            nyprosessinstans.setType(ProsessType.OPPFRISKNING);
            nyprosessinstans.setData(ProsessDataKey.AKTØR_ID, aktør_Id);
            nyprosessinstans.setData(ProsessDataKey.BRUKER_ID, tpsFasade.hentIdentForAktørId(aktør_Id));

            nyprosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, hentPeriode(søknadDokument));
            nyprosessinstans.setData(ProsessDataKey.LAND, hentLand(søknadDokument));

            nyprosessinstans.setSteg(ProsessSteg.JFR_HENT_PERS_OPPL);
            nyprosessinstans.setData(ProsessDataKey.OPPFRISK_SAKSOPPLYSNING, true);
            nyprosessinstans.setRegistrertDato(nå);

            prosessinstansRepository.save(nyprosessinstans);
            binge.leggTil(nyprosessinstans);
        } else {
            log.warn("Aktiv prosessinstans finnes allerede. Ikke mulig å oppfriske saksopplysning.");
        }
    }
}
