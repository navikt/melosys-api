package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.UtpekingAvvisDto;
import org.apache.commons.lang3.BooleanUtils;

public class SedPdfData {

    private String begrunnelse;
    private Boolean etterspørInformasjon;
    private String nyttLovvalgsland;
    private String fritekst;

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Boolean isEtterspørInformasjon() {
        return etterspørInformasjon;
    }

    public void setEtterspørInformasjon(Boolean etterspørInformasjon) {
        this.etterspørInformasjon = etterspørInformasjon;
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
            nyttLovvalgsland, begrunnelse, BooleanUtils.toBooleanDefaultIfNull(etterspørInformasjon, false)
        ));

        sedDataDto.setYtterligereInformasjon(fritekst);
    }
}
