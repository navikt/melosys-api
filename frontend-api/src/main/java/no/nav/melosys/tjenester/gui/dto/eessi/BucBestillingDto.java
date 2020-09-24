package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.Collection;

import no.nav.melosys.domain.eessi.BucType;

import java.util.List;

public class BucBestillingDto {

    private BucType bucType;
    private List<String> mottakerLand;
    private List<String> mottakerInstitusjoner;
    private Collection<VedleggDto> vedlegg;

    public BucBestillingDto(BucType bucType, List<String> mottakerLand, List<String> mottakerInstitusjoner, Collection<VedleggDto> vedlegg) {
        this.bucType = bucType;
        this.mottakerLand = mottakerLand;
        this.mottakerInstitusjoner = mottakerInstitusjoner;
        this.vedlegg = vedlegg;
    }

    public BucType getBucType() {
        return bucType;
    }

    public void setBucType(BucType bucType) {
        this.bucType = bucType;
    }

    public List<String> getMottakerLand() {
        return mottakerLand;
    }

    public void setMottakerLand(List<String> mottakerLand) {
        this.mottakerLand = mottakerLand;
    }

    public List<String> getMottakerInstitusjoner() {
        return mottakerInstitusjoner;
    }

    public void setMottakerInstitusjoner(List<String> mottakerInstitusjoner) {
        this.mottakerInstitusjoner = mottakerInstitusjoner;
    }

    public Collection<VedleggDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(Collection<VedleggDto> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
