package no.nav.melosys.saksflyt.felles;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HentOpplysningerFelles { // todo -> HentOpplysningerService ? RegisteropplysningerService ? HentRegisteropplysningerService
    private static final Logger log = LoggerFactory.getLogger(HentOpplysningerFelles.class);

    private final Map<SaksopplysningType, ThrowingFunction<RegisteropplysningerRequest, List<Saksopplysning>, MelosysException>> SAKSOPPLYSNING_TYPE_CONSUMER_MAP =
        Maps.immutableEnumMap(ImmutableMap.<SaksopplysningType, ThrowingFunction<RegisteropplysningerRequest, List<Saksopplysning>, MelosysException>>builder()
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
    private final MedlFasade medlFasade;
    private final EregFasade eregFasade;
    private final AaregFasade aaregFasade;
    private final SakOgBehandlingFasade sakOgBehandlingFasade;
    private final InntektService inntektService;
    private final UtbetaldataService utbetaldataService;
    private final SaksopplysningerService saksopplysningerService;
    private final Integer arbeidsforholdhistorikkAntallMåneder;
    private final Integer medlemskaphistorikkAntallÅr;

    @Autowired
    public HentOpplysningerFelles(@Qualifier("system") TpsFasade tpsFasade,
                                  MedlFasade medlFasade,
                                  EregFasade eregFasade,
                                  AaregFasade aaregFasade,
                                  SakOgBehandlingFasade sakOgBehandlingFasade,
                                  InntektService inntektService,
                                  UtbetaldataService utbetaldataService,
                                  SaksopplysningerService saksopplysningerService,
                                  @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}") Integer arbeidsforholdhistorikkAntallMåneder,
                                  @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr
    ) {
        this.tpsFasade = tpsFasade;
        this.medlFasade = medlFasade;
        this.eregFasade = eregFasade;
        this.aaregFasade = aaregFasade;
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        this.inntektService = inntektService;
        this.utbetaldataService = utbetaldataService;
        this.saksopplysningerService = saksopplysningerService;
        this.arbeidsforholdhistorikkAntallMåneder = arbeidsforholdhistorikkAntallMåneder;
        this.medlemskaphistorikkAntallÅr = medlemskaphistorikkAntallÅr;
    }

    public void hentOgLagreOpplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws MelosysException {
        for (var opplysningstype : registeropplysningerRequest.getOpplysningstyper()) {
            if (!SAKSOPPLYSNING_TYPE_CONSUMER_MAP.containsKey(opplysningstype)) {
                throw new TekniskException("Støtter ikke å hente opplysninger for saksopplysningType " + opplysningstype);
            }

            List<Saksopplysning> saksopplysninger = hentSaksopplysninger(opplysningstype, registeropplysningerRequest);
            lagreSaksopplysninger(saksopplysninger, registeropplysningerRequest.getBehandling());

            log.info("{} hentet for behandling {}", opplysningstype.getBeskrivelse(), registeropplysningerRequest.getBehandling().getId());
        }
    }

    private List<Saksopplysning> hentSaksopplysninger(SaksopplysningType opplysningstype, RegisteropplysningerRequest registeropplysningerRequest) throws MelosysException {
        return SAKSOPPLYSNING_TYPE_CONSUMER_MAP.get(opplysningstype).apply(registeropplysningerRequest);
    }

    private void lagreSaksopplysninger(List<Saksopplysning> saksopplysninger, Behandling behandling) {
        for (Saksopplysning saksopplysning : saksopplysninger) {
            saksopplysningerService.lagreSaksopplysning(saksopplysning, behandling);
        }
    }

    private List<Saksopplysning> hentArbeidsforholdopplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws TekniskException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getFom();

        if (PeriodeKontroller.feilIPeriode(fom, tom)) {
            log.info("Kunne ikke hente arbeidsforholdopplysninger grunnet feil i periode");
            return Collections.emptyList();
        }

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

    private List<Saksopplysning> hentPersonopplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Saksopplysning saksopplysning = tpsFasade.hentPersonMedAdresse(registeropplysningerRequest.getFnr());
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentMedlemskapsopplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getFom();

        if (PeriodeKontroller.feilIPeriode(fom, tom)) {
            log.info("Kunne ikke hente medlemskapsopplysninger grunnet feil i periode");
            return Collections.emptyList();
        }

        Saksopplysning saksopplysning = medlFasade.hentPeriodeListe(registeropplysningerRequest.getFnr(), fom.minusYears(medlemskaphistorikkAntallÅr), tom);
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentInntektsopplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws TekniskException, SikkerhetsbegrensningException {
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getFom();

        if (PeriodeKontroller.feilIPeriode(fom, tom)) {
            log.info("Kunne ikke hente inntektopplysninger grunnet feil i periode");
            return Collections.emptyList();
        }

        Periode periodeForYtelser = hentPeriodeForYtelser(fom, tom);
        Saksopplysning saksopplysning = inntektService.hentInntektListe(registeropplysningerRequest.getFnr(), periodeForYtelser.fom, periodeForYtelser.tom);

        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentUtbetalingsopplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws FunksjonellException, TekniskException {
        Behandling behandling = registeropplysningerRequest.getBehandling();
        LocalDate fom = registeropplysningerRequest.getFom();
        LocalDate tom = registeropplysningerRequest.getFom();

        if (PeriodeKontroller.feilIPeriode(fom, tom)) {
            log.info("Kunne ikke hente utbetalingsopplysninger grunnet feil i periode");
            return Collections.emptyList();
        }

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

    private List<Saksopplysning> hentOrganisasjonsopplysninger(RegisteropplysningerRequest registeropplysningerRequest) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        Behandling behandling = registeropplysningerRequest.getBehandling();
        Set<String> orgnumre = new HashSet<>();

        Optional<SaksopplysningDokument> arbeidsforholdDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.ARBFORH); // TODO: må teste om vi kan hente i samme transaksjon
        Optional<SaksopplysningDokument> inntektDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.INNTK);

        arbeidsforholdDokument.ifPresent(dokument -> orgnumre.addAll(((ArbeidsforholdDokument) dokument).hentOrgnumre()));
        inntektDokument.ifPresent(dokument -> orgnumre.addAll(((InntektDokument) dokument).hentOrgnumre()));

        List<Saksopplysning> saksopplysninger = new ArrayList<>();
        for (String orgnr : orgnumre) {
            saksopplysninger.add(eregFasade.hentOrganisasjon(orgnr));
        }

        return saksopplysninger;
    }

    private List<Saksopplysning> hentPersonhistorikk(RegisteropplysningerRequest registeropplysningerRequest) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Saksopplysning saksopplysning = tpsFasade.hentPersonhistorikk(registeropplysningerRequest.getFnr(), registeropplysningerRequest.getFom());
        return List.of(saksopplysning);
    }

    private List<Saksopplysning> hentSakOgBehandlingSaker(RegisteropplysningerRequest registeropplysningerRequest) throws IkkeFunnetException, IntegrasjonException {
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
    private interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }
}
