package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.service.dokument.brev.KopiMottaker;

public class FattTrygdeavtaleEllerFtrlVedtakDto extends FattVedtakDto {
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private List<KopiMottaker> kopiMottakere;

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getEktefelleFritekst() {
        return ektefelleFritekst;
    }

    public String getBarnFritekst() {
        return barnFritekst;
    }

    public List<KopiMottaker> getKopiMottakere() {
        return kopiMottakere;
    }

    public void setInnledningFritekst(String innledningFritekst) {
        this.innledningFritekst = innledningFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public void setEktefelleFritekst(String ektefelleFritekst) {
        this.ektefelleFritekst = ektefelleFritekst;
    }

    public void setBarnFritekst(String barnFritekst) {
        this.barnFritekst = barnFritekst;
    }

    public void setKopiMottakere(List<KopiMottaker> kopiMottakere) {
        this.kopiMottakere = kopiMottakere;
    }
}
