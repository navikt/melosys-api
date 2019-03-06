package no.nav.melosys.domain.dokument.sed;

import java.util.Set;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.soeknad.UtenlandskIdent;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

public class SedDokument extends SaksopplysningDokument {

    private String rinaSaksnummer;

    private String rinaDokumentId;

    private String fnr;

    private UtenlandskIdent utenlandskIdent;

    private Periode periode;

    private LovvalgBestemmelse lovvalgBestemmelse;

    private Landkoder lovvalgsland;

    private Set<String> treffRegisterkontroll;

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public void setRinaSaksnummer(String rinaSaksnummer) {
        this.rinaSaksnummer = rinaSaksnummer;
    }

    public String getRinaDokumentId() {
        return rinaDokumentId;
    }

    public void setRinaDokumentId(String rinaDokumentId) {
        this.rinaDokumentId = rinaDokumentId;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public UtenlandskIdent getUtenlandskIdent() {
        return utenlandskIdent;
    }

    public void setUtenlandskIdent(UtenlandskIdent utenlandskIdent) {
        this.utenlandskIdent = utenlandskIdent;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public LovvalgBestemmelse getLovvalgBestemmelse() {
        return lovvalgBestemmelse;
    }

    public void setLovvalgBestemmelse(LovvalgBestemmelse lovvalgBestemmelse) {
        this.lovvalgBestemmelse = lovvalgBestemmelse;
    }

    public Landkoder getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(Landkoder lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public Set<String> getTreffRegisterkontroll() {
        return treffRegisterkontroll;
    }

    public void setTreffRegisterkontroll(Set<String> treffRegisterkontroll) {
        this.treffRegisterkontroll = treffRegisterkontroll;
    }
}
