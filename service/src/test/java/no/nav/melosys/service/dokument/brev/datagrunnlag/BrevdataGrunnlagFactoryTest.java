package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.SaksbehandlingDataFactory;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.person.Informasjonsbehov.MED_FAMILIERELASJONER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevdataGrunnlagFactoryTest {
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private PersondataFasade persondataFasade;

    private BrevdataGrunnlagFactory brevdataGrunnlagFactory;

    @BeforeEach
    void setUp() {
        brevdataGrunnlagFactory = new BrevdataGrunnlagFactory(avklartefaktaService, avklarteVirksomheterService, kodeverkService,
                                                              persondataFasade);
    }

    @Test
    void hentPersondata_avklarteMedfølgendeBarnFinnes_henterFamilie() {
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagMedfolgendeFamilie());
        when(persondataFasade.hentPerson(anyString(), eq(MED_FAMILIERELASJONER))).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        DoksysBrevbestilling doksysBrevbestilling = new DoksysBrevbestilling.Builder().medBehandling(SaksbehandlingDataFactory.lagBehandling()).build();

        brevdataGrunnlagFactory.av(doksysBrevbestilling);
        verify(persondataFasade).hentPerson(anyString(), eq(Informasjonsbehov.MED_FAMILIERELASJONER));
    }

    @Test
    void hentPersondata_avklarteMedfølgendeBarnFinnesIkke_henterIkkeFamilie() {
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(ingenMedfolgendeFamilie());
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        DoksysBrevbestilling doksysBrevbestilling = new DoksysBrevbestilling.Builder().medBehandling(SaksbehandlingDataFactory.lagBehandling()).build();

        brevdataGrunnlagFactory.av(doksysBrevbestilling);
        verify(persondataFasade).hentPerson(anyString());
    }

    private AvklarteMedfolgendeFamilie ingenMedfolgendeFamilie() {
        return new AvklarteMedfolgendeFamilie(Collections.emptySet(), Collections.emptySet());
    }

    private AvklarteMedfolgendeFamilie lagMedfolgendeFamilie() {
        return new AvklarteMedfolgendeFamilie(Set.of(new OmfattetFamilie("adfa")), Collections.emptySet());
    }
}
