package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
import static java.util.stream.Collectors.toList;
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
        DokgenBrevbestilling brevbestilling = berikBestillingMedPersondata(mottattBrevbestilling);
        DokgenDto dto = lagDokgenDtoFraBestilling(brevbestilling);

        //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes med mindre myndighet
        Mottaker mottaker = dto.getMottaker();
        if (!Aktoersroller.MYNDIGHET.getKode().equals(mottaker.type())) {
            String poststed = mottaker.poststed();
            if (hasText(mottaker.postnr())) {
                poststed = dokgenMapperDatahenter.hentPoststed(mottaker.postnr());
            }
            String land = (dokgenMapperDatahenter.hentLandnavn(mottaker.land()));

            dto.setMottaker(new Mottaker(mottaker.navn(), mottaker.adresselinjer(), mottaker.postnr(), poststed, land, mottaker.type()));
        }

        return dto;
    }

    private DokgenBrevbestilling berikBestillingMedPersondata(DokgenBrevbestilling mottattBrevbestilling) {
        return mottattBrevbestilling.toBuilder().medPersonDokument(dokgenMapperDatahenter.hentPersondata(mottattBrevbestilling)).build();
    }

    private List<Instant> hentMangelbrevDatoer(String saksnummer) {
        List<Journalpost> dokumenter = dokumentHentingService.hentDokumenter(saksnummer).stream().filter(dokument ->
                dokument.getHoveddokument().getTittel().equals(MELDING_MANGLENDE_OPPLYSNINGER.getBeskrivelse()))
            .collect(toList());

        return dokumenter.stream()
            .map(Journalpost::getForsendelseJournalfoert)
            .filter(Objects::nonNull)
            .sorted(Comparator.naturalOrder())
            .collect(toList());
    }

    private Avslagbrev hentAvslagsbrev(DokgenBrevbestilling brevbestilling) {
        List<Instant> mangelbrevDatoer = hentMangelbrevDatoer(brevbestilling.getBehandling().getFagsak().getSaksnummer());

        return Avslagbrev.av(((AvslagBrevbestilling) brevbestilling).toBuilder().build(),
            mangelbrevDatoer,
            MangelbrevSvarfrist.hentSvarfristForSisteDato(mangelbrevDatoer)
        );
    }

    private DokgenDto lagDokgenDtoFraBestilling(DokgenBrevbestilling brevbestilling) {
        return switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(
                brevbestilling.toBuilder()
                    .medAvsenderLand(dokgenMapperDatahenter.hentLandnavn(brevbestilling.getAvsenderLand()))
                    .build(),
                Saksbehandlingstid.hentDatoBehandlingstid(brevbestilling.getForsendelseMottatt())
            );
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(brevbestilling,
                Saksbehandlingstid.hentDatoBehandlingstid(brevbestilling.getForsendelseMottatt()));
            case MANGELBREV_BRUKER -> MangelbrevBruker.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build(),
                MangelbrevSvarfrist.hentSvarfristForMangelbrev(Instant.now())
            );
            case MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.BRUKER))
                    .build(),
                MangelbrevSvarfrist.hentSvarfristForMangelbrev(Instant.now())
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
            case AVSLAG_MANGLENDE_OPPLYSNINGER -> hentAvslagsbrev(brevbestilling);
            default -> throw new FunksjonellException(
                format("ProduserbartDokument %s er ikke støttet av melosys-dokgen",
                    brevbestilling.getProduserbartdokument()));
        };
    }
}
