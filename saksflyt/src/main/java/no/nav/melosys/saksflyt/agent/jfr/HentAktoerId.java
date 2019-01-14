package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_AKTØR_ID;

/**
 * Henter en aktørID
 *
 * Transisjoner:
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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerId);
        prosessinstans.setData(AKTØR_ID, aktørId);

        prosessinstans.setSteg(ProsessSteg.JFR_OPPRETT_SAK_OG_BEH);
        log.info("Hentet aktørId for prosessinstans {}", prosessinstans.getId());
    }
}
