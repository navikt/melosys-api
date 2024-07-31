package no.nav.melosys.integrasjon.kodeverk.impl.dto;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

public class BetydningDto {

    @JsonDeserialize(using = LocalDateDeserializer.class)
    public LocalDate gyldigFra;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    public LocalDate gyldigTil;

    public Map<String, BeskrivelseDto> beskrivelser;
}
