package no.nav.melosys.saksflyt.steg.sob;

import java.time.LocalDateTime;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;

import static no.nav.melosys.domain.Tema.MED;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

public abstract class SakOgBehandlingStegBehander extends AbstraktStegBehandler {

    private final SakOgBehandlingFasade sakOgBehandlingFasade;

    protected SakOgBehandlingStegBehander(SakOgBehandlingFasade sakOgBehandlingFasade) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
    }

    private static BehandlingStatusMapper lagBehandlingStatusMapper(String saksnummer, Long behandlingID, String aktørID) {
        // BehandlingsId i SOB skal være unik i NAV, så vi prefikser med applikasjonsID.
        String behandlingsId = String.format("%s-%d", Fagsystem.MELOSYS.getKode(), behandlingID);

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(Fagsystem.MELOSYS.getKode());
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(MED.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        return builder.build();
    }

    protected void sakOgBehandlingOpprettet(String saksnummer, Long behandlingId, String aktørID) throws IntegrasjonException {
        sakOgBehandlingFasade.sendBehandlingOpprettet(lagBehandlingStatusMapper(saksnummer, behandlingId, aktørID));
    }

    protected void sakOgBehandlingAvsluttet(String saksnummer, Long behandlingId, String aktørID) throws IntegrasjonException {
        sakOgBehandlingFasade.sendBehandlingAvsluttet(lagBehandlingStatusMapper(saksnummer, behandlingId, aktørID));
    }
}
