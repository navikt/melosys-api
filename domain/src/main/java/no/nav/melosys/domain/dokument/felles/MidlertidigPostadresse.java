package no.nav.melosys.domain.dokument.felles;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MidlertidigPostadresse {

    public enum Adressetype {
        STRUKTURERT, USTRUKTURERT
    }

    public Adressetype adressetype;

    public StrukturertAdresse strukturertAdresse;

    public UstrukturertAdresse ustrukturertAdresse;
}
