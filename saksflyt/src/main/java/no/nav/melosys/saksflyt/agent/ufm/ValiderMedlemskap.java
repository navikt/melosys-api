package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValiderMedlemskap extends RegistreringUnntakValiderer {

    private static final Logger log = LoggerFactory.getLogger(ValiderMedlemskap.class);

    private final MedlFasade medlFasade;

    @Autowired
    ValiderMedlemskap(SaksopplysningRepository saksopplysningRepository,
                      AvklartefaktaService avklartefaktaService, MedlFasade medlFasade) {
        super(saksopplysningRepository, avklartefaktaService);
        this.medlFasade = medlFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Instant nå = Instant.now();
        boolean erEndring = prosessinstans.getData(ProsessDataKey.ER_ENDRING, Boolean.class);

        SedDokument sedDokument = (SedDokument) hentSedSaksopplysning(prosessinstans).getDokument();
        String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        LocalDate fom = sedDokument.getPeriode().getFom();
        LocalDate tom = sedDokument.getPeriode().getTom();

        Saksopplysning saksopplysningMedlemskap = medlFasade.hentPeriodeListe(fnr, fom, tom);
        saksopplysningMedlemskap.setBehandling(prosessinstans.getBehandling());
        saksopplysningMedlemskap.setRegistrertDato(nå);
        saksopplysningMedlemskap.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysningMedlemskap);

        if (!erEndring && harOverlappendePerioder((MedlemskapDokument) saksopplysningMedlemskap.getDokument(), fom, tom)) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    private boolean harOverlappendePerioder(MedlemskapDokument medlemskapDokument, LocalDate fom, LocalDate tom) throws TekniskException {
        //Kan motta SED med åpen periode
        if (tom == null) {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (fom.isBefore(periode.getTom())) return true;
            }
        } else {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (fom.isAfter(periode.getFom()) && fom.isBefore(periode.getTom())) return true;
                if (tom.isAfter(periode.getFom()) && tom.isBefore(periode.getTom())) return true;
                if (fom.isBefore(periode.getFom()) && tom.isAfter(periode.getTom())) return true;
            }
        }
        return false;
    }
}
