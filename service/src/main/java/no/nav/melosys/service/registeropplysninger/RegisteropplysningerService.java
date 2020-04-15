package no.nav.melosys.service.registeropplysninger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    private final TpsFasade tpsFasade;
    private final MedlPeriodeService medlPeriodeService;
    private final EregFasade eregFasade;
    private final AaregFasade aaregFasade;
    private final BehandlingService behandlingService;
    private final SakOgBehandlingFasade sakOgBehandlingFasade;
    private final InntektService inntektService;
    private final UtbetaldataService utbetaldataService;
    private final SaksopplysningerService saksopplysningerService;
    private final Integer arbeidsforholdhistorikkAntallMåneder;
    private final Integer medlemskaphistorikkAntallÅr;

    @Autowired
    public RegisteropplysningerService(@Qualifier("system") TpsFasade tpsFasade,
                                       MedlPeriodeService medlPeriodeService, @Qualifier("system") EregFasade eregFasade,
                                       AaregFasade aaregFasade,
                                       BehandlingService behandlingService,
                                       SakOgBehandlingFasade sakOgBehandlingFasade,
                                       InntektService inntektService,
                                       UtbetaldataService utbetaldataService,
                                       SaksopplysningerService saksopplysningerService,
                                       @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}") Integer arbeidsforholdhistorikkAntallMåneder,
                                       @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr
    ) {
        this.tpsFasade = tpsFasade;
        this.medlPeriodeService = medlPeriodeService;
        this.eregFasade = eregFasade;
        this.aaregFasade = aaregFasade;
        this.behandlingService = behandlingService;
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        this.inntektService = inntektService;
        this.utbetaldataService = utbetaldataService;
        this.saksopplysningerService = saksopplysningerService;
        this.arbeidsforholdhistorikkAntallMåneder = arbeidsforholdhistorikkAntallMåneder;
        this.medlemskaphistorikkAntallÅr = medlemskaphistorikkAntallÅr;
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

        final LocalDate iDag = LocalDate.now();
        if (fom.isAfter(iDag)) {
            fom = iDag.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        } else {
            fom = fom.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        }

        if (tom == null || tom.isAfter(iDag)) {
            tom = iDag;
        }

        Saksopplysning saksopplysning = aaregFasade.finnArbeidsforholdPrArbeidstaker(registeropplysningerRequest.getFnr(), fom, tom);
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentPersonopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Saksopplysning saksopplysning = tpsFasade.hentPersonMedAdresse(registeropplysningerRequest.getFnr());
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentMedlemskapsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        Saksopplysning saksopplysning = medlPeriodeService.hentPeriodeListe(registeropplysningerRequest.getFnr(), fom.minusYears(medlemskaphistorikkAntallÅr), tom);
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentInntektsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws TekniskException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getTom();

        Periode periodeForYtelser = hentPeriodeForYtelser(fom, tom);
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

        Periode periodeForYtelser = hentPeriodeForYtelser(fom, tom);
        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd(registeropplysningerRequest.getFnr(), periodeForYtelser.fom.atDay(1), periodeForYtelser.tom.atDay(1));

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentOrganisasjonsopplysninger(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
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
        Saksopplysning saksopplysning = tpsFasade.hentPersonhistorikk(registeropplysningerRequest.getFnr(), registeropplysningerRequest.getFom());
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentSakOgBehandlingSaker(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws IkkeFunnetException, IntegrasjonException {
        String aktørId = tpsFasade.hentAktørIdForIdent(registeropplysningerRequest.getFnr());
        Saksopplysning saksopplysning = sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(aktørId);

        return List.of(saksopplysning);
    }

    private static Periode hentPeriodeForYtelser(LocalDate fom, LocalDate tom) {

        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if (tom == null) {
            fomMnd = YearMonth.from(fom);
            tomMnd = YearMonth.from(fom.plusYears(2));
        } else if (fom.isBefore(nå) && tom.isAfter(nå)) { //1. Periode påbegynt: utbetalinger periode med 2 mnd tilbake
            fomMnd = YearMonth.from(fom.minusMonths(2L));
            tomMnd = YearMonth.from(tom);
        } else if (fom.isAfter(nå)) { //2. Periode ikke påbegynt. Inneværende mnd og 2 mnd tilbake
            fomMnd = YearMonth.from(nå.minusMonths(2L));
            tomMnd = YearMonth.from(nå);
        } else { //3. Avsluttet: sjekker hele periode
            fomMnd = YearMonth.from(fom);
            tomMnd = YearMonth.from(tom);
        }

        return new Periode(fomMnd, tomMnd);
    }

    private static final class Periode {
        private YearMonth fom;
        private YearMonth tom;

        Periode(YearMonth fom, YearMonth tom) {
            this.fom = fom;
            this.tom = tom;
        }
    }

    @FunctionalInterface
    private interface HentRegisteropplysninger {
        List<Saksopplysning> hent(RegisteropplysningerRequest registeropplysningerRequest, Behandling behandling) throws MelosysException;
    }
}
