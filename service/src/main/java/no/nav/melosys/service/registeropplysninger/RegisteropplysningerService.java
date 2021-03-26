package no.nav.melosys.service.registeropplysninger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sob.SobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisteropplysningerService {
    private static final Logger log = LoggerFactory.getLogger(RegisteropplysningerService.class);

    private static final Comparator<SaksopplysningType> SAKSOPPLYSNINGSTYPE_COMPARATOR = (s1, s2) -> {
        if (s1 == SaksopplysningType.ARBFORH || s1 == SaksopplysningType.INNTK) return -1;
        if (s2 == SaksopplysningType.ARBFORH || s2 == SaksopplysningType.INNTK) return 1;
        return 0;
    };

    private final Map<SaksopplysningType, HentRegisteropplysninger> SAKSOPPLYSNING_TYPE_FUNCTION_MAP =
        Maps.immutableEnumMap(ImmutableMap.<SaksopplysningType, HentRegisteropplysninger>builder()
            .put(SaksopplysningType.ARBFORH, this::hentArbeidsforholdopplysninger)
            .put(SaksopplysningType.INNTK, this::hentInntektsopplysninger)
            .put(SaksopplysningType.MEDL, this::hentMedlemskapsopplysninger)
            .put(SaksopplysningType.ORG, this::hentOrganisasjonsopplysninger)
            .put(SaksopplysningType.PERSHIST, this::hentPersonhistorikk)
            .put(SaksopplysningType.PERSOPL, this::hentPersonopplysninger)
            .put(SaksopplysningType.SOB_SAK, this::hentSakOgBehandlingSaker)
            .put(SaksopplysningType.UTBETAL, this::hentUtbetalingsopplysninger)
            .build());

    private final PersondataFasade persondataFasade;
    private final MedlPeriodeService medlPeriodeService;
    private final EregFasade eregFasade;
    private final AaregFasade aaregFasade;
    private final BehandlingService behandlingService;
    private final SobService sobService;
    private final InntektService inntektService;
    private final UtbetaldataService utbetaldataService;
    private final SaksopplysningerService saksopplysningerService;
    private final RegisteropplysningerPeriodeFactory registeropplysningerPeriodeFactory;

    @Autowired
    public RegisteropplysningerService(@Qualifier("system") PersondataFasade persondataFasade,
                                       MedlPeriodeService medlPeriodeService, @Qualifier("system") EregFasade eregFasade,
                                       AaregFasade aaregFasade,
                                       BehandlingService behandlingService,
                                       SobService sobService,
                                       InntektService inntektService,
                                       UtbetaldataService utbetaldataService,
                                       SaksopplysningerService saksopplysningerService,
                                       RegisteropplysningerPeriodeFactory registeropplysningerPeriodeFactory
    ) {
        this.persondataFasade = persondataFasade;
        this.medlPeriodeService = medlPeriodeService;
        this.eregFasade = eregFasade;
        this.aaregFasade = aaregFasade;
        this.behandlingService = behandlingService;
        this.sobService = sobService;
        this.inntektService = inntektService;
        this.utbetaldataService = utbetaldataService;
        this.saksopplysningerService = saksopplysningerService;
        this.registeropplysningerPeriodeFactory = registeropplysningerPeriodeFactory;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = MelosysException.class)
    public void hentOgLagreOpplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(registeropplysningerRequest.getBehandlingID());
        hentOgLagreOpplysninger(registeropplysningerRequest, behandling);
    }

    private void hentOgLagreOpplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws MelosysException {
        for (var opplysningstype : sorterteSaksopplysningstyper(registeropplysningerRequest.getOpplysningstyper())) {
            if (!SAKSOPPLYSNING_TYPE_FUNCTION_MAP.containsKey(opplysningstype)) {
                throw new TekniskException("Støtter ikke å hente opplysninger for saksopplysningType " + opplysningstype);
            }

            behandling.getSaksopplysninger().removeIf(s -> s.getType() == opplysningstype);
            List<Saksopplysning> saksopplysninger = hentSaksopplysninger(opplysningstype, registeropplysningerRequest, behandling);
            lagreSaksopplysninger(saksopplysninger, behandling);
        }

        behandling.setSisteOpplysningerHentetDato(Instant.now());
        behandlingService.lagre(behandling);
    }

    private Set<SaksopplysningType> sorterteSaksopplysningstyper(Set<SaksopplysningType> saksopplysningTyper) {
        return saksopplysningTyper.stream()
            .sorted(SAKSOPPLYSNINGSTYPE_COMPARATOR)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Saksopplysning> hentSaksopplysninger(SaksopplysningType opplysningstype, RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws MelosysException {
        return SAKSOPPLYSNING_TYPE_FUNCTION_MAP.get(opplysningstype).hent(registeropplysningerRequest, behandling);
    }

    private void lagreSaksopplysninger(List<Saksopplysning> saksopplysninger, Behandling behandling) {
        for (Saksopplysning saksopplysning : saksopplysninger) {
            behandling.getSaksopplysninger().add(lagSaksopplysning(saksopplysning, behandling));
            log.info("Registeropplysninger for {} hentet for behandling {}", saksopplysning.getType().getBeskrivelse(), behandling.getId());
        }
    }

    private Saksopplysning lagSaksopplysning(Saksopplysning saksopplysning, Behandling behandling) {
        Instant nå = Instant.now();
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        return saksopplysning;
    }

    private List<Saksopplysning> hentArbeidsforholdopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws TekniskException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        RegisteropplysningerPeriodeFactory.DatoPeriode periodeForArbeidsforhold = registeropplysningerPeriodeFactory.hentPeriodeForArbeidsforhold(fom, tom, behandling);
        Saksopplysning saksopplysning = aaregFasade.finnArbeidsforholdPrArbeidstaker(registeropplysningerRequest.getFnr(), periodeForArbeidsforhold.fom, periodeForArbeidsforhold.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentPersonopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Saksopplysning saksopplysning = persondataFasade.hentPerson(registeropplysningerRequest.getFnr(), registeropplysningerRequest.getInformasjonsbehov());
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentMedlemskapsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        RegisteropplysningerPeriodeFactory.DatoPeriode periodeForMedlemskap = registeropplysningerPeriodeFactory.hentPeriodeForMedlemskap(fom, tom, behandling);
        Saksopplysning saksopplysning = medlPeriodeService.hentPeriodeListe(registeropplysningerRequest.getFnr(), periodeForMedlemskap.fom, periodeForMedlemskap.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentInntektsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws TekniskException, FunksjonellException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        RegisteropplysningerPeriodeFactory.Periode periodeForYtelser = registeropplysningerPeriodeFactory.hentPeriodeForInntekt(fom, tom, behandling);
        Saksopplysning saksopplysning = inntektService.hentInntektListe(registeropplysningerRequest.getFnr(), periodeForYtelser.fom, periodeForYtelser.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentUtbetalingsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws FunksjonellException, TekniskException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        LocalDate treÅrTilbake = LocalDate.now().minusYears(3);
        if (fom.isBefore(treÅrTilbake)) {
            if (tom != null && tom.isBefore(treÅrTilbake)) {
                log.info("Kunne ikke hente utbetalingsdokument for behandling {} da periode er for langt tilbake i tid", behandling.getId());
                return Collections.emptyList();
            }

            fom = treÅrTilbake;
        }

        RegisteropplysningerPeriodeFactory.Periode periodeForYtelser = registeropplysningerPeriodeFactory.hentPeriodeForInntekt(fom, tom, behandling);
        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd(registeropplysningerRequest.getFnr(), periodeForYtelser.fom.atDay(1), periodeForYtelser.tom.atDay(1));

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentOrganisasjonsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws IkkeFunnetException, IntegrasjonException {
        Set<String> orgnumre = new HashSet<>();

        Optional<ArbeidsforholdDokument> arbeidsforholdDokument = saksopplysningerService.finnArbeidsforholdsopplysninger(behandling.getId());
        Optional<InntektDokument> inntektDokument = saksopplysningerService.finnInntektsopplysninger(behandling.getId());

        arbeidsforholdDokument.ifPresent(dokument -> orgnumre.addAll(dokument.hentOrgnumre()));
        inntektDokument.ifPresent(dokument -> orgnumre.addAll(dokument.hentOrgnumre()));

        List<Saksopplysning> saksopplysninger = new ArrayList<>();
        for (String orgnr : orgnumre) {
            saksopplysninger.add(eregFasade.hentOrganisasjon(orgnr));
        }

        return saksopplysninger;
    }

    private List<Saksopplysning> hentPersonhistorikk(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Saksopplysning saksopplysning = persondataFasade.hentPersonhistorikk(registeropplysningerRequest.getFnr(), registeropplysningerRequest.getFom());
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentSakOgBehandlingSaker(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws IkkeFunnetException, IntegrasjonException {
        String aktørId = persondataFasade.hentAktørIdForIdent(registeropplysningerRequest.getFnr());
        Saksopplysning saksopplysning = sobService.finnSakOgBehandlingskjedeListe(aktørId);

        return List.of(saksopplysning);
    }

    @FunctionalInterface
    private interface HentRegisteropplysninger {
        List<Saksopplysning> hent(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws MelosysException;
    }
}
