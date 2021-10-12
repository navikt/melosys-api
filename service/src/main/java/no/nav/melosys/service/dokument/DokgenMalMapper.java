package no.nav.melosys.service.dokument;

import java.time.Instant;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia.AttestStorbritannia;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMalMapper {

    private final BehandlingsresultatService behandlingsresultatService;
    private final EregFasade eregFasade;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;
    private final InnvilgelseFtrlMapper innvilgelseFtrlMapper;

    @Autowired
    public DokgenMalMapper(BehandlingsresultatService behandlingsresultatService,
                           @Qualifier("system") EregFasade eregFasade, KodeverkService kodeverkService,
                           @Qualifier("system") PersondataFasade persondataFasade, Unleash unleash,
                           InnvilgelseFtrlMapper innvilgelseFtrlMapper) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
        this.innvilgelseFtrlMapper = innvilgelseFtrlMapper;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling mottattBrevbestilling) {
        //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes
        DokgenBrevbestilling brevbestilling = berikBestillingMedPersondata(mottattBrevbestilling);
        DokgenDto dto = lagDokgenDtoFraBestilling(brevbestilling);

        if (hasText(dto.getPostnr())) {
            dto.setPoststed(hentPoststed(dto.getPostnr()));
        }
        dto.setLand(hentLandnavn(dto.getLand()));
        return dto;
    }

    private DokgenBrevbestilling berikBestillingMedPersondata(DokgenBrevbestilling mottattBrevbestilling) {
        return mottattBrevbestilling.toBuilder().medPersonDokument(hentPersondata(mottattBrevbestilling)).build();
    }

    private DokgenDto lagDokgenDtoFraBestilling(DokgenBrevbestilling brevbestilling) {
        return switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(
                brevbestilling.toBuilder()
                    .medAvsenderLand(hentLandnavn(brevbestilling.getAvsenderLand()))
                    .build()
            );
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(brevbestilling);
            case MANGELBREV_BRUKER -> MangelbrevBruker.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build()
            );
            case MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(
                ((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(hentFullmektigNavn(brevbestilling.getBehandling().getFagsak()))
                    .build()
            );
            case ATTEST_NO_UK_1 -> AttestStorbritannia.av(
                ((AttestStorbritanniaBrevbestilling) brevbestilling).toBuilder()
                    .build()
            );
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8 -> innvilgelseFtrlMapper.map((InnvilgelseBrevbestilling) brevbestilling);
            default -> throw new FunksjonellException(
                format("ProduserbartDokument %s er ikke støttet av melosys-dokgen",
                    brevbestilling.getProduserbartdokument()));
        };
    }

    private Persondata hentPersondata(DokgenBrevbestilling brevbestilling) {
        final var behandling = brevbestilling.getBehandling();
        if (unleash.isEnabled("melosys.brev.adresser.pdl")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        String fnr = behandling.hentPersonDokument().hentFolkeregisterident();
        return (Persondata) persondataFasade.hentPersonFraTps(fnr, Informasjonsbehov.STANDARD).getDokument();
    }

    private String hentPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr);
    }

    private Instant hentVedtaksdato(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }

    private String hentFullmektigNavn(Fagsak fagsak) {
        return fagsak.finnRepresentant(Representerer.BRUKER)
            .map(aktoer -> eregFasade.hentOrganisasjonNavn(aktoer.getOrgnr()))
            .orElse(null);
    }

    private String hentLandnavn(String landkode) {
        var landnavn = "";
        if (hasText(landkode)) {
            landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode);
            if (landnavn.equals("UKJENT")) {
                landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, landkode);
            }
        }
        return landnavn.equals("UKJENT") ? "" : landnavn;
    }
}
