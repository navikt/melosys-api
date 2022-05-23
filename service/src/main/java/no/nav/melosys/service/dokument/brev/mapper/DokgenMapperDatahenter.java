package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentHentingSystemService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMapperDatahenter {

    private final BehandlingsresultatService behandlingsresultatService;
    private final EregFasade eregFasade;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;
    private final DokumentHentingService dokumentHentingService;

    protected DokgenMapperDatahenter(BehandlingsresultatService behandlingsresultatService,
                                     EregFasade eregFasade,
                                     PersondataFasade persondataFasade,
                                     DokumentHentingSystemService dokumentHentingService,
                                     KodeverkService kodeverkService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
        this.dokumentHentingService = dokumentHentingService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    String hentNorskPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr);
    }

    String hentLandnavnFraLandkode(String landkode) {
        var landnavn = "";
        if (hasText(landkode)) {
            landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, landkode);
            if (landnavn.equals("UKJENT")) {
                landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode);
            }
        }
        return landnavn.equals("UKJENT") ? "" : landnavn;
    }

    String hentFullmektigNavn(Fagsak fagsak, Representerer representerer) {
        return fagsak.finnRepresentant(representerer)
            .map(aktoer -> {
                if (StringUtils.hasText(aktoer.getOrgnr())) {
                    return eregFasade.hentOrganisasjonNavn(aktoer.getOrgnr());
                } else if (StringUtils.hasText(aktoer.getPersonIdent())) {
                    return persondataFasade.hentSammensattNavn(aktoer.getPersonIdent());
                }
                return null;
            })
            .orElse(null);
    }

    Instant hentVedtaksdato(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }

    Behandlingsresultat hentBehandlingsresultat(long behandlingId) {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingId);
    }

    Persondata hentPersondata(DokgenBrevbestilling brevbestilling) {
        final var behandling = brevbestilling.getBehandling();
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }

    Persondata hentPersonMottaker(Aktoer mottaker) {
        if (mottaker.erOrganisasjon()) {
            return null;
        }
        if (StringUtils.hasText(mottaker.getPersonIdent())) {
            return persondataFasade.hentPerson(mottaker.getPersonIdent());
        }
        if (StringUtils.hasText(mottaker.getAktørId())) {
            return persondataFasade.hentPerson(mottaker.getAktørId());
        }
        throw new FunksjonellException("PersonMottaker mangler aktørID og personIdent");
    }

    String hentSammensattNavn(String fnr) {
        return persondataFasade.hentSammensattNavn(fnr);
    }

    public List<Instant> hentMangelbrevDatoer(DokgenBrevbestilling brevbestilling) {
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

}
