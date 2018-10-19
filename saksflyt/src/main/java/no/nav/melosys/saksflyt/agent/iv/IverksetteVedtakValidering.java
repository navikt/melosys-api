package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.OPPGAVE_ID;
import static no.nav.melosys.domain.ProsessSteg.IV_FERDIGSTILLOPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.IV_VALIDERING;

/**
 * Avslutter en oppgave i GSAK.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK:
 *     JFR_AVSLUTT_OPPGAVE -> JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 *     JFR_AVSLUTT_OPPGAVE -> JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksetteVedtakValidering extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksetteVedtakValidering.class);


    @Autowired
    public IverksetteVedtakValidering() {
        log.info("FerdigStillOppgave initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_VALIDERING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();

        if (behandling.getType() != Behandlingstype.SØKNAD) {
            String feilmelding = "Feil behandlingstype: " + behandling.getType();
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

    }
}
