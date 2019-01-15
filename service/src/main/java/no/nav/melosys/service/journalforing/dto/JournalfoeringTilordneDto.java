package no.nav.melosys.service.journalforing.dto;

import no.nav.melosys.domain.kodeverk.Behandlingstyper;

public class JournalfoeringTilordneDto extends JournalfoeringDto {
    private String saksnummer; // Melosys saksnummer
    private Behandlingstyper behandlingstype;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }
}
