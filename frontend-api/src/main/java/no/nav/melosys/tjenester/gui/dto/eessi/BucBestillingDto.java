package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.Collection;

import no.nav.melosys.domain.eessi.BucType;

import java.util.List;

public class BucBestillingDto {

    private BucType bucType;
    private List<String> mottakerLand;
    private List<String> mottakerIder;
    private Collection<VedleggDto> vedlegg;

    public BucBestillingDto(BucType bucType, List<String> mottakerLand, List<String> mottakerIder, Collection<VedleggDto> vedlegg) {
        this.bucType = bucType;
        this.mottakerLand = mottakerLand;
        this.mottakerIder = mottakerIder;
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

    public List<String> getMottakerIder() {
        return mottakerIder;
    }

    public void setMottakerIder(List<String> mottakerIder) {
        this.mottakerIder = mottakerIder;
    }

    public Collection<VedleggDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(Collection<VedleggDto> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
