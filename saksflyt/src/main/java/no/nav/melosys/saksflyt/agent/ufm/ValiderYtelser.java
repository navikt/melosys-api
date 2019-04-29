package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValiderYtelser extends RegistreringUnntakValiderer {

    private static final Logger log = LoggerFactory.getLogger(ValiderYtelser.class);

    private final InntektService inntektService;

    @Autowired
    ValiderYtelser(SaksopplysningRepository saksopplysningRepository, AvklartefaktaService avklartefaktaService, InntektService inntektService) {
        super(saksopplysningRepository, avklartefaktaService);
        this.inntektService = inntektService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_VALIDER_YTELSER;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Instant nå = Instant.now();

        SedDokument sedDokument = (SedDokument) hentSedSaksopplysning(prosessinstans).getDokument();
        String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        LocalDate fom = sedDokument.getPeriode().getFom();
        LocalDate tom = sedDokument.getPeriode().getTom();

        Saksopplysning saksopplysning = hentInntektListe(fnr, fom ,tom);
        saksopplysning.setBehandling(prosessinstans.getBehandling());
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        InntektDokument inntektDokument = (InntektDokument) saksopplysning.getDokument();

        //TODO: skal også sjekke mot UR. Sjekker nå kun mot inntektskomponent. MELOSYS-2496
        if (!validerInntekt(inntektDokument, fom, tom)) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.MOTTAR_YTELSER);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    private Saksopplysning hentInntektListe(String fnr, LocalDate fom, LocalDate tom) throws SikkerhetsbegrensningException, IntegrasjonException {

        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if(tom == null) {
            fomMnd = YearMonth.from(fom);
            tomMnd = null;
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

        return inntektService.hentInntektListe(fnr, fomMnd, tomMnd);
    }

    private boolean validerInntekt(InntektDokument inntektDokument, LocalDate fom, LocalDate tom) {

        YearMonth fra = YearMonth.from(fom);
        YearMonth til = tom != null ? YearMonth.from(tom) : null;

        if(inntektDokument == null || inntektDokument.getArbeidsInntektMaanedListe().isEmpty()) {
            return true;
        }

        for (YtelseFraOffentlige ytelseFraOffentlige : hentYtelseFraOffentlige(inntektDokument)) {
            if (erUtbetaltIPeriode(ytelseFraOffentlige, fra, til)) {
                return false;
            }
        }

        return true;
    }

    private boolean erUtbetaltIPeriode(YtelseFraOffentlige ytelseFraOffentlige, YearMonth fom, YearMonth tom) {
        YearMonth utbetaltIPeriode = ytelseFraOffentlige.utbetaltIPeriode;

        if (utbetaltIPeriode == null) {
            return false;
        }

        if (tom == null) {
            tom = fom.plusYears(2);
        }

        if (utbetaltIPeriode.isAfter(fom) && utbetaltIPeriode.isBefore(tom)) {
            return true;
        } else {
            return utbetaltIPeriode.equals(fom) || utbetaltIPeriode.equals(tom);
        }
    }

    private Collection<YtelseFraOffentlige> hentYtelseFraOffentlige(InntektDokument inntektDokument) {
        return inntektDokument.getArbeidsInntektMaanedListe().stream()
            .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
            .filter(Objects::nonNull)
            .map(ArbeidsInntektInformasjon::getInntektListe)
            .flatMap(Collection::stream)
            .filter(YtelseFraOffentlige.class::isInstance)
            .map(YtelseFraOffentlige.class::cast)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
