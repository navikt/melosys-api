package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia.AttestStorbritannia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMalMapper {

    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final InnvilgelseFtrlMapper innvilgelseFtrlMapper;
    private TryggdeavteleAtestMapper tryggdeavteleAtestMapper;

    @Autowired
    public DokgenMalMapper(DokgenMapperDatahenter dokgenMapperDatahenter,
                           InnvilgelseFtrlMapper innvilgelseFtrlMapper,
                           TryggdeavteleAtestMapper tryggdeavteleAtestMapper) {
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
        this.innvilgelseFtrlMapper = innvilgelseFtrlMapper;
        this.tryggdeavteleAtestMapper = tryggdeavteleAtestMapper;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling mottattBrevbestilling) {
        //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes
        DokgenBrevbestilling brevbestilling = berikBestillingMedPersondata(mottattBrevbestilling);
        DokgenDto dto = lagDokgenDtoFraBestilling(brevbestilling);

        if (hasText(dto.getPostnr())) {
            dto.setPoststed(dokgenMapperDatahenter.hentPoststed(dto.getPostnr()));
        }
        dto.setLand(dokgenMapperDatahenter.hentLandnavn(dto.getLand()));
        return dto;
    }

    private DokgenBrevbestilling berikBestillingMedPersondata(DokgenBrevbestilling mottattBrevbestilling) {
        return mottattBrevbestilling.toBuilder().medPersonDokument(dokgenMapperDatahenter.hentPersondata(mottattBrevbestilling)).build();
    }

    private DokgenDto lagDokgenDtoFraBestilling(DokgenBrevbestilling brevbestilling) {
        return switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(
                brevbestilling.toBuilder()
                    .medAvsenderLand(dokgenMapperDatahenter.hentLandnavn(brevbestilling.getAvsenderLand()))
                    .build()
            );
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(brevbestilling);
            case MANGELBREV_BRUKER -> MangelbrevBruker.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build()
            );
            case MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.BRUKER))
                    .build()
            );
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8 -> innvilgelseFtrlMapper.map((InnvilgelseBrevbestilling) brevbestilling);
            case ATTEST_NO_UK_1 -> tryggdeavteleAtestMapper.map((AttestStorbritanniaBrevbestilling) brevbestilling);
//                AttestStorbritannia.av(
//                ((AttestStorbritanniaBrevbestilling) brevbestilling).toBuilder()
//                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
//                    .build()
//            );
            default -> throw new FunksjonellException(
                format("ProduserbartDokument %s er ikke støttet av melosys-dokgen",
                    brevbestilling.getProduserbartdokument()));
        };
    }
}
