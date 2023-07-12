package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collection;

import no.nav.melosys.domain.kodeverk.Mottakerroller;

public class MottakerDto {
    private String type;
    private Mottakerroller rolle;
    private boolean orgnrSettesAvSaksbehandler;
    private Collection<MottakerAdresseDto> adresser;
    private MottakerFeilmeldingDto mottakerFeilmeldingDto;

    public MottakerDto() {
    }

    public MottakerDto(String type, Mottakerroller rolle, boolean orgnrSettesAvSaksbehandler, Collection<MottakerAdresseDto> adresser, MottakerFeilmeldingDto mottakerFeilmeldingDto) {
        this.type = type;
        this.rolle = rolle;
        this.orgnrSettesAvSaksbehandler = orgnrSettesAvSaksbehandler;
        this.adresser = adresser;
        this.mottakerFeilmeldingDto = mottakerFeilmeldingDto;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRolle(Mottakerroller rolle) {
        this.rolle = rolle;
    }

    public void setOrgnrSettesAvSaksbehandler(boolean orgnrSettesAvSaksbehandler) {
        this.orgnrSettesAvSaksbehandler = orgnrSettesAvSaksbehandler;
    }

    public void setAdresser(Collection<MottakerAdresseDto> adresser) {
        this.adresser = adresser;
    }

    public void setFeilmelding(MottakerFeilmeldingDto mottakerFeilmeldingDto) {
        this.mottakerFeilmeldingDto = mottakerFeilmeldingDto;
    }

    public String getType() {
        return type;
    }

    public Mottakerroller getRolle() {
        return rolle;
    }

    public boolean getOrgnrSettesAvSaksbehandler() {
        return orgnrSettesAvSaksbehandler;
    }

    public Collection<MottakerAdresseDto> getAdresser() {
        return adresser;
    }

    public MottakerFeilmeldingDto getFeilmelding() {
        return mottakerFeilmeldingDto;
    }
}
