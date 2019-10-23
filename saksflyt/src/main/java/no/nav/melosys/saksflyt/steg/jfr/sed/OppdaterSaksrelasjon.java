package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("oppdaterSaksrelasjonSedMottak")
public class OppdaterSaksrelasjon extends AbstraktStegBehandler {

    private static Logger log = LoggerFactory.getLogger(OppdaterSaksrelasjon.class);

    private final EessiService eessiService;

    public OppdaterSaksrelasjon(EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPDATER_SAKSRELASJON;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        Long gsakSaksnummer = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        String rinaSaksnummer = melosysEessiMelding.getRinaSaksnummer();
        String bucType = melosysEessiMelding.getBucType();

        log.info("Lagrer saksrelasjon: gsakSaksnummer {}, rinasaksnummer {}, buctype {}", gsakSaksnummer, rinaSaksnummer, bucType);
        eessiService.lagreSaksrelasjon(gsakSaksnummer, rinaSaksnummer, bucType);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }
}
