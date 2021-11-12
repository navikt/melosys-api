package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMalMapper {

    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final InnvilgelseFtrlMapper innvilgelseFtrlMapper;
    private final TrygdeavtaleAttestMapper trygdeavtaleAttestMapper;
    private final InnvilgelseUKMapper innvilgelseUKMapper;

    @Autowired
    public DokgenMalMapper(DokgenMapperDatahenter dokgenMapperDatahenter,
                           InnvilgelseFtrlMapper innvilgelseFtrlMapper,
                           TrygdeavtaleAttestMapper trygdeavtaleAttestMapper,
                           InnvilgelseFtrlMapper innvilgelseFtrlMapper,
                           InnvilgelseUKMapper innvilgelseUKMapper) {
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
        this.innvilgelseFtrlMapper = innvilgelseFtrlMapper;
        this.trygdeavtaleAttestMapper = trygdeavtaleAttestMapper;
        this.innvilgelseUKMapper = innvilgelseUKMapper;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling mottattBrevbestilling) {
        //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes
        DokgenBrevbestilling brevbestilling = berikBestillingMedPersondata(mottattBrevbestilling);
        DokgenDto dto = lagDokgenDtoFraBestilling(brevbestilling);
        Mottaker mottaker = dto.getMottaker();

        String poststed = mottaker.poststed();
        if (hasText(mottaker.postnr())) {
            poststed = dokgenMapperDatahenter.hentPoststed(mottaker.postnr());
        }
        String land = (dokgenMapperDatahenter.hentLandnavn(mottaker.land()));

        dto.setMottaker(new Mottaker(mottaker.navn(), mottaker.adresselinjer(), mottaker.postnr(), poststed, land, mottaker.type()));
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
            case ATTEST_NO_UK_1 -> trygdeavtaleAttestMapper.map(((AttestStorbritanniaBrevbestilling) brevbestilling)
                .toBuilder()
                .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId())).build());
            case INNVILGELSE_UK -> innvilgelseUKMapper.map((InnvilgelseBrevbestilling) brevbestilling);
            case GENERELT_FRITEKSTBREV_BRUKER -> Fritekstbrev.av(((FritekstbrevBrevbestilling) brevbestilling).toBuilder()
                    .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.BRUKER)).build(),
                Aktoersroller.BRUKER
            );
            case GENERELT_FRITEKSTBREV_ARBEIDSGIVER -> Fritekstbrev.av(((FritekstbrevBrevbestilling) brevbestilling).toBuilder()
                    .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.ARBEIDSGIVER)).build(),
                Aktoersroller.ARBEIDSGIVER
            );
            default -> throw new FunksjonellException(
                format("ProduserbartDokument %s er ikke støttet av melosys-dokgen",
                    brevbestilling.getProduserbartdokument()));
        };
    }
}
