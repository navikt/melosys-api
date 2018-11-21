package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public final class LovvalgsperiodeTjenesteTest {

    private static final LocalDate FOM = LocalDate.now();
    private static final LovvalgsperiodeDto FORVENTET = new LovvalgsperiodeDto(new PeriodeDto(FOM, FOM),
            LovvalgBestemmelse_883_2004.FO_883_2004_ART16_2,
            Landkoder.SK,
            null, null,
            InnvilgelsesResultat.AVSLAATT, null, Medlemskapstype.FRIVILLIG);

    private static final long BEHANDLING_UTEN_TILGANG = 238L;
    private static final long BEHANDLING_MED_TEKNISK_FEIL = 832L;
    private final JsonSchemaTest jsonSchemaTest;

    public LovvalgsperiodeTjenesteTest() {
        jsonSchemaTest = new JsonSchemaTest("lovvalgsperioder-schema.json");
    }

    @Test
    public void hentEksisterendeLovvalgsperiodeGir200OkOgEnForekomst() throws Exception {
        testHentLovvalgsperioder(13L, Collections.singletonList(FORVENTET));
    }

    @Test
    public void hentIkkeEksisterendeLovvalgsperiodeGir200OkOgTomJson() throws Exception {
        testHentLovvalgsperioder(Long.MAX_VALUE, Collections.emptyList());
    }

    @Test
    public void hentLovvalgsperiodeUtenTilgang() throws Exception {
        testUnntakIhentLovvalgsperiode(BEHANDLING_UTEN_TILGANG,
                Collections.emptyList(), new SikkerhetsbegrensningException("ignorert"));
    }

    @Test
    public void hentLovvalgsperiodeMedTekniskFeil() throws Exception {
        testUnntakIhentLovvalgsperiode(BEHANDLING_MED_TEKNISK_FEIL,
                Collections.emptyList(), new TekniskException("ignorert"));
    }

    private void testUnntakIhentLovvalgsperiode(long behandlingsid,
            Collection<LovvalgsperiodeDto> forventet, Throwable forventetUnntak) throws Exception {
        Throwable unntak = catchThrowable(() -> testHentLovvalgsperioder(behandlingsid, Collections.emptyList()));
        assertThat(unntak).isInstanceOf(forventetUnntak.getClass());
        if (forventetUnntak.getCause() != null) {
            assertThat(unntak).hasCauseInstanceOf(forventetUnntak.getCause().getClass());
        }
    }

    private void testHentLovvalgsperioder(long behandlingsid,
            Collection<LovvalgsperiodeDto> forventet) throws Exception {
        LovvalgsperiodeService lovvalgsperiodeService = lagLovvalgsperiodeService();
        Tilgang tilgang = mock(Tilgang.class);
        doThrow(new SikkerhetsbegrensningException("Computer says no"))
                .when(tilgang).sjekk(eq(BEHANDLING_UTEN_TILGANG));
        doThrow(new TekniskException("Det har oppstått en..."))
                .when(tilgang).sjekk(eq(BEHANDLING_MED_TEKNISK_FEIL));
        LovvalgsperiodeTjeneste instans = new LovvalgsperiodeTjeneste(lovvalgsperiodeService, tilgang);
        Response resultat = instans.hentLovvalgsperioder(behandlingsid);
        assertEquals(Response.Status.OK.getStatusCode(), resultat.getStatus());
        @SuppressWarnings("unchecked")
        Collection<LovvalgsperiodeDto> resultatliste = (Collection<LovvalgsperiodeDto>) resultat.getEntity();
        assertThat(resultatliste.size()).isEqualTo(forventet.size());
        jsonSchemaTest.validerListe(resultatliste);
    }

    @Test
    public void lagreEnLovvalgsperiodeGir200OkOgEkko() throws Exception {
        testLagreLovvalgsperioder(42L, Collections.singletonList(FORVENTET));
    }

    private void testLagreLovvalgsperioder(long behandlingsid,
            Collection<LovvalgsperiodeDto> perioder) throws Exception {
        LovvalgsperiodeService lovvalgsperiodeService = lagLovvalgsperiodeService();
        Tilgang tilgang = mock(Tilgang.class);
        LovvalgsperiodeTjeneste instans = new LovvalgsperiodeTjeneste(lovvalgsperiodeService, tilgang);
        jsonSchemaTest.validerListe(perioder);
        Collection<LovvalgsperiodeDto> resultat = instans.lagreLovvalgsperioder(behandlingsid, perioder);
        assertThat(resultat.size()).isEqualTo(perioder.size());
        if (!perioder.isEmpty()) {
            assertThat(perioder.iterator().next())
                    .isEqualToComparingFieldByFieldRecursively(resultat.iterator().next());
        }
        jsonSchemaTest.validerListe(resultat);
    }

    private static LovvalgsperiodeService lagLovvalgsperiodeService() {
        BehandlingsresultatRepository behandlingsresultatRepo = mock(BehandlingsresultatRepository.class);
        LovvalgsperiodeRepository lovvalgsperiodeRepo = mock(LovvalgsperiodeRepository.class);
        LovvalgsperiodeService lovvalgsperiodeService = new LovvalgsperiodeService(behandlingsresultatRepo, lovvalgsperiodeRepo);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(FORVENTET.periode.getFom());
        lovvalgsperiode.setTom(FORVENTET.periode.getTom());
        lovvalgsperiode.setLovvalgsland(Landkoder.valueOf(FORVENTET.lovvalgsland));
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.valueOf(FORVENTET.lovvalgBestemmelse));
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.valueOf(FORVENTET.innvilgelsesResultat));
        lovvalgsperiode.setMedlemskapstype(Medlemskapstype.valueOf(FORVENTET.medlemskapstype));
        when(behandlingsresultatRepo.findOne(eq(42L))).thenReturn(lagBehandlingsresultat());
        List<Lovvalgsperiode> ingenPerioder = Collections.<Lovvalgsperiode> emptyList();
        List<Lovvalgsperiode> enPeriode = Collections.singletonList(lovvalgsperiode);
        when(lovvalgsperiodeRepo.findByBehandlingsresultatId(eq(13L))).thenReturn(enPeriode);
        mockWithGenericVarargsArray(lovvalgsperiodeRepo, ingenPerioder, enPeriode);
        return lovvalgsperiodeService;
    }

    @SuppressWarnings("unchecked")
    private static void mockWithGenericVarargsArray(LovvalgsperiodeRepository lovvalgsperiodeRepo,
            List<Lovvalgsperiode> empty, List<Lovvalgsperiode> single) {
        when(lovvalgsperiodeRepo.findByBehandlingsresultatId(eq(42L))).thenReturn(empty, single);
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat resultat = new Behandlingsresultat();
        return resultat;
    }

}
