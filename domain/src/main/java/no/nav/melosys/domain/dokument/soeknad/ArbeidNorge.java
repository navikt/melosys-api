package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Opplysninger om arbeid i Norge
 */
public class ArbeidNorge {
    public List<String> valgteArbeidsforhold = new ArrayList<>();
    public Boolean arbeidsforholdOpprettholdIHelePerioden;
    @JsonProperty("brukerErSelvstendigNaeringsdrivende")
    public Boolean brukerErSelvstendigNæringsdrivende;
    public Boolean selvstendigFortsetterEtterArbeidIUtlandet;
    @JsonProperty("arbeidsforholdVikarNavn")
    public String brukerArbeiderIVikarbyrå; //FIXME: Fjernes ?
    public String vikarOrgnr;
    public String flyendePersonellHjemmebase;
    @JsonProperty("ansattPaSokkelEllerSkip")
    public String ansattPaSokkelEllerSkip; //FIXME: boolean?
    public String navnSkipEllerSokkel;
    public String sokkelLand; //FIXME: Land?
    @JsonProperty("skipFartsomrade")
    public String skipFartsområde;
    public String skipFlaggLand;
    public String kontaktNavn;
    public String kontaktEpost;
    public String fullmektigFirma;
    public String fullmektigAdresse;
}
