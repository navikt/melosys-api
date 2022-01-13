package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMalMapper {

    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final InnvilgelseFtrlMapper innvilgelseFtrlMapper;
    private final StorbritanniaMapper storbritanniaMapper;
    private final DokumentHentingService dokumentHentingService;

    @Autowired
    public DokgenMalMapper(DokgenMapperDatahenter dokgenMapperDatahenter,
                           InnvilgelseFtrlMapper innvilgelseFtrlMapper,
                           StorbritanniaMapper storbritanniaMapper,
                           DokumentHentingService dokumentHentingService) {
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
        this.innvilgelseFtrlMapper = innvilgelseFtrlMapper;
        this.storbritanniaMapper = storbritanniaMapper;
        this.dokumentHentingService = dokumentHentingService;
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

    private List<Instant> hentMangelbrevDatoer(String saksnummer) {
        List<Journalpost> dokumenter = dokumentHentingService.hentDokumenter(saksnummer).stream().filter(dokument ->
                dokument.getHoveddokument().getTittel().equals(MELDING_MANGLENDE_OPPLYSNINGER.getBeskrivelse()))
            .collect(Collectors.toList());

        return dokumenter.stream()
            .map(Journalpost::getForsendelseJournalfoert)
            .filter(Objects::nonNull)
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());
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
            case STORBRITANNIA -> storbritanniaMapper.map((InnvilgelseBrevbestilling) brevbestilling.toBuilder()
                .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId())).build());
            case GENERELT_FRITEKSTBREV_BRUKER -> Fritekstbrev.av(((FritekstbrevBrevbestilling) brevbestilling).toBuilder()
                    .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.BRUKER)).build(),
                Aktoersroller.BRUKER
            );
            case GENERELT_FRITEKSTBREV_ARBEIDSGIVER -> Fritekstbrev.av(((FritekstbrevBrevbestilling) brevbestilling).toBuilder()
                    .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.ARBEIDSGIVER)).build(),
                Aktoersroller.ARBEIDSGIVER
            );
            case AVSLAG_MANGLENDE_OPPLYSNINGER -> Avslagbrev.av(((AvslagBrevbestilling) brevbestilling).toBuilder().build(),
                hentMangelbrevDatoer(brevbestilling.getBehandling().getFagsak().getSaksnummer())
            );
            default -> throw new FunksjonellException(
                format("ProduserbartDokument %s er ikke støttet av melosys-dokgen",
                    brevbestilling.getProduserbartdokument()));
        };
    }
}
