package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("oppdaterSaksrelasjonSedMottak")
public class OppdaterSaksrelasjon implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterSaksrelasjon.class);

    private final EessiService eessiService;

    public OppdaterSaksrelasjon(@Qualifier("system") EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPDATER_SAKSRELASJON;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        Long gsakSaksnummer = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        String rinaSaksnummer = melosysEessiMelding.getRinaSaksnummer();
        String bucType = melosysEessiMelding.getBucType();

        log.info("Lagrer saksrelasjon: gsakSaksnummer {}, rinasaksnummer {}, buctype {}", gsakSaksnummer, rinaSaksnummer, bucType);
        eessiService.lagreSaksrelasjon(gsakSaksnummer, rinaSaksnummer, bucType);

        if (prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(ProsessSteg.SED_GENERELL_SAK_HENT_PERSON);
        } else {
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        }
    }
}
