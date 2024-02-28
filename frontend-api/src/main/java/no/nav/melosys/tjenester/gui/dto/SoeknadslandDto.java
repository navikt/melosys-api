package no.nav.melosys.tjenester.gui.dto;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Landkoder;

public class SoeknadslandDto {
    public List<String> landkoder;
    public boolean flereLandUkjentHvilke;

    public SoeknadslandDto(List<String> landkoder, boolean flereLandUkjentHvilke) {
        this.landkoder = landkoder;
        this.flereLandUkjentHvilke = flereLandUkjentHvilke;
    }

    public static SoeknadslandDto av(Soeknadsland søknadsland) {
        if (søknadsland == null) {
            return new SoeknadslandDto(Collections.emptyList(), false);
        }
        return new SoeknadslandDto(søknadsland.getLandkoder(), søknadsland.isFlereLandUkjentHvilke());
    }

    public static SoeknadslandDto av(Landkoder landkode) {
        List<String> landkoder = landkode != null ? Collections.singletonList(landkode.getKode()) : Collections.emptyList();
        return new SoeknadslandDto(landkoder, false);
    }
}
