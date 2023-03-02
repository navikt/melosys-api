package no.nav.melosys.domain.dokument.sed;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.jaxb.LovvalgBestemmelseXmlAdapter;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.*;

@XmlRootElement
public class SedDokument implements SaksopplysningDokument {
    private String rinaSaksnummer;
    private String rinaDokumentID;
    private Landkoder avsenderLandkode;
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

    public Landkoder getAvsenderLandkode() {
        return avsenderLandkode;
    }

    public void setAvsenderLandkode(Landkoder avsenderLandkode) {
        this.avsenderLandkode = avsenderLandkode;
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

    public Lovvalgsperiode opprettInnvilgetLovvalgsperiode() {
        Lovvalgsperiode nyLovvalgsperiode = new Lovvalgsperiode();
        nyLovvalgsperiode.setBestemmelse(getLovvalgBestemmelse());
        nyLovvalgsperiode.setFom(getLovvalgsperiode().getFom());
        nyLovvalgsperiode.setTom(getLovvalgsperiode().getTom());
        nyLovvalgsperiode.setLovvalgsland(getLovvalgslandKode() != null ? Land_iso2.valueOf(getLovvalgslandKode().getKode()) : null);
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

    public boolean erUnntaksperiode() {
        return !Landkoder.NO.equals(lovvalgslandKode);
    }

    public boolean erMedlemskapsperiode() {
        return Landkoder.NO.equals(lovvalgslandKode);
    }
}
