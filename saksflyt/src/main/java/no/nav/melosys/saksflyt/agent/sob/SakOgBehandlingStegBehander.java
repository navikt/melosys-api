package no.nav.melosys.saksflyt.agent.sob;

import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;

import static no.nav.melosys.domain.Tema.MED;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

public abstract class SakOgBehandlingStegBehander extends AbstraktStegBehandler {

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    static BehandlingStatusMapper lagBehandlingStatusMapper(String saksnummer, Long behandlingID, String aktørID) {
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
}
