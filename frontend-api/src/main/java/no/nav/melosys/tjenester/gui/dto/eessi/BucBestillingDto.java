package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;

public class BucBestillingDto {

    private BucType bucType;
    private List<String> mottakerInstitusjoner;
    private Collection<VedleggDto> vedlegg;

    public BucBestillingDto(BucType bucType, List<String> mottakerInstitusjoner, Collection<VedleggDto> vedlegg) {
        this.bucType = bucType;
        this.mottakerInstitusjoner = mottakerInstitusjoner;
        this.vedlegg = vedlegg;
    }

    public BucType getBucType() {
        return bucType;
    }

    public void setBucType(BucType bucType) {
        this.bucType = bucType;
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
