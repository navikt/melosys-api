package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;

public record BucBestillingDto(BucType bucType,
                               List<String> mottakerInstitusjoner,
                               Collection<VedleggDto> vedlegg) {}
