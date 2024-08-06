package no.nav.melosys.service.registeropplysninger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntekt.InntektService;
import no.nav.melosys.integrasjon.utbetaling.UtbetaldataRestService;
import no.nav.melosys.service.aareg.ArbeidsforholdService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
            .put(SaksopplysningType.UTBETAL, this::hentUtbetalingsopplysninger)
            .build());

    private final MedlPeriodeService medlPeriodeService;
    private final EregFasade eregFasade;
    private final ArbeidsforholdService arbeidsforholdService;
    private final BehandlingService behandlingService;
    private final InntektService inntektService;
    private final SaksopplysningerService saksopplysningerService;
    private final RegisteropplysningerPeriodeFactory registeropplysningerPeriodeFactory;
    private final UtbetaldataRestService utbetaldataRestService;

    public RegisteropplysningerService(MedlPeriodeService medlPeriodeService,
                                       EregFasade eregFasade,
                                       ArbeidsforholdService arbeidsforholdService,
                                       BehandlingService behandlingService,
                                       InntektService inntektService,
                                       SaksopplysningerService saksopplysningerService,
                                       RegisteropplysningerPeriodeFactory registeropplysningerPeriodeFactory,
                                       UtbetaldataRestService utbetaldataRestService) {
        this.medlPeriodeService = medlPeriodeService;
        this.eregFasade = eregFasade;
        this.arbeidsforholdService = arbeidsforholdService;
        this.behandlingService = behandlingService;
        this.inntektService = inntektService;
        this.saksopplysningerService = saksopplysningerService;
        this.registeropplysningerPeriodeFactory = registeropplysningerPeriodeFactory;
        this.utbetaldataRestService = utbetaldataRestService;
    }

    @Transactional
    public void hentOgLagreOpplysninger(RegisteropplysningerRequest registeropplysningerRequest) {
        if (PeriodeRegler.feilIPeriode(registeropplysningerRequest.getFom(), registeropplysningerRequest.getTom())) {
            log.info("Henter ikke registeropplysninger for behandling {} pga. manglende periode eller feil i periode. fom={}, tom={}",
                registeropplysningerRequest.getBehandlingID(), registeropplysningerRequest.getFom(), registeropplysningerRequest.getTom());
            registeropplysningerRequest = registeropplysningerRequest.lagKopiUtenPeriodeOgOpplysningstyperSomKreverPeriode();
        }
        if (registeropplysningerRequest.getOpplysningstyper().isEmpty()) {
            log.info("Var ingen registeropplysninger å hente for behandling {}", registeropplysningerRequest.getBehandlingID());
            return;
        }
        if (registeropplysningerRequest.hentOpplysningerFor5aar()) {
            registeropplysningerRequest.setFom(registeropplysningerRequest.getFom().minusYears(5));
        }

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(registeropplysningerRequest.getBehandlingID());

        hentOgLagreOpplysninger(registeropplysningerRequest, behandling);
    }

    private void hentOgLagreOpplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
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

    private List<Saksopplysning> hentSaksopplysninger(SaksopplysningType opplysningstype, RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
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

    private List<Saksopplysning> hentArbeidsforholdopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        RegisteropplysningerPeriodeFactory.DatoPeriode periodeForArbeidsforhold = registeropplysningerPeriodeFactory.hentPeriodeForArbeidsforhold(fom, tom);
        Saksopplysning saksopplysning = arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(registeropplysningerRequest.getFnr(), periodeForArbeidsforhold.fom, periodeForArbeidsforhold.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentMedlemskapsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        RegisteropplysningerPeriodeFactory.DatoPeriode periodeForMedlemskap = registeropplysningerPeriodeFactory.hentPeriodeForMedlemskap(fom, tom, behandling);
        Saksopplysning saksopplysning = medlPeriodeService.hentPeriodeListe(registeropplysningerRequest.getFnr(), periodeForMedlemskap.fom, periodeForMedlemskap.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentInntektsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        RegisteropplysningerPeriodeFactory.Periode periodeForYtelser = registeropplysningerPeriodeFactory.hentPeriodeForInntekt(fom, tom, behandling);
        Saksopplysning saksopplysning = inntektService.hentInntektListe(registeropplysningerRequest.getFnr(), periodeForYtelser.fom, periodeForYtelser.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentUtbetalingsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
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

        Saksopplysning saksopplysning = utbetaldataRestService.hentUtbetalingerBarnetrygd(registeropplysningerRequest.getFnr(), periodeForYtelser.fom.atDay(1), periodeForYtelser.tom.atDay(1));
        return saksopplysning != null ? List.of(saksopplysning) : List.of();
    }

    private List<Saksopplysning> hentOrganisasjonsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) {
        Set<String> orgnumreFraArbeidsforhold = saksopplysningerService.finnArbeidsforholdsopplysninger(behandling.getId())
            .map(ArbeidsforholdDokument::hentOrgnumre)
            .orElseGet(Collections::emptySet);
        Set<String> orgnumreFraInntekt = saksopplysningerService.finnInntektsopplysninger(behandling.getId())
            .map(InntektDokument::hentOrgnumre)
            .orElseGet(Collections::emptySet);

        return Sets.union(orgnumreFraArbeidsforhold, orgnumreFraInntekt).stream()
            .filter(RegisteropplysningerService::erGyldigOrgnr)
            .map(eregFasade::hentOrganisasjon)
            .toList();
    }

    // Ereg har ikke data om personer, men arbeidsforhold og inntekt kan innneholde fnr registrert som orgnr
    private static boolean erGyldigOrgnr(String orgnr) {
        return orgnr != null && orgnr.length() != 11;
    }

    @Transactional
    public void slettRegisterOpplysninger(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        behandling.getSaksopplysninger().removeIf(s -> s.getType() != SaksopplysningType.SEDOPPL);
        behandling.setSisteOpplysningerHentetDato(null);
        behandlingService.lagre(behandling);
    }

    @FunctionalInterface
    private interface HentRegisteropplysninger {
        List<Saksopplysning> hent(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling);
    }
}
