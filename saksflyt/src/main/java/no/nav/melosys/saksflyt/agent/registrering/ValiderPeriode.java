package no.nav.melosys.saksflyt.agent.registrering;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ValiderPeriode extends RegistreringUnntakValiderer {

    private static final Logger log = LoggerFactory.getLogger(ValiderPeriode.class);

    ValiderPeriode(SaksopplysningRepository saksopplysningRepository, AvklartefaktaService avklartefaktaService) {
        super(saksopplysningRepository, avklartefaktaService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_VALIDER_PERIODE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        SedDokument sedDokument = (SedDokument) hentSedSaksopplysning(prosessinstans).getDokument();
        Periode periode = sedDokument.getPeriode();

        if (periode.getTom() == null) {
            registrerFeil(prosessinstans,Unntak_periode_begrunnelser.FEIL_I_PERIODEN);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
        } else if (fomErEtterTom(periode)) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.FEIL_I_PERIODEN);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
        } else if (!periodeInnenfor24Mnd(periode)) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
        } else if (periodeForGammel(periode)) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
        } else {
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
        }
    }

    private boolean fomErEtterTom(Periode periode) {
        return periode.getFom().isAfter(periode.getTom());
    }

    private boolean periodeInnenfor24Mnd(Periode periode) {
        return ChronoUnit.MONTHS.between(periode.getFom(), periode.getTom()) <= 23L;
    }

    private boolean periodeForGammel(Periode periode) {
        return periode.getTom().isBefore(LocalDate.now().minusYears(5L));
    }
}
