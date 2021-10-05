package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMalMapper {

    private final DokgenMapperUtils dokgenMapperUtils;
    private final InnvilgelseFtrlMapper innvilgelseFtrlMapper;

    @Autowired
    public DokgenMalMapper(DokgenMapperUtils dokgenMapperUtils,
                           InnvilgelseFtrlMapper innvilgelseFtrlMapper) {
        this.dokgenMapperUtils = dokgenMapperUtils;
        this.innvilgelseFtrlMapper = innvilgelseFtrlMapper;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling mottattBrevbestilling) {
        //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes
        DokgenBrevbestilling brevbestilling = berikBestillingMedPersondata(mottattBrevbestilling);
        DokgenDto dto = lagDokgenDtoFraBestilling(brevbestilling);

        if (hasText(dto.getPostnr())) {
            dto.setPoststed(dokgenMapperUtils.hentPoststed(dto.getPostnr()));
        }
        dto.setLand(dokgenMapperUtils.hentLandnavn(dto.getLand()));
        return dto;
    }

    private DokgenBrevbestilling berikBestillingMedPersondata(DokgenBrevbestilling mottattBrevbestilling) {
        return mottattBrevbestilling.toBuilder().medPersonDokument(dokgenMapperUtils.hentPersondata(mottattBrevbestilling)).build();
    }

    private DokgenDto lagDokgenDtoFraBestilling(DokgenBrevbestilling brevbestilling) {
        return switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(
                brevbestilling.toBuilder()
                    .medAvsenderLand(dokgenMapperUtils.hentLandnavn(brevbestilling.getAvsenderLand()))
                    .build()
            );
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(brevbestilling);
            case MANGELBREV_BRUKER -> MangelbrevBruker.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperUtils.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build()
            );
            case MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperUtils.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(dokgenMapperUtils.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.BRUKER))
                    .build()
            );
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8 -> innvilgelseFtrlMapper.map((InnvilgelseBrevbestilling) brevbestilling);
            default -> throw new FunksjonellException(
                format("ProduserbartDokument %s er ikke støttet av melosys-dokgen",
                    brevbestilling.getProduserbartdokument()));
        };
    }
}
