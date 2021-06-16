package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.person.KjoennType;
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

    @Override
    public boolean harIkkeRegistrertAdresse() {
        return bostedsadresse.erTom() &&
            postadresse.erTom() &&
            midlertidigPostadresse.land == null;
    }

    @Override
    public boolean manglerBostedsadresse() {
        return bostedsadresse.erTom();
    }

    @Override
    public Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder) {
        return familiemedlemmer.stream()
            .filter(Familiemedlem::erForelder)
            .filter(forelder -> !fnrGjeldendeForelder.equals(forelder.fnr))
            .findAny();
    }

    @Override
    public boolean harBeskyttelsesbehov() {
        return diskresjonskode != null && diskresjonskode.erKode6();
    }

    @Override
    public Set<Land> hentAlleStatsborgerskap() {
        return Set.of(statsborgerskap);
    }

    @Override
    public KjoennType hentKjønnType() {
        return KjoennType.avKode(kjønn.getKode());
    }

    @Override
    public String hentFolkeregisterIdent() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    @Override
    public Sivilstand getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(Sivilstand sivilstand) {
        this.sivilstand = sivilstand;
    }

    @Override
    public LocalDate getSivilstandGyldighetsperiodeFom() {
        return sivilstandGyldighetsperiodeFom;
    }

    public void setSivilstandGyldighetsperiodeFom(LocalDate sivilstandGyldighetsperiodeFom) {
        this.sivilstandGyldighetsperiodeFom = sivilstandGyldighetsperiodeFom;
    }

    /** Kodeverk: Landkoder */
    @Override
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

    @Override
    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    @Override
    public String getMellomnavn() {
        return mellomnavn;
    }

    public void setMellomnavn(String mellomnavn) {
        this.mellomnavn = mellomnavn;
    }

    @Override
    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }

    @Override
    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    @Override
    public List<Familiemedlem> getFamiliemedlemmer() {
        return familiemedlemmer;
    }

    public void setFamiliemedlemmer(List<Familiemedlem> familiemedlemmer) {
        this.familiemedlemmer = familiemedlemmer;
    }

    @Override
    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    @Override
    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    @Override
    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    public void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    @Override
    public Personstatus getPersonstatus() {
        return personstatus;
    }

    public void setPersonstatus(Personstatus personstatus) {
        this.personstatus = personstatus;
    }

    @Override
    public LocalDate getStatsborgerskapDato() {
        return statsborgerskapDato;
    }

    public void setStatsborgerskapDato(LocalDate statsborgerskapDato) {
        this.statsborgerskapDato = statsborgerskapDato;
    }

    @Override
    public Bostedsadresse getBostedsadresse() {
        return bostedsadresse;
    }

    public void setBostedsadresse(Bostedsadresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

    @Override
    public UstrukturertAdresse getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(UstrukturertAdresse postadresse) {
        this.postadresse = postadresse;
    }

    @Override
    public MidlertidigPostadresse getMidlertidigPostadresse() {
        return midlertidigPostadresse;
    }

    public void setMidlertidigPostadresse(MidlertidigPostadresse midlertidigPostadresse) {
        this.midlertidigPostadresse = midlertidigPostadresse;
    }

    @Override
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
