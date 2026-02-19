package no.nav.melosys.service.dokument.brev;

import io.getunleash.Unleash;
import no.nav.melosys.domain.eessi.A008Formaal;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.UtpekingAvvisDto;
import no.nav.melosys.featuretoggle.ToggleName;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SedPdfData {
    static final Logger log = LoggerFactory.getLogger(SedPdfData.class);

    private String begrunnelseUtenlandskMyndighet;
    private Boolean vilSendeAnmodningOmMerInformasjon;
    private String nyttLovvalgsland;
    private String fritekst;
    private A008Formaal a008Formaal;
    private Boolean erFjernarbeidTWFA;

    public SedPdfData() {
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

    public String getA008Formaal() {
        return a008Formaal != null ? a008Formaal.getVerdi() : null;
    }

    public void setA008Formaal(String a008Formaal) {
        this.a008Formaal = A008Formaal.hentVerdi(a008Formaal);
    }

    public Boolean getErFjernarbeidTWFA() {
        return erFjernarbeidTWFA;
    }

    public void setErFjernarbeidTWFA(Boolean erFjernarbeidTWFA) {
        this.erFjernarbeidTWFA = erFjernarbeidTWFA;
    }

    // Utfyller SedDataDto med fritekst og annen informasjon som ikke lagres strukturert i Melosys
    public void utfyllSedDataDto(Unleash unleash, SedDataDto sedDataDto) {

        //OK å alltid sette denne. Blir ikke brukt med mindre A004-pdf skal produseres
        sedDataDto.setUtpekingAvvis(new UtpekingAvvisDto(
            nyttLovvalgsland, begrunnelseUtenlandskMyndighet, BooleanUtils.toBooleanDefaultIfNull(vilSendeAnmodningOmMerInformasjon, false)
        ));

        sedDataDto.setYtterligereInformasjon(fritekst);
        if (unleash.isEnabled(ToggleName.MELOSYS_CDM_4_4)) {
            sedDataDto.setA008Formaal(a008Formaal);
            sedDataDto.setErFjernarbeidTWFA(BooleanUtils.isTrue(erFjernarbeidTWFA));
        }
    }
}
