package no.nav.melosys.domain.dokument.soeknad;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Opplysninger om arbeid i Norge
 */
public class ArbeidNorge {
    public Boolean arbeidsforholdOpprettholdIHelePerioden;
    @JsonProperty("brukerErSelvstendigNaeringsdrivende")
    public Boolean brukerErSelvstendigNæringsdrivende;
    public Boolean selvstendigFortsetterEtterArbeidIUtlandet;
    @JsonProperty("brukerArbeiderIVikarbyra")
    public Boolean brukerArbeiderIVikarbyrå; //TODO fjernes?
    public String vikarOrgnr;
    public String flyendePersonellHjemmebase;
    @JsonProperty("ansattPaSokkelEllerSkip")
    public String ansattPaSokkelEllerSkip; //TODO boolean?
    public String navnSkipEllerSokkel;
    public String sokkelLand; //TODO Landkode?
    @JsonProperty("skipFartsomrade")
    public String skipFartsområde;
    public String skipFlaggLand;
    public List<String> valgteArbeidsforhold;
}
