package no.nav.melosys.service.oppgave.dto;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Landkoder;

public class SoeknadslandDto {
    public List<String> landkoder;
    public boolean erUkjenteEllerAlleEosLand;

    public SoeknadslandDto(List<String> landkoder, boolean erUkjenteEllerAlleEosLand) {
        this.landkoder = landkoder;
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
    }

    public static SoeknadslandDto av(Soeknadsland søknadsland) {
        return new SoeknadslandDto(søknadsland.landkoder, søknadsland.erUkjenteEllerAlleEosLand);
    }

    public static SoeknadslandDto av(Landkoder landkode) {
        List<String> landkoder = landkode != null ? Collections.singletonList(landkode.getKode()) : Collections.emptyList();
        return new SoeknadslandDto(landkoder, false);
    }
}
