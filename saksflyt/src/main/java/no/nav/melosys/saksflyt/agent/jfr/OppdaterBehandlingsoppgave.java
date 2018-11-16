package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Oppretter/oppdaterer behandlingsoppgave i Melosys ved mottak av nytt dokument
 * https://confluence.adeo.no/pages/viewpage.action?pageId=264973205
 *
 * Transisjoner:
 * JFR_OPPDATER_BEHANDLINGSOPPGAVE → null hvis alt ok
 * JFR_OPPDATER_BEHANDLINGSOPPGAVE → FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterBehandlingsoppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsoppgave.class);

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    private final GsakFasade gsakFasade;

    @Autowired
    public OppdaterBehandlingsoppgave(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, GsakFasade gsakFasade) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.gsakFasade = gsakFasade;
        log.info("OppdaterBehandlingsstatus initialisert");
    }


    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_OPPDATER_BEHANDLINGSOPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        if (saksnummer == null) {
            String feilmelding = "Prosessinstans " + prosessinstans.getId() + " mangler saksnummer";
            log.error(feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            String feilmelding = "Det finnes ingen fagsak med saksnummer " + saksnummer;
            log.error(feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        } else if (fagsak.getStatus() == Fagsaksstatus.AVSLUTTET) {
            fagsak.setStatus(Fagsaksstatus.OPPRETTET);
        }

        Behandling behandling = fagsak.getAktivBehandling();
        if (behandling == null) {
            behandling = new Behandling();
            behandling.setType(Behandlingstype.NY_VURDERING);

            Aktoer aktør = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
            if (aktør == null || aktør.getAktørId() == null) {
                String feilmelding = "Det finnes ingen bruker på fagsak med saksnummer " + saksnummer;
                log.error(feilmelding);
                håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
                return;
            }

            Long gsakSakId = gsakFasade.opprettSak(saksnummer, Behandlingstype.SØKNAD, aktør.getAktørId());
            fagsak.setGsakSaksnummer(gsakSakId);
        }

        fagsakRepository.save(fagsak);
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        behandlingRepository.save(behandling);

        log.info("Prosessinstans {} har oppdatert behandling {}", prosessinstans.getId(), behandling.getId());
        prosessinstans.setSteg(null);
    }
}
