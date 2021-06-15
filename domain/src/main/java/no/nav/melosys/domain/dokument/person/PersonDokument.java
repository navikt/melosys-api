package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.person.Persondata;


/**
 * Representerer svar fra personregisteret (TPS)
 *
 */
public class PersonDokument implements Persondata, SaksopplysningDokument {
    private String fnr;
    private Sivilstand sivilstand;
    private LocalDate sivilstandGyldighetsperiodeFom;
    private Land statsborgerskap;
    @JsonProperty("kjoenn")
    private KjoennsType kjønn;
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private String sammensattNavn;
    private List<Familiemedlem> familiemedlemmer = new ArrayList<>();
    @JsonProperty("foedselsdato")
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private Diskresjonskode diskresjonskode;
    private Personstatus personstatus;
    private LocalDate statsborgerskapDato;
    private Bostedsadresse bostedsadresse = new Bostedsadresse();
    private UstrukturertAdresse postadresse = new UstrukturertAdresse();
    private MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();
    private UstrukturertAdresse gjeldendePostadresse = new UstrukturertAdresse();
    @JsonProperty(defaultValue = "false" )
    private boolean erEgenAnsatt;

    public boolean harIkkeRegistrertAdresse() {
        return bostedsadresse.erTom() &&
            postadresse.erTom() &&
            midlertidigPostadresse.land == null;
    }

    public boolean manglerBostedsadresse() {
        return bostedsadresse.erTom();
    }

    public Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder) {
        return familiemedlemmer.stream()
            .filter(Familiemedlem::erForelder)
            .filter(forelder -> !fnrGjeldendeForelder.equals(forelder.fnr))
            .findAny();
    }

    public boolean harBeskyttelsesbehov() {
        return diskresjonskode != null && diskresjonskode.erKode6();
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public Sivilstand getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(Sivilstand sivilstand) {
        this.sivilstand = sivilstand;
    }

    public LocalDate getSivilstandGyldighetsperiodeFom() {
        return sivilstandGyldighetsperiodeFom;
    }

    public void setSivilstandGyldighetsperiodeFom(LocalDate sivilstandGyldighetsperiodeFom) {
        this.sivilstandGyldighetsperiodeFom = sivilstandGyldighetsperiodeFom;
    }

    /** Kodeverk: Landkoder */
    public Land getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(Land statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    /** Kodeverk: Kjønnstyper */
    public KjoennsType getKjønn() {
        return kjønn;
    }

    public void setKjønn(KjoennsType kjønn) {
        this.kjønn = kjønn;
    }

    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    public String getMellomnavn() {
        return mellomnavn;
    }

    public void setMellomnavn(String mellomnavn) {
        this.mellomnavn = mellomnavn;
    }

    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public List<Familiemedlem> getFamiliemedlemmer() {
        return familiemedlemmer;
    }

    public void setFamiliemedlemmer(List<Familiemedlem> familiemedlemmer) {
        this.familiemedlemmer = familiemedlemmer;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    public void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public Personstatus getPersonstatus() {
        return personstatus;
    }

    public void setPersonstatus(Personstatus personstatus) {
        this.personstatus = personstatus;
    }

    public LocalDate getStatsborgerskapDato() {
        return statsborgerskapDato;
    }

    public void setStatsborgerskapDato(LocalDate statsborgerskapDato) {
        this.statsborgerskapDato = statsborgerskapDato;
    }

    public Bostedsadresse getBostedsadresse() {
        return bostedsadresse;
    }

    public void setBostedsadresse(Bostedsadresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

    public UstrukturertAdresse getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(UstrukturertAdresse postadresse) {
        this.postadresse = postadresse;
    }

    public MidlertidigPostadresse getMidlertidigPostadresse() {
        return midlertidigPostadresse;
    }

    public void setMidlertidigPostadresse(MidlertidigPostadresse midlertidigPostadresse) {
        this.midlertidigPostadresse = midlertidigPostadresse;
    }

    public UstrukturertAdresse getGjeldendePostadresse() {
        return gjeldendePostadresse;
    }

    public void setGjeldendePostadresse(UstrukturertAdresse gjeldendePostadresse) {
        this.gjeldendePostadresse = gjeldendePostadresse;
    }

    public boolean isErEgenAnsatt() {
        return erEgenAnsatt;
    }

    public void setErEgenAnsatt(boolean erEgenAnsatt) {
        this.erEgenAnsatt = erEgenAnsatt;
    }
}
