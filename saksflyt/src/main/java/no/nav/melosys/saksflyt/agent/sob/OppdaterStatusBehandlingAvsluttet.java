package no.nav.melosys.saksflyt.agent.sob;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static no.nav.melosys.domain.RolleType.BRUKER;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

/**
 * Steget sørger for å skrive til Sak og Behandling når en behandling avsluttes
 *
 * Transisjoner:
 * IV_STATUS_BEH_AVSL → null hvis alt ok
 * IV_STATUS_BEH_AVSL → FEILET_MASKINELT hvis oppdatering av status feilet
 */
@Component
public class OppdaterStatusBehandlingAvsluttet extends SakOgBehandlingStegBehander {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingAvsluttet.class);

    private final SakOgBehandlingFasade sakOgBehandlingFasade;

    public OppdaterStatusBehandlingAvsluttet(SakOgBehandlingFasade sakOgBehandlingFasade) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        log.info("OppdaterStatusBehandlingAvsluttet initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return IV_STATUS_BEH_AVSL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        Aktoer aktør = fagsak.hentAktørMedRolleType(BRUKER);
        String aktørID = aktør.getAktørId();

        Tema arkivtema = avgjørArkivTema(behandling.getType());

        // BehandlingsId i SOB skal være unik i NAV, så vi prefikser med applikasjonsID.
        String behandlingsId = String.format("%s-%d", Fagsystem.MELOSYS.getKode(), behandling.getId());
        
        // FIXME: Nullsjekk på noe her?

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(Fagsystem.MELOSYS.getKode());
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(arkivtema.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        sakOgBehandlingFasade.sendBehandlingAvsluttet(builder.build());

        prosessinstans.setSteg(null);
        log.info("Oppdatert sob-status til avsluttet for prosessinstans {}", prosessinstans.getId());
    }
}
