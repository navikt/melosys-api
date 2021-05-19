package no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;

public class AnmodningUnntakDto {
    private String mottakerinstitusjon;
    private String fritekstSed;
    private Collection<VedleggDto> vedlegg = new ArrayList<>();

    public String getMottakerinstitusjon() {
        return mottakerinstitusjon;
    }

    public void setMottakerinstitusjon(String mottakerinstitusjon) {
        this.mottakerinstitusjon = mottakerinstitusjon;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }

    public Collection<VedleggDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(Collection<VedleggDto> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
