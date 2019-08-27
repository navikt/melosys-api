package no.nav.melosys.saksflyt.steg.jfr;

import java.util.List;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.SoeknadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_SØKNAD;

/**
 * Oppretter en søknad basert på opplysninger fra journalføring.
 *
 * Transisjoner:
 * JFR_OPPRETT_SOEKNAD -> JFR_OPPRETT_GSAK_SAK eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettSoeknad extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettSoeknad.class);

    private final SoeknadService søknadService;

    @Autowired
    public OpprettSoeknad(SoeknadService søknadService) {
        this.søknadService = søknadService;
        log.info("OpprettSoeknad initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        long behandlingID = prosessinstans.getBehandling().getId();

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.periode = periode;
        soeknadDokument.soeknadsland.landkoder = prosessinstans.getData(ProsessDataKey.SØKNADSLAND, List.class);

        søknadService.registrerSøknad(behandlingID, soeknadDokument);

        prosessinstans.setSteg(JFR_OPPRETT_GSAK_SAK);
        log.info("Prosessinstans {} har opprettet søknad for behandling {}.", prosessinstans.getId(), behandlingID);
    }
}
