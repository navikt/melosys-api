package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DokgenMapperDatahenterTest {

    @Mock
    private EregFasade eregFasade;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private PersondataFasade persondataFasade;

    private DokgenMapperDatahenter dokgenMapperDatahenter;

    @BeforeEach
    void init() {
        dokgenMapperDatahenter = new DokgenMapperDatahenter(behandlingsresultatService, eregFasade, persondataFasade, kodeverkService);
    }

    @Test
    void hentFullmektigNavn_fullmektigPerson_henterSammensattNavnPerson() {
        Fagsak fagsak = new Fagsak();
        Aktoer representant = new Aktoer();
        representant.setPersonIdent(FNR_REPRESENTANT);
        representant.setRolle(Aktoersroller.REPRESENTANT);
        representant.setRepresenterer(Representerer.BRUKER);
        fagsak.setAktører(Set.of(representant, new Aktoer()));

        when(persondataFasade.hentSammensattNavn(FNR_REPRESENTANT)).thenReturn("Etternavn, Fornavn");

        dokgenMapperDatahenter.hentFullmektigNavn(fagsak, Representerer.BRUKER);

        verify(persondataFasade).hentSammensattNavn(FNR_REPRESENTANT);
    }

    @Test
    void hentFullmektigNavn_fullmektigOrg_henterNavnOrganisasjon() {
        Fagsak fagsak = new Fagsak();
        Aktoer representant = new Aktoer();
        representant.setOrgnr(ORGNR_REPRESENTANT);
        representant.setRolle(Aktoersroller.REPRESENTANT);
        representant.setRepresenterer(Representerer.BRUKER);
        fagsak.setAktører(Set.of(representant, new Aktoer()));

        when(eregFasade.hentOrganisasjonNavn(ORGNR_REPRESENTANT)).thenReturn("Orgnavn");

        dokgenMapperDatahenter.hentFullmektigNavn(fagsak, Representerer.BRUKER);

        verify(eregFasade).hentOrganisasjonNavn(ORGNR_REPRESENTANT);
    }

    @Test
    void hentPersondata_mottakerErIkkeVirksomhet_kallerPersondataFasade() {
        dokgenMapperDatahenter.hentPersondata(lagBehandling());

        verify(persondataFasade).hentPerson(any());
    }

    @Test
    void hentPersondata_mottakerErVirksomhet_returnererNull() {
        var behandling = lagBehandling();
        behandling.getFagsak().getAktører().forEach(a -> a.setRolle(Aktoersroller.VIRKSOMHET));
        var response = dokgenMapperDatahenter.hentPersondata(behandling);

        assertThat(response).isNull();
        verify(persondataFasade, never()).hentPerson(any());
    }

    @Test
    void hentPersonMottaker_mottakerAktørID_brukerAktørID() {
        dokgenMapperDatahenter.hentPersonMottaker(lagMottaker(Mottakerroller.BRUKER));

        verify(persondataFasade).hentPerson(FNR_BRUKER);
    }

    @Test
    void hentPersonMottaker_mottakerPersonIdent_brukerPersonIdent() {
        dokgenMapperDatahenter.hentPersonMottaker(lagMottakerRepresentant(Aktoertype.PERSON));

        verify(persondataFasade).hentPerson(FNR_REPRESENTANT);
    }
}
