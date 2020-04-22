package no.nav.melosys.domain.dokument.sed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.jaxb.LovvalgBestemmelseXmlAdapter;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

@XmlRootElement
public class SedDokument implements SaksopplysningDokument {
    private String rinaSaksnummer;
    private String rinaDokumentID;
    private String avsenderID;
    private String fnr;
    private Periode lovvalgsperiode;
    @XmlJavaTypeAdapter(LovvalgBestemmelseXmlAdapter.class)
    private LovvalgBestemmelse lovvalgBestemmelse;
    private Landkoder lovvalgslandKode;
    @XmlJavaTypeAdapter(LovvalgBestemmelseXmlAdapter.class)
    private LovvalgBestemmelse unntakFraLovvalgBestemmelse;
    private Landkoder unntakFraLovvalgslandKode;
    private boolean erEndring;
    private SedType sedType;
    private BucType bucType;
    private List<String> statsborgerskapKoder = new ArrayList<>();
    private List<Arbeidssted> arbeidssteder = new ArrayList<>();
    private boolean erElektronisk = true;

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public void setRinaSaksnummer(String rinaSaksnummer) {
        this.rinaSaksnummer = rinaSaksnummer;
    }

    public String getRinaDokumentID() {
        return rinaDokumentID;
    }

    public void setRinaDokumentID(String rinaDokumentID) {
        this.rinaDokumentID = rinaDokumentID;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public void setAvsenderID(String avsenderID) {
        this.avsenderID = avsenderID;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public Periode getLovvalgsperiode() {
        return lovvalgsperiode;
    }

    public void setLovvalgsperiode(Periode lovvalgsperiode) {
        this.lovvalgsperiode = lovvalgsperiode;
    }

    @XmlTransient
    public LovvalgBestemmelse getLovvalgBestemmelse() {
        return lovvalgBestemmelse;
    }

    public void setLovvalgBestemmelse(LovvalgBestemmelse lovvalgBestemmelse) {
        this.lovvalgBestemmelse = lovvalgBestemmelse;
    }

    public Landkoder getLovvalgslandKode() {
        return lovvalgslandKode;
    }

    public void setLovvalgslandKode(Landkoder lovvalgslandKode) {
        this.lovvalgslandKode = lovvalgslandKode;
    }

    @XmlTransient
    public LovvalgBestemmelse getUnntakFraLovvalgBestemmelse() {
        return unntakFraLovvalgBestemmelse;
    }

    public void setUnntakFraLovvalgBestemmelse(LovvalgBestemmelse unntakFraLovvalgBestemmelse) {
        this.unntakFraLovvalgBestemmelse = unntakFraLovvalgBestemmelse;
    }

    public Landkoder getUnntakFraLovvalgslandKode() {
        return unntakFraLovvalgslandKode;
    }

    public void setUnntakFraLovvalgslandKode(Landkoder unntakFraLovvalgslandKode) {
        this.unntakFraLovvalgslandKode = unntakFraLovvalgslandKode;
    }

    public boolean getErEndring() {
        return erEndring;
    }

    public void setErEndring(boolean erEndring) {
        this.erEndring = erEndring;
    }

    public List<String> getStatsborgerskapKoder() {
        return statsborgerskapKoder;
    }

    public void setStatsborgerskapKoder(List<String> statsborgerskapKoder) {
        this.statsborgerskapKoder = statsborgerskapKoder;
    }

    public List<Arbeidssted> getArbeidssteder() {
        return arbeidssteder;
    }

    public void setArbeidssteder(List<Arbeidssted> arbeidssteder) {
        this.arbeidssteder = arbeidssteder;
    }

    public SedType getSedType() {
        return sedType;
    }

    public void setSedType(SedType sedType) {
        this.sedType = sedType;
    }

    public BucType getBucType() {
        return bucType;
    }

    public void setBucType(BucType bucType) {
        this.bucType = bucType;
    }

    public boolean getErElektronisk() {
        return erElektronisk;
    }

    public void setErElektronisk(boolean erElektronisk) {
        this.erElektronisk = erElektronisk;
    }

    // AvsenderID har format <landkode ISO2>:<institusjonID>
    public Optional<Landkoder> finnAvsenderLand() {
        return Optional.ofNullable(getAvsenderID()).map(id -> Landkoder.valueOf(id.substring(0, 2)));
    }

    public Lovvalgsperiode opprettInnvilgetLovvalgsperiode() {
        Lovvalgsperiode nyLovvalgsperiode = new Lovvalgsperiode();
        nyLovvalgsperiode.setBestemmelse(getLovvalgBestemmelse());
        nyLovvalgsperiode.setFom(getLovvalgsperiode().getFom());
        nyLovvalgsperiode.setTom(getLovvalgsperiode().getTom());
        nyLovvalgsperiode.setLovvalgsland(getLovvalgslandKode());
        nyLovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        if (Landkoder.NO != lovvalgslandKode) {
            nyLovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
            nyLovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);
        } else {
            nyLovvalgsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
            nyLovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        }

        return nyLovvalgsperiode;
    }
}
