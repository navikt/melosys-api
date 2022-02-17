package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;
import no.nav.melosys.service.dokument.DokumentHentingService;
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
        // Henter opplysninger på nytt for å sikre at korrekt adresse benyttes (med mindre myndighet)
        DokgenBrevbestilling brevbestilling = berikBestillingMedPersondata(mottattBrevbestilling);
        DokgenDto dto = lagDokgenDtoFraBestilling(brevbestilling);

        Mottaker mottaker = dto.getMottaker();
        if (Aktoersroller.TRYGDEMYNDIGHET.getKode().equals(mottaker.type())) {
            return dto;
        }

        dto.setMottaker(lagMottakerUtenKoder(mottaker));
        return dto;
    }

    private Mottaker lagMottakerUtenKoder(Mottaker mottakerMedKoder) {
        String poststed = mottakerMedKoder.poststed();
        if (Landkoder.NO.getKode().equals(mottakerMedKoder.land()) && hasText(mottakerMedKoder.postnr())) {
            poststed = dokgenMapperDatahenter.hentNorskPoststed(mottakerMedKoder.postnr());
        }
        String land = (dokgenMapperDatahenter.hentLandnavnFraLandkode(mottakerMedKoder.land()));
        return new Mottaker(mottakerMedKoder.navn(), mottakerMedKoder.adresselinjer(), mottakerMedKoder.postnr(), poststed, land, mottakerMedKoder.type());
    }

    private DokgenBrevbestilling berikBestillingMedPersondata(DokgenBrevbestilling mottattBrevbestilling) {
        return mottattBrevbestilling.toBuilder().medPersonDokument(dokgenMapperDatahenter.hentPersondata(mottattBrevbestilling)).build();
    }

    private List<Instant> hentMangelbrevDatoer(DokgenBrevbestilling brevbestilling) {
        String saksnummer = brevbestilling.getBehandling().getFagsak().getSaksnummer();
        Behandling behandling = brevbestilling.getBehandling();

        List<Journalpost> dokumenter = dokumentHentingService.hentDokumenter(saksnummer).stream().filter(dokument ->
                dokument.getHoveddokument().getTittel().equals(MELDING_MANGLENDE_OPPLYSNINGER.getBeskrivelse())
                    && dokument.getForsendelseJournalfoert() != null
                    && dokument.getForsendelseJournalfoert().isAfter(behandling.getRegistrertDato())
                    && dokument.getAvsenderType().equals(Avsendertyper.PERSON)
            ).toList();

        return dokumenter.stream()
            .map(Journalpost::getForsendelseJournalfoert)
            .filter(Objects::nonNull)
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private Avslagbrev hentAvslagsbrev(DokgenBrevbestilling brevbestilling) {
        List<Instant> mangelbrevDatoer = hentMangelbrevDatoer(brevbestilling);

        return Avslagbrev.av(((AvslagBrevbestilling) brevbestilling).toBuilder().build(),
            mangelbrevDatoer,
            mangelbrevDatoer.isEmpty() ? null : MangelbrevSvarfrist.beregnFristFraDato(Collections.max(mangelbrevDatoer))
        );
    }

    private DokgenDto lagDokgenDtoFraBestilling(DokgenBrevbestilling brevbestilling) {
        return switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(
                brevbestilling.toBuilder()
                    .medAvsenderLand(dokgenMapperDatahenter.hentLandnavnFraLandkode(brevbestilling.getAvsenderLand()))
                    .build(),
                Saksbehandlingstid.beregnSaksbehandlingsfrist(brevbestilling.getForsendelseMottatt())
            );
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(brevbestilling,
                Saksbehandlingstid.beregnSaksbehandlingsfrist(brevbestilling.getForsendelseMottatt()));
            case MANGELBREV_BRUKER -> MangelbrevBruker.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build(),
                MangelbrevSvarfrist.beregnFristFraDato(Instant.now())
            );
            case MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling.getBehandling().getFagsak(), Representerer.BRUKER))
                    .build(),
                MangelbrevSvarfrist.beregnFristFraDato(Instant.now())
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
