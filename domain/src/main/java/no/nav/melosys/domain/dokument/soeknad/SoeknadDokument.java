package no.nav.melosys.domain.dokument.soeknad;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Landkode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;

/**
 * FIXME: EESSI2-424. Flat struktur, tatt ut fra https://confluence.adeo.no/pages/viewpage.action?pageId=239369913
 * Avventer Json struktur før vi gjør noe mer
 */
@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument implements HarPeriode {
    
    // Personopplysninger
    public String sammensattNavn;
    public String fnr;
    public LocalDate fødselsdato;
    public Bostedsadresse bostedsadresseNorge;
    public Bostedsadresse bostedsadresseUtland;
    public Landkode statsborgerskap;
    public String utenlandskId;
    // FIXME: Barn?
    
    // Opplysninger om arbeid i utlandet
    public Iterable<Landkode> arbeidsland; // FIXME
    public Periode arbeidsperiode;
    public Gateadresse arbeidsstedUtland;
    public BigDecimal arbeidsandelNorge;
    public BigDecimal arbeidsanderUtland;
    // FIXME: Bostedsland?
    public Boolean erstatterTidligereUtsendt;
    
    // Opplysninger om foretak i utlandet
    // TODO
    
    // Opplysninger om opphold i utland
    public Landkode oppholdsland;
    public Periode oppholdsPeriode;
    // TODO: resten
    
    // Opplysninger om arbeid i Norge
    public Boolean brukerErSelvstendigNæringsdrivende;
    public Boolean brukerArbeiderIVikarbyrå;
    public Boolean ansattPåSokkelEllerSkip;
    public String navnSkipEllerSokkel;
    public Landkode sokkelLand;
    public Landkode skipFlaggLand;
    // TODO: resten
    
    // Opplysninger om juridiske arbeidsgivere i Norge
    // TODO
    
    // Opplysninger om arbeidsinntekt
    // TODO
    
    @Override
    public ErPeriode getPeriode() {
        return oppholdsPeriode;
    }

}
