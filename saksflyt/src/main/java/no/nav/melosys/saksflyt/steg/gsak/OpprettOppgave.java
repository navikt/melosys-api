package no.nav.melosys.saksflyt.steg.gsak;

import java.util.Optional;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.AbstraktOpprettBehandlingsoppgave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.SEND_FORVALTNINGSMELDING;

/**
 * Oppretter en oppgave i GSAK.
 */
@Component
public class OpprettOppgave extends AbstraktOpprettBehandlingsoppgave {
    private static final String STØTTES_IKKE = " er ikke støttet";

    @Autowired
    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        super(gsakFasade);
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GSAK_OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        validerSakstype(prosessinstans.getBehandling().getFagsak().getType());
        super.opprettOppgave(prosessinstans);

        if (prosessinstans.getType() == ProsessType.JFR_NY_SAK) {
            boolean skalSendesForvaltningsmelding =
                Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).orElse(true);
            if (skalSendesForvaltningsmelding) {
                prosessinstans.setSteg(SEND_FORVALTNINGSMELDING);
            } else {
                prosessinstans.setSteg(ProsessSteg.FERDIG);
            }
        } else if (prosessinstans.getType() == ProsessType.JFR_NY_BEHANDLING) {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        } else {
            String feilmelding = prosessinstans.getId() + ":" + System.lineSeparator()
                + "prosessType " + prosessinstans.getType() + STØTTES_IKKE;
            throw new TekniskException(feilmelding);
        }
    }

    private void validerSakstype(Sakstyper sakstype) throws FunksjonellException {
        if (sakstype != Sakstyper.EU_EOS) {
            throw new FunksjonellException("Sakstyper " + sakstype + STØTTES_IKKE);
        }
    }
}
