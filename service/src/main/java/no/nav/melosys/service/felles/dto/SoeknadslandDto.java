package no.nav.melosys.service.felles.dto;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Landkoder;

public class SoeknadslandDto {
    @JsonProperty("landkoder")
    private List<String> landkoder;
    @JsonProperty("erUkjenteEllerAlleEosLand")
    private boolean erUkjenteEllerAlleEosLand;

    public SoeknadslandDto() {
    }

    public SoeknadslandDto(List<String> landkoder, boolean erUkjenteEllerAlleEosLand) {
        this.landkoder = landkoder;
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
    }

    public List<String> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<String> landkoder) {
        this.landkoder = landkoder;
    }

    public boolean erUkjenteEllerAlleEosLand() {
        return erUkjenteEllerAlleEosLand;
    }

    public void setErUkjenteEllerAlleEosLand(boolean erUkjenteEllerAlleEosLand) {
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
    }

    public boolean erGyldig() {
        if (getLandkoder() == null) return false;
        if (erUkjenteEllerAlleEosLand()) return getLandkoder().isEmpty();
        return !getLandkoder().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoeknadslandDto)) return false;
        SoeknadslandDto that = (SoeknadslandDto) o;
        return this.erUkjenteEllerAlleEosLand == that.erUkjenteEllerAlleEosLand &&
            this.getLandkoder().equals(that.getLandkoder());
    }

    public static SoeknadslandDto av(Soeknadsland søknadsland) {
        return new SoeknadslandDto(søknadsland.landkoder, søknadsland.erUkjenteEllerAlleEosLand);
    }

    public static SoeknadslandDto av(Landkoder landkode) {
        List<String> landkoder = landkode != null ? Collections.singletonList(landkode.getKode()) : Collections.emptyList();
        return new SoeknadslandDto(landkoder, false);
    }
}
