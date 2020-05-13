package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.UtpekingAvvisDto;
import org.apache.commons.lang3.BooleanUtils;

public class SedPdfData {

    private String begrunnelseUtenlandskMyndighet;
    private Boolean vilSendeAnmodningOmMerInformasjon;
    private String nyttLovvalgsland;
    private String fritekst;

    public SedPdfData() {
    }

    public SedPdfData(String begrunnelseUtenlandskMyndighet, Boolean vilSendeAnmodningOmMerInformasjon, String nyttLovvalgsland, String fritekst) {
        this.begrunnelseUtenlandskMyndighet = begrunnelseUtenlandskMyndighet;
        this.vilSendeAnmodningOmMerInformasjon = vilSendeAnmodningOmMerInformasjon;
        this.nyttLovvalgsland = nyttLovvalgsland;
        this.fritekst = fritekst;
    }

    public String getBegrunnelseUtenlandskMyndighet() {
        return begrunnelseUtenlandskMyndighet;
    }

    public void setBegrunnelseUtenlandskMyndighet(String begrunnelseUtenlandskMyndighet) {
        this.begrunnelseUtenlandskMyndighet = begrunnelseUtenlandskMyndighet;
    }

    public Boolean isVilSendeAnmodningOmMerInformasjon() {
        return vilSendeAnmodningOmMerInformasjon;
    }

    public void setVilSendeAnmodningOmMerInformasjon(Boolean vilSendeAnmodningOmMerInformasjon) {
        this.vilSendeAnmodningOmMerInformasjon = vilSendeAnmodningOmMerInformasjon;
    }

    public String getNyttLovvalgsland() {
        return nyttLovvalgsland;
    }

    public void setNyttLovvalgsland(String nyttLovvalgsland) {
        this.nyttLovvalgsland = nyttLovvalgsland;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    // Utfyller SedDataDto med fritekst og annen informasjon som ikke lagres strukturert i Melosys
    public void utfyllSedDataDto(SedDataDto sedDataDto) {

        //OK å alltid sette denne. Blir ikke brukt med mindre A004-pdf skal produseres
        sedDataDto.setUtpekingAvvis(new UtpekingAvvisDto(
            nyttLovvalgsland, begrunnelseUtenlandskMyndighet, BooleanUtils.toBooleanDefaultIfNull(vilSendeAnmodningOmMerInformasjon, false)
        ));

        sedDataDto.setYtterligereInformasjon(fritekst);
    }
}
