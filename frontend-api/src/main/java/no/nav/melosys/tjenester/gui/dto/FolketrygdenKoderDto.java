package no.nav.melosys.tjenester.gui.dto;

import java.util.Collection;

import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class FolketrygdenKoderDto {

    private final Collection<Trygdedekninger> trygdedekninger;

    public FolketrygdenKoderDto(Collection<Trygdedekninger> trygdedekninger) {
        this.trygdedekninger = trygdedekninger;
    }

    public Collection<Trygdedekninger> getTrygdedekninger() {
        return trygdedekninger;
    }
}
