package no.nav.melosys.service.dokument;

import java.time.Instant;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
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

    private final KodeverkService kodeverkService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EregFasade eregFasade;
    private final PersondataFasade persondataFasade;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;

    @Autowired
    public DokgenMalMapper(KodeverkService kodeverkService,
                           BehandlingsresultatService behandlingsresultatService,
                           @Qualifier("system") EregFasade eregFasade,
                           @Qualifier("system") PersondataFasade persondataFasade, MedlemAvFolketrygdenService medlemAvFolketrygdenService) {
        this.kodeverkService = kodeverkService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
        this.persondataFasade = persondataFasade;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling brevbestilling) {
        DokgenDto dto;
        if (brevbestilling.getOrg() == null) {
            String fnr = brevbestilling.getBehandling().hentPersonDokument().hentFolkeregisterIdent();
            //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes
            var persondata = (Persondata) persondataFasade.hentPersonFraTps(fnr, Informasjonsbehov.STANDARD).getDokument();
            brevbestilling.toBuilder().medPersonDokument(persondata).build();
        }
        dto = switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(brevbestilling);
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(brevbestilling);
            case MANGELBREV_BRUKER -> MangelbrevBruker.av(((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                .build());
            case MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                .medFullmektigNavn(hentFullmektigNavn(brevbestilling.getBehandling().getFagsak()))
                .build());
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8 -> InnvilgelseFtrl.av((InnvilgelseBrevbestilling) brevbestilling, hentMedlemAvFolketrygden(brevbestilling.getBehandlingId()));

            default -> throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet av melosys-dokgen", brevbestilling.getProduserbartdokument()));
        };

        if (hasText(dto.getPostnr())) {
            dto.setPoststed(hentPoststed(dto.getPostnr()));
        }
        dto.setLand(hentLandnavn(dto.getLand()));
        return dto;
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
        return fagsak.hentRepresentant(Representerer.BRUKER)
            .map(aktoer -> eregFasade.hentOrganisasjonNavn(aktoer.getOrgnr()))
            .orElse(null);
    }

    private String hentLandnavn(String landkode) {
        var landnavn = "";
        if (hasText(landkode)) {
            landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode);
            if (landnavn.equals("UKJENT")) {
                landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODERISO2, landkode);
            }
        }
        return landnavn.equals("UKJENT") ? "" : landnavn;
    }

    private MedlemAvFolketrygden hentMedlemAvFolketrygden(long behandlingId) {
        return medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingId);
    }
}
