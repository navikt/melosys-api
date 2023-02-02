package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collection;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MottakerDto {
    private String type;
    private Aktoersroller rolle;
    private boolean orgnrSettesAvSaksbehandler;
    private Collection<MottakerAdresseDto> adresser;
    private String feilmelding;

    public MottakerDto() {
    }

    public MottakerDto(String type, Aktoersroller rolle, boolean orgnrSettesAvSaksbehandler, Collection<MottakerAdresseDto> adresser, String feilmelding) {
        this.type = type;
        this.rolle = rolle;
        this.orgnrSettesAvSaksbehandler = orgnrSettesAvSaksbehandler;
        this.adresser = adresser;
        this.feilmelding = feilmelding;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRolle(Aktoersroller rolle) {
        this.rolle = rolle;
    }

    public void setOrgnrSettesAvSaksbehandler(boolean orgnrSettesAvSaksbehandler) {
        this.orgnrSettesAvSaksbehandler = orgnrSettesAvSaksbehandler;
    }

    public void setAdresser(Collection<MottakerAdresseDto> adresser) {
        this.adresser = adresser;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }

    public String getType() {
        return type;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public boolean getOrgnrSettesAvSaksbehandler() {
        return orgnrSettesAvSaksbehandler;
    }

    public Collection<MottakerAdresseDto> getAdresser() {
        return adresser;
    }

    public String getFeilmelding() {
        return feilmelding;
    }
}
