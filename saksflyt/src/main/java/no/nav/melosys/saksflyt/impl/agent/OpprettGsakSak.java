package no.nav.melosys.saksflyt.impl.agent;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_OPPDATER_JOURNALPOST;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.GSAK_SAK_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;

/**
 * Oppretter en sak i GSAK.
 *
 * Transisjoner:
 * JFR_OPPRETT_GSAK_SAK -> JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettGsakSak extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(OpprettGsakSak.class);

    GsakFasade gsakFasade;

    @Autowired
    public OpprettGsakSak(Binge binge, ProsessinstansRepository prosessinstansRepo, GsakFasade gsakFasade) {
        super(binge, prosessinstansRepo);
        this.gsakFasade = gsakFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_GSAK_SAK;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String aktørId = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);

        String gsakSakId = gsakFasade.opprettSak(saksnummer, BehandlingType.SØKNAD, aktørId);

        prosessinstans.setData(GSAK_SAK_ID, gsakSakId);
        prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
    }
}
