package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_AKTØR_ID;

/**
 * Henter en aktørID
 *
 * Transisjoner:
 *  a) Behandlingtypen er ENDRET_PERIODE.
 * JFR_AKTOER_ID -> JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 *  b) Ellers.
 * JFR_AKTOER_ID -> JFR_OPPRETT_SAK_OG_BEH eller FEILET_MASKINELT hvis feil
 */
@Component
public class HentAktoerId extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentAktoerId.class);

    private final TpsFasade tpsFasade;

    @Autowired
    public HentAktoerId(TpsFasade tpsFasade) {
        this.tpsFasade = tpsFasade;
        log.info("HentAktoerId initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_AKTØR_ID;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerId);
        prosessinstans.setData(AKTØR_ID, aktørId);

        Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
        if (behandlingstype.equals(Behandlingstyper.ENDRET_PERIODE)) {
            prosessinstans.setSteg(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
        } else {
            prosessinstans.setSteg(ProsessSteg.JFR_OPPRETT_SAK_OG_BEH);
        }
        log.info("Hentet aktørId for prosessinstans {}", prosessinstans.getId());
    }
}
