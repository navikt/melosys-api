package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;

import java.util.Collection;

public class MottakerDto {
    private String type;
    private Mottakerroller rolle;
    private boolean orgnrSettesAvSaksbehandler;
    private Collection<BrevAdresse> adresser;
    private FeilmeldingDto feilmelding;

    public MottakerDto() {
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

    public void setAdresser(Collection<BrevAdresse> adresser) {
        this.adresser = adresser;
    }

    public void setFeilmelding(FeilmeldingDto feilmeldingDto) {
        this.feilmelding = feilmeldingDto;
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

    public Collection<BrevAdresse> getAdresser() {
        return adresser;
    }

    public FeilmeldingDto getFeilmelding() {
        return feilmelding;
    }
}
