package no.nav.melosys.integrasjon.eessi.dto;

import java.util.Collection;

import no.nav.melosys.domain.eessi.Vedlegg;
import no.nav.melosys.domain.eessi.sed.SedDataDto;

public class OpprettBucOgSedDto {

    private final SedDataDto sedDataDto;
    private final Collection<Vedlegg> vedlegg;

    public OpprettBucOgSedDto(SedDataDto sedDataDto, Collection<Vedlegg> vedlegg) {
        this.sedDataDto = sedDataDto;
        this.vedlegg = vedlegg;
    }

    public SedDataDto getSedDataDto() {
        return sedDataDto;
    }

    public Collection<Vedlegg> getVedlegg() {
        return vedlegg;
    }
}
