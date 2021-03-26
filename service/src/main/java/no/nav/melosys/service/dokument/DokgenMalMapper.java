package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
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

    @Autowired
    public DokgenMalMapper(KodeverkService kodeverkService,
                           BehandlingsresultatService behandlingsresultatService,
                           @Qualifier("system") EregFasade eregFasade,
                           @Qualifier("system") PersondataFasade persondataFasade) {
        this.kodeverkService = kodeverkService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
        this.persondataFasade = persondataFasade;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling brevbestilling) throws TekniskException, FunksjonellException {
        DokgenDto dto;
        if (brevbestilling.getOrg() == null) {
            String fnr = brevbestilling.getBehandling().hentPersonDokument().fnr;
            //NOTE Henter opplysninger på nytt for å sikre at korrekt adresse benyttes
            PersonDokument personDokument = (PersonDokument) persondataFasade.hentPerson(fnr, Informasjonsbehov.STANDARD).getDokument();
            brevbestilling.toBuilder().medPersonDokument(personDokument).build();
        }
        switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                dto = SaksbehandlingstidSoknad.av(brevbestilling);
                break;
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                dto = SaksbehandlingstidKlage.av(brevbestilling);
                break;
            case MANGELBREV_BRUKER:
                dto = MangelbrevBruker.av(((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build());
                break;
            case MANGELBREV_ARBEIDSGIVER:
                MangelbrevBrevbestilling bestilling = (MangelbrevBrevbestilling) brevbestilling;
                dto = MangelbrevArbeidsgiver.av(bestilling.toBuilder()
                    .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(hentFullmektigNavn(brevbestilling.getBehandling().getFagsak()))
                    .build());
                break;
            default:
                throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet av melosys-dokgen", brevbestilling.getProduserbartdokument()));
        }

        if (hasText(dto.getPostnr())) {
            dto.setPoststed(hentPoststed(dto.getPostnr()));
        }
        dto.setLand(hentLandnavn(dto.getLand()));
        return dto;
    }

    private String hentPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr, LocalDate.now());
    }

    private Instant hentVedtaksdato(Long behandlingId) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }

    private String hentFullmektigNavn(Fagsak fagsak) throws IkkeFunnetException, IntegrasjonException {
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
        if (representant.isPresent()) {
            return eregFasade.hentOrganisasjonNavn(representant.get().getOrgnr());
        }
        return null;
    }

    private String hentLandnavn(String landkode) {
        String landnavn = "";
        if (hasText(landkode)) {
            landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode, LocalDate.now());
            if (landnavn.equals("UKJENT")) {
                landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODERISO2, landkode, LocalDate.now());
            }
        }
        return landnavn.equals("UKJENT") ? "" : landnavn;
    }
}
