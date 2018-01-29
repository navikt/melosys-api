package no.nav.melosys.domain.dokument.soeknad;

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
    public Boolean brukerArbeiderIVikarbyrå; //FIXME: fjernes?
    public String vikarOrgnr;
    public String flyendePersonellHjemmebase;
    @JsonProperty("ansattPaSokkelEllerSkip")
    public String ansattPaSokkelEllerSkip; //FIXME: boolean?
    public String navnSkipEllerSokkel;
    public String sokkelLand; //FIXME: Land?
    @JsonProperty("skipFartsomrade")
    public String skipFartsområde;
    public String skipFlaggLand;
}
