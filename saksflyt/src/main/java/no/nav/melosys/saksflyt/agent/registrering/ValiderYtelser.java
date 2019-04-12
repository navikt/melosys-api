package no.nav.melosys.saksflyt.agent.registrering;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.*;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ValiderYtelser extends RegistreringUnntakValiderer {

    private static final Logger log = LoggerFactory.getLogger(ValiderYtelser.class);

    private final InntektService inntektService;

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
    @Transactional(rollbackFor = MelosysException.class)
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

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
        if (!validerInntekt(inntektDokument)) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.MOTTAR_YTELSER);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    private Saksopplysning hentInntektListe(String fnr, LocalDate fom, LocalDate tom) throws SikkerhetsbegrensningException, IntegrasjonException {

        YearMonth fra, til;

        LocalDate nå = LocalDate.now();
        if(tom == null) {
            fra = YearMonth.from(fom);
            til = null;
        } else if (fom.isBefore(nå) && tom.isAfter(nå)) { //1. Periode påbegynt: utbetalinger periode med 2 mnd tilbake
            fra = YearMonth.from(fom.minusMonths(2L));
            til = YearMonth.from(tom);
        } else if (fom.isAfter(nå)) { //2. Periode ikke påbegynt. Inneværende mnd og 2 mnd tilbake
            fra = YearMonth.from(nå.minusMonths(2L));
            til = YearMonth.from(nå);
        } else { //3. Avsluttet: sjekker hele periode
            fra = YearMonth.from(fom);
            til = YearMonth.from(tom);
        }

        return inntektService.hentInntektListe(fnr, fra, til);
    }

    private boolean validerInntekt(InntektDokument inntektDokument) {
        return inntektDokument == null || inntektDokument.getArbeidsInntektMaanedListe().isEmpty();
    }
}
