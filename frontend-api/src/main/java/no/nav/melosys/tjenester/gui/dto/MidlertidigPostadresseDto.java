package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.adresse.UstrukturertAdresse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MidlertidigPostadresseDto {

    public enum Adressetype {
        STRUKTURERT, USTRUKTURERT
    }

    public Adressetype adressetype;

    public StrukturertAdresse strukturertAdresse;

    public UstrukturertAdresse ustrukturertAdresse;
}
