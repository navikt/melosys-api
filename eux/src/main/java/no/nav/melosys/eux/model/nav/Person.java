
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Person {


    private String etternavn;

    private String etternavnvedfoedsel;

    private Foedested foedested;

    private String foedselsdato;

    private String fornavn;

    private String fornavnvedfoedsel;

    private String kjoenn;

    private List<Pin> pin;

    private List<Statsborgerskap> statsborgerskap;

    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }

    public String getEtternavnvedfoedsel() {
        return etternavnvedfoedsel;
    }

    public void setEtternavnvedfoedsel(String etternavnvedfoedsel) {
        this.etternavnvedfoedsel = etternavnvedfoedsel;
    }

    public Foedested getFoedested() {
        return foedested;
    }

    public void setFoedested(Foedested foedested) {
        this.foedested = foedested;
    }

    public String getFoedselsdato() {
        return foedselsdato;
    }

    public void setFoedselsdato(String foedselsdato) {
        this.foedselsdato = foedselsdato;
    }

    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    public String getFornavnvedfoedsel() {
        return fornavnvedfoedsel;
    }

    public void setFornavnvedfoedsel(String fornavnvedfoedsel) {
        this.fornavnvedfoedsel = fornavnvedfoedsel;
    }

    public String getKjoenn() {
        return kjoenn;
    }

    public void setKjoenn(String kjoenn) {
        this.kjoenn = kjoenn;
    }

    public List<Pin> getPin() {
        return pin;
    }

    public void setPin(List<Pin> pin) {
        this.pin = pin;
    }

    public List<Statsborgerskap> getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

}
