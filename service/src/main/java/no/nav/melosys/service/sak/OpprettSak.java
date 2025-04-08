package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpprettSak {
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlingRegler saksbehandlingRegler;
    private final FagsakService fagsakService;
    private final EessiService eessiService;

    private final LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;

    public OpprettSak(@Lazy ProsessinstansService prosessinstansService,
                      SaksbehandlingRegler saksbehandlingRegler,
                      FagsakService fagsakService, EessiService eessiService, LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService) {
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.fagsakService = fagsakService;
        this.eessiService = eessiService;
        this.lovligeKombinasjonerSaksbehandlingService = lovligeKombinasjonerSaksbehandlingService;
    }

    @Transactional
    public void opprettNySakOgBehandling(OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getMottaksdato() == null) {
            throw new FunksjonellException("Mottaksdato er påkrevd for å opprette sak uten oppgave/journalpost");
        }
        if (opprettSakDto.getBehandlingsaarsakType() == null) {
            throw new FunksjonellException("Årsak er påkrevd for å opprette behandling");
        }
        if (StringUtils.isNotEmpty(opprettSakDto.getBehandlingsaarsakFritekst()) && opprettSakDto.getBehandlingsaarsakType() != Behandlingsaarsaktyper.FRITEKST) {
            throw new FunksjonellException("Kan ikke lagre fritekst som årsak når årsakstype er " + opprettSakDto.getBehandlingsaarsakType());
        }

        validerOpprettSakDto(opprettSakDto);
        prosessinstansService.opprettNySakOgBehandling(opprettSakDto.tilOpprettSakRequest());
    }

    void validerOpprettSakDto(OpprettSakDto opprettSakDto) {
        var hovedpart = opprettSakDto.getHovedpart();
        var sakstype = opprettSakDto.getSakstype();
        var sakstema = opprettSakDto.getSakstema();
        var behandlingstema = opprettSakDto.getBehandlingstema();
        var behandlingstype = opprettSakDto.getBehandlingstype();

        lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
            hovedpart, sakstype, sakstema, behandlingstema, behandlingstype);

        if ((sakstype == Sakstyper.EU_EOS)
            && !saksbehandlingRegler.harIngenFlyt(sakstype, sakstema, behandlingstype, behandlingstema)
            && !saksbehandlingRegler.harIkkeYrkesaktivFlyt(sakstype, behandlingstema)
            && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(sakstype, sakstema, behandlingstema)
            && behandlingstema != Behandlingstema.PENSJONIST
        ) {
            validerSøknadData(opprettSakDto.getSoknadDto());
        }
    }

    private void validerSøknadData(SøknadDto soknadDto) {
        boolean feilet = false;
        StringBuilder feilmeldingBuilder = new StringBuilder();
        if (soknadDto == null) {
            throw new FunksjonellException("SoknadDto må ikke være null for å opprette en søknadbehandling.");
        }
        PeriodeDto periodeDto = soknadDto.periode;
        if (periodeDto.getFom() == null) {
            feilet = true;
            feilmeldingBuilder.append("søknadsperiodes fra og med dato, ");
        }
        if (!soknadDto.land.erGyldig()) {
            feilet = true;
            feilmeldingBuilder.append("land, ");
        }
        if (feilet) {
            throw new FunksjonellException(feilmeldingBuilder.append("mangler for å opprette en søknadbehandling.").toString());
        }
        if (periodeDto.getTom() != null && periodeDto.getFom().isAfter(periodeDto.getTom())) {
            throw new FunksjonellException("Fra og med dato kan ikke være etter til og med dato.");
        }
    }
}
