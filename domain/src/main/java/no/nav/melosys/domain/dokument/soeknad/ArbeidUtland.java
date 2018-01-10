package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Opplysninger om arbeid i utlandet
 */
public class ArbeidUtland {
    public List<Land> arbeidsland;
    public Periode arbeidsperiode;
    public BigDecimal arbeidsandelNorge;
    public BigDecimal arbeidsandelUtland;
    public Gateadresse arbeidsstedUtland; // TODO kan Gateadresse brukes? SED må sjekkes
    public String bostedsland;
    public Boolean erstatterTidligereUtsendt;
}
