package no.nav.melosys.service.felles.dto;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Landkoder;

public class SoeknadslandDto {
    private List<String> landkoder;
    private boolean erUkjenteEllerAlleEosLand;

    public SoeknadslandDto() {
    }

    public SoeknadslandDto(@JsonProperty("landkoder") List<String> landkoder, @JsonProperty("erUkjenteEllerAlleEosLand") boolean erUkjenteEllerAlleEosLand) {
        this.landkoder = landkoder;
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
    }

    public List<String> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<String> landkoder) {
        this.landkoder = landkoder;
    }

    public boolean isErUkjenteEllerAlleEosLand() {
        return erUkjenteEllerAlleEosLand;
    }

    public void setErUkjenteEllerAlleEosLand(boolean erUkjenteEllerAlleEosLand) {
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
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
