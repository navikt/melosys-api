package no.nav.melosys.service.persondata;

import java.util.Collections;
import java.util.HashSet;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.tps.TpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersondataServiceTest {
    @Mock
    private PDLConsumer pdlConsumer;
    @Mock
    private TpsService tpsService;
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private PersondataService persondataService;

    @BeforeEach
    public void setup() {
        persondataService = new PersondataService(pdlConsumer, tpsService, fakeUnleash);
    }

    @Test
    void hentAktørID_finnes_verifiserAktørId() {
        when(pdlConsumer.hentIdenter(anyString())).thenReturn(lagIdentliste());
        assertThat(persondataService.hentAktørIdForIdent("123")).isEqualTo("11111");
    }

    @Test
    void hentAktørID_finnesIkke_feiler() {
        when(pdlConsumer.hentIdenter("321")).thenReturn(lagTomIdentliste());
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> persondataService.hentAktørIdForIdent("321"))
            .withMessageContaining("Finner ikke aktørID");
    }

    @Test
    void hentFolkeregisterIdent_finnes_verifiserIdent() {
        when(pdlConsumer.hentIdenter(anyString())).thenReturn(lagIdentliste());
        assertThat(persondataService.hentFolkeregisterIdent("123")).isEqualTo("22222");
    }

    @Test
    void hentFolkeregisterIdent_finnesIkke_feiler() {
        when(pdlConsumer.hentIdenter(anyString())).thenReturn(lagTomIdentliste());
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> persondataService.hentFolkeregisterIdent("123"))
            .withMessageContaining("Finner ikke folkeregisterident");
    }

    private Identliste lagIdentliste() {
        var identliste = new Identliste(new HashSet<>());
        identliste.identer().add(new Ident("11111", AKTORID));
        identliste.identer().add(new Ident("22222", FOLKEREGISTERIDENT));
        identliste.identer().add(new Ident("33333", NPID));

        return identliste;
    }

    private Identliste lagTomIdentliste() {
        return new Identliste(Collections.emptySet());
    }
}
