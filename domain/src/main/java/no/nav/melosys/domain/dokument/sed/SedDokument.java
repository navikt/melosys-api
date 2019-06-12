package no.nav.melosys.domain.dokument.sed;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.jaxb.LovvalgBestemmelseXmlAdapter;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

@XmlRootElement
public class SedDokument extends SaksopplysningDokument {

    private String rinaSaksnummer;

    private String rinaDokumentID;

    private String fnr;

    private Periode lovvalgsperiode;

    @XmlJavaTypeAdapter(LovvalgBestemmelseXmlAdapter.class)
    private LovvalgBestemmelse lovvalgBestemmelse;

    private Landkoder lovvalgslandKode;

    private boolean erEndring;

    private List<String> statsborgerskapKoder = new ArrayList<>();

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

}
