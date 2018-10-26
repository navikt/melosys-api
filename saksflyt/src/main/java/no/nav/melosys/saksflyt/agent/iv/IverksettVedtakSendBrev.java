package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.GSAK_AVSLUTT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.IV_SEND_BREV;

/**
 * Sende ulike brev basert på lovvalgsbestemmelse.
 * <p>
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_SEND_BREV -> GSAK_AVSLUTT_OPPGAVE eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksettVedtakSendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendBrev.class);

    @Autowired
    public IverksettVedtakSendBrev() {
        log.info("IverksetteVedtakSendBrev initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_SEND_BREV;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        ProsessType prosessType = prosessinstans.getType();

        if (ProsessType.IVERKSETT_VEDTAK == prosessType) {
            //FIXME: Mangler send brev implementsjon
            prosessinstans.setSteg(GSAK_AVSLUTT_OPPGAVE);
        } else {
            String feilmelding = "Ukjent prosess type: " + prosessType;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
        }

    }
}
