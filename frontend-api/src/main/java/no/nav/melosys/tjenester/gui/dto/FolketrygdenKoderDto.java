package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class FolketrygdenKoderDto {

    public List<Trygdedekninger> trygdedekninger;

    public List<Trygdedekninger> getTrygdedekninger() {
        return trygdedekninger;
    }

    public void setTrygdedekninger(List<Trygdedekninger> trygdedekninger) {
        this.trygdedekninger = trygdedekninger;
    }
}
