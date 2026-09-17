package no.nav.melosys.saksflyt;

import java.util.UUID;

import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.context.ApplicationEvent;

/**
 * Publiseres når en prosessinstans går til FEILET.
 *
 * <p>Motstykket til {@link ProsessinstansFerdigEvent} for feilsituasjonen: uten et signal her ville
 * prosessinstanser som står PÅ_VENT bak den feilede aldri blitt sluppet fram, siden opplåsingen
 * skjer i {@link ProsessinstansFerdigListener} på ferdig-eventet. Før MELOSYS-8151 rammet det bare
 * redeliveries av samme skjema; med serialisering per søknadsgruppe ville én feilet del blokkert
 * alle de øvrige delene av søknaden til neste oppstart.
 *
 * <p>Arver bevisst IKKE fra {@link ProsessinstansFerdigEvent}: den eksisterende lytteren ville da
 * fanget dette eventet også, og opplåsing ved feil ville endret oppførsel for alle prosesstyper —
 * ikke bare digital søknad. Se {@link ProsessinstansFerdigListener#prosessinstansFeilet}.
 */
public class ProsessinstansFeiletEvent extends ApplicationEvent {

    public ProsessinstansFeiletEvent(Prosessinstans prosessinstans) {
        super(prosessinstans);
    }

    public Prosessinstans hentProsessinstans() {
        return (Prosessinstans) getSource();
    }

    public UUID getUuid() {
        return hentProsessinstans().getId();
    }

    public UUID getParentId() {
        return hentProsessinstans().getData(ProsessDataKey.PROCESS_PARENT_ID, UUID.class);
    }

    public String getLåsReferanse() {
        return hentProsessinstans().getLåsReferanse();
    }
}
