package no.nav.melosys.service.persondata.familie;

import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.Sivilstand;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagInaktivBehandlingSomIkkeResulterIVedtak;
import static no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson;
import static no.nav.melosys.service.persondata.familie.FamiliemedlemObjectFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamiliemedlemServiceTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private PDLConsumer pdlConsumer;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @InjectMocks
    private FamiliemedlemService familiemedlemService;

    @Test
    void hentFamiliemedlemmerFraBehandlingID_inaktivBehandlingMedTpsData() {
        var inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak();
        when(behandlingService.hentBehandling(1L)).thenReturn(inaktivBehandling);
        no.nav.melosys.domain.dokument.person.Sivilstand sivilstand = mock(no.nav.melosys.domain.dokument.person.Sivilstand.class);
        when(sivilstand.getKode()).thenReturn("BLA");
        when(saksopplysningerService.harTpsPersonopplysninger(1L)).thenReturn(true);
        when(saksopplysningerService.hentTpsPersonopplysninger(inaktivBehandling.getId())).thenReturn(lagPersonDokumentMedFamiliemedlemmer(sivilstand));


        Set<Familiemedlem> familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(1L);


        assertThat(familiemedlemmer).extracting(Familiemedlem::navn).extracting(Navn::fornavn).contains("BARN", "NAVN");
        assertThat(familiemedlemmer).extracting(Familiemedlem::familierelasjon).contains(Familierelasjon.BARN, Familierelasjon.RELATERT_VED_SIVILSTAND);
    }

    @Test
    void hentFamiliemedlemmerFraBehandlingID_aktivBehandling() {
        long behandlingID = 1L;
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(lagBehandling());
        when(pdlConsumer.hentFamilierelasjoner(IDENT_HOVEDPERSON)).thenReturn(lagHovedpersonMedBarn());
        when(pdlConsumer.hentBarn(IDENT_BARN)).thenReturn(lagPerson());
        when(pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT)).thenReturn(lagPersonGift());
        when(pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT_HISTORISK)).thenReturn(lagPersonGiftHistorisk());


        Set<Familiemedlem> familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(behandlingID);


        assertThat(familiemedlemmer).extracting(Familiemedlem::familierelasjon).contains(Familierelasjon.BARN, Familierelasjon.RELATERT_VED_SIVILSTAND);
    }


    @Test
    void hentFamiliemedlemmerFraBehandlingID_inaktivBehandling() {
        final var inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak();
        when(behandlingService.hentBehandling(1L)).thenReturn(inaktivBehandling);
        when(saksopplysningerService.harTpsPersonopplysninger(1L)).thenReturn(false);
        when(saksopplysningerService.hentPdlPersonopplysninger(1L)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerMedFamilie());


        Set<Familiemedlem> familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(1L);


        assertThat(familiemedlemmer).extracting(Familiemedlem::familierelasjon).contains(Familierelasjon.BARN, Familierelasjon.RELATERT_VED_SIVILSTAND);
    }

    @Test
    void hentFamiliemedlemmer_dobbeltGiftemålSituasjon_forventerEttGiftemål_ogEttBarn() {

        Person hovedperson = lagHovedperson();
        Person giftPerson = lagPersonGift();
        Person giftHistoriskPerson = lagPersonGiftHistorisk();

        when(pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT)).thenReturn(giftPerson);
        when(pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT_HISTORISK)).thenReturn(giftHistoriskPerson);


        Set<Familiemedlem> familiemedlemmer = familiemedlemService.hentFamiliemedlemmer(hovedperson, IDENT_HOVEDPERSON);


        assertThat(familiemedlemmer)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .matches(Familiemedlem::erRelatertVedSivilstand)
                .extracting(Familiemedlem::navn)
                .matches(navn -> navn.harLiktFornavn(PERSON_GIFT_FORNAVN), "Har likt fornavn");

        verify(pdlConsumer, times(1)).hentEktefelleEllerPartner(IDENT_PERSON_GIFT);
        verify(pdlConsumer, times(1)).hentEktefelleEllerPartner(IDENT_PERSON_GIFT_HISTORISK);
    }

    private PersonDokument lagPersonDokumentMedFamiliemedlemmer(Sivilstand sivilstand) {
        PersonDokument person = new PersonDokument();
        List<no.nav.melosys.domain.dokument.person.Familiemedlem> familiemedlemmer = new ArrayList<>();

        familiemedlemmer.add(lagFamilemedlem("NAVN NAVNSEN", "354652678134", no.nav.melosys.domain.dokument.person.Familierelasjon.EKTE, sivilstand));
        familiemedlemmer.add(lagFamilemedlem("BARN NAVNSEN", "134354652678", no.nav.melosys.domain.dokument.person.Familierelasjon.BARN, null));
        person.setFamiliemedlemmer(familiemedlemmer);
        return person;
    }

    private no.nav.melosys.domain.dokument.person.Familiemedlem lagFamilemedlem(String navn, String fnr, no.nav.melosys.domain.dokument.person.Familierelasjon familierelasjon, Sivilstand sivilstand) {
        no.nav.melosys.domain.dokument.person.Familiemedlem familiemedlem = new no.nav.melosys.domain.dokument.person.Familiemedlem();
        familiemedlem.fnr = fnr;
        familiemedlem.navn = navn;
        familiemedlem.familierelasjon = familierelasjon;
        familiemedlem.fødselsdato = LocalDate.EPOCH;
        familiemedlem.fnrAnnenForelder = familierelasjon == no.nav.melosys.domain.dokument.person.Familierelasjon.BARN ? "fnrAnnen" : null;
        familiemedlem.sivilstand = sivilstand;
        return familiemedlem;
    }
}
