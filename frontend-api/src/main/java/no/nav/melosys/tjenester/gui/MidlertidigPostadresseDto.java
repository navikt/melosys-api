package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.melosys.tjenester.gui.dto.StrukturertAdresseDto;
import no.nav.melosys.tjenester.gui.dto.UstrukturertAdresseDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MidlertidigPostadresseDto {

    public enum Adressetype {
        STRUKTURERT, USTRUKTURERT
    }

    public Adressetype adressetype;

    public StrukturertAdresseDto strukturertAdresse;

    public UstrukturertAdresseDto ustrukturertAdresse;
}
