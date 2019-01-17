package no.nav.melosys.service.journalforing.dto;

import no.nav.melosys.domain.Behandlingstype;

public class JournalfoeringTilordneDto extends JournalfoeringDto {
    private String saksnummer; // Melosys saksnummer
    private Behandlingstype behandlingstype;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Behandlingstype getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstype behandlingstype) {
        this.behandlingstype = behandlingstype;
    }
}
