package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

final class LovvalgsperiodeTjenesteTest extends JsonSchemaTestParent {
    private static final String LOVVALGSPERIODER_SCHEMA = "lovvalgsperioder-schema.json";
    private static final LocalDate FOM = LocalDate.now();
    private static final LovvalgsperiodeDto FORVENTET = new LovvalgsperiodeDto(new PeriodeDto(FOM, FOM),
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
            Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
            Landkoder.SK,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FULL_DEKNING_EOSFO,
            Medlemskapstyper.FRIVILLIG,
            "10");

    private static final long BEHANDLING_UTEN_TILGANG = 238L;
    private static final long BEHANDLING_MED_TEKNISK_FEIL = 832L;

    @Test
    void hentEksisterendeLovvalgsperiodeGir200OkOgEnForekomst() throws Exception {
        testHentLovvalgsperioder(13L, Collections.singletonList(FORVENTET));
    }

    @Test
    void hentIkkeEksisterendeLovvalgsperiodeGir200OkOgTomJson() throws Exception {
        testHentLovvalgsperioder(Long.MAX_VALUE, Collections.emptyList());
    }

    @Test
    void hentLovvalgsperiodeUtenTilgang() {
        testUnntakIhentLovvalgsperiode(BEHANDLING_UTEN_TILGANG, new SikkerhetsbegrensningException("ignorert"));
    }

    @Test
    void hentLovvalgsperiodeMedTekniskFeil() {
        testUnntakIhentLovvalgsperiode(BEHANDLING_MED_TEKNISK_FEIL, new TekniskException("ignorert"));
    }

    @Test
    void hentOpprinneligLovvalgsperiode_returnererPeriode() {
        LovvalgsperiodeService lovvalgsperiodeService = spy(lagLovvalgsperiodeService());
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        LocalDate fomDato = LocalDate.of(2018, 12, 12);
        LocalDate tomDato = LocalDate.of(2019, 12, 12);
        lovvalgsperiode.setFom(fomDato);
        lovvalgsperiode.setTom(tomDato);
        doReturn(lovvalgsperiode).when(lovvalgsperiodeService).hentOpprinneligLovvalgsperiode(5L);
        LovvalgsperiodeTjeneste instans = new LovvalgsperiodeTjeneste(lovvalgsperiodeService, mock(Aksesskontroll.class));

        PeriodeDto periodeDto = instans.hentOpprinneligLovvalgsperiode(5L).get("opprinneligLovvalgsperiode");

        assertThat(periodeDto.getFom()).isEqualTo(fomDato);
        assertThat(periodeDto.getTom()).isEqualTo(tomDato);
    }

    private void testUnntakIhentLovvalgsperiode(long behandlingsid, Throwable forventetUnntak) {
        Throwable unntak = catchThrowable(() -> testHentLovvalgsperioder(behandlingsid, Collections.emptyList()));
        assertThat(unntak).isInstanceOf(forventetUnntak.getClass());
        if (forventetUnntak.getCause() != null) {
            assertThat(unntak).hasCauseInstanceOf(forventetUnntak.getCause().getClass());
        }
    }

    private void testHentLovvalgsperioder(long behandlingsid, Collection<LovvalgsperiodeDto> forventet) throws Exception {
        LovvalgsperiodeService lovvalgsperiodeService = lagLovvalgsperiodeService();
        Aksesskontroll aksesskontroll = mock(Aksesskontroll.class);
        doThrow(new SikkerhetsbegrensningException("Computer says no"))
                .when(aksesskontroll).autoriser(BEHANDLING_UTEN_TILGANG);
        doThrow(new TekniskException("Det har oppstått en..."))
                .when(aksesskontroll).autoriser(BEHANDLING_MED_TEKNISK_FEIL);
        LovvalgsperiodeTjeneste instans = new LovvalgsperiodeTjeneste(lovvalgsperiodeService, aksesskontroll);
        ResponseEntity<?> resultat = instans.hentLovvalgsperioder(behandlingsid);
        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Collection<LovvalgsperiodeDto> resultatliste = (Collection<LovvalgsperiodeDto>) resultat.getBody();
        assertThat(resultatliste).hasSize(forventet.size());
        validerArray(resultatliste, LOVVALGSPERIODER_SCHEMA);
    }

    @Test
    void lagreEnLovvalgsperiodeGir200OkOgEkko() throws Exception {
        testLagreLovvalgsperioder(42L, Collections.singletonList(FORVENTET));
    }

    private void testLagreLovvalgsperioder(long behandlingsid,
            Collection<LovvalgsperiodeDto> perioder) throws Exception {
        LovvalgsperiodeService lovvalgsperiodeService = lagLovvalgsperiodeService();
        Aksesskontroll aksesskontroll = mock(Aksesskontroll.class);
        LovvalgsperiodeTjeneste instans = new LovvalgsperiodeTjeneste(lovvalgsperiodeService, aksesskontroll);
        validerArray(perioder, LOVVALGSPERIODER_SCHEMA);
        Collection<LovvalgsperiodeDto> resultat = instans.lagreLovvalgsperioder(behandlingsid, perioder);
        assertThat(resultat).hasSize(perioder.size());
        if (!perioder.isEmpty()) {
            assertThat(perioder.iterator().next())
                    .usingRecursiveComparison().isEqualTo(resultat.iterator().next());
        }
        validerArray(resultat, LOVVALGSPERIODER_SCHEMA);
    }

    private static LovvalgsperiodeService lagLovvalgsperiodeService() {
        BehandlingsresultatRepository behandlingsresultatRepo = mock(BehandlingsresultatRepository.class);
        LovvalgsperiodeRepository lovvalgsperiodeRepo = mock(LovvalgsperiodeRepository.class);
        TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository = mock(TidligereMedlemsperiodeRepository.class);
        LovvalgsperiodeService lovvalgsperiodeService = new LovvalgsperiodeService(behandlingsresultatRepo, lovvalgsperiodeRepo, tidligereMedlemsperiodeRepository, mock(BehandlingRepository.class));
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(FORVENTET.periode.getFom());
        lovvalgsperiode.setTom(FORVENTET.periode.getTom());
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(Landkoder.valueOf(FORVENTET.lovvalgsland));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.valueOf(FORVENTET.lovvalgsbestemmelse));
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.valueOf(FORVENTET.tilleggBestemmelse));
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.valueOf(FORVENTET.innvilgelsesResultat));
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.valueOf(FORVENTET.medlemskapstype));
        lovvalgsperiode.setMedlPeriodeID(Long.valueOf(FORVENTET.medlemskapsperiodeID));
        when(behandlingsresultatRepo.findById(42L)).thenReturn(Optional.of(lagBehandlingsresultat()));
        List<Lovvalgsperiode> ingenPerioder = Collections.emptyList();
        List<Lovvalgsperiode> enPeriode = Collections.singletonList(lovvalgsperiode);
        when(lovvalgsperiodeRepo.findByBehandlingsresultatId(13L)).thenReturn(enPeriode);
        mockWithGenericVarargsArray(lovvalgsperiodeRepo, ingenPerioder, enPeriode);
        return lovvalgsperiodeService;
    }

    @SuppressWarnings("unchecked")
    private static void mockWithGenericVarargsArray(LovvalgsperiodeRepository lovvalgsperiodeRepo,
            List<Lovvalgsperiode> empty, List<Lovvalgsperiode> single) {
        when(lovvalgsperiodeRepo.findByBehandlingsresultatId(42L)).thenReturn(empty, single);
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        return new Behandlingsresultat();
    }
}
