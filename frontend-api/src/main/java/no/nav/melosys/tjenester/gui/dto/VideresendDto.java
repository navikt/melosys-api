package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;

public class VideresendDto {
    private String mottakerinstitusjon;
    private String fritekst;
    private String ytterligereInformasjonSed;
    private String a008Formaal;
    private Collection<VedleggDto> vedlegg = new ArrayList<>();

    public String getMottakerinstitusjon() {
        return mottakerinstitusjon;
    }

    public void setMottakerinstitusjon(String mottakerinstitusjon) {
        this.mottakerinstitusjon = mottakerinstitusjon;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    public String getYtterligereInformasjonSed() {
        return ytterligereInformasjonSed;
    }

    public void setYtterligereInformasjonSed(String ytterligereInformasjonSed) {
        this.ytterligereInformasjonSed = ytterligereInformasjonSed;
    }

    public String getA008Formaal() {
        return a008Formaal;
    }

    public void setA008Formaal(String a008Formaal) {
        this.a008Formaal = a008Formaal;
    }

    public Collection<VedleggDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(Collection<VedleggDto> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
