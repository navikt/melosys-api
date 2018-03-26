package no.nav.melosys.tjenester.gui.dto.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.FagSakTypeDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.SaksTypeDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.Status;

public class OppgaverAGTilMineSakerDTOConverter {

    public static SaksTypeDto mappeSaksTypeOgBehandling(Fagsak fagsak) {
        SaksTypeDto saksTypeDto = new SaksTypeDto();
        //Set Status
        saksTypeDto.setStatus(new Status(fagsak.getStatus().getKode(), fagsak.getStatus().getBeskrivelse()));
        //Set Behandling
        Optional<BehandlingStatus> behandlingStatus = fagsak.getBehandlinger().stream().filter(behandling -> !behandling.getStatus().equals(BehandlingStatus.AVSLUTTET)).findAny().map(Behandling::getStatus);
        saksTypeDto.setBehandlingDto(new no.nav.melosys.tjenester.gui.dto.oppgave.BehandlingDto(behandlingStatus.get().getKode(), behandlingStatus.get().getBeskrivelse()));
        //Set FagSakType
        saksTypeDto.setFagSakTypeDto(new FagSakTypeDto(fagsak.getType().getKode(), fagsak.getType().getBeskrivelse()));
        return saksTypeDto;
    }

    public static PeriodeDto mappeDato(SoeknadDokument soeknadDokument) {
        Optional<Periode> periodeDtoSource = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsperiode);
        if (periodeDtoSource.isPresent()) {
            return new PeriodeDto(soeknadDokument.arbeidUtland.arbeidsperiode.getFom(), soeknadDokument.arbeidUtland.arbeidsperiode.getTom());
        }
        periodeDtoSource = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsPeriode);
        if (periodeDtoSource.isPresent()) {
            return new PeriodeDto(soeknadDokument.oppholdUtland.oppholdsPeriode.getFom(), soeknadDokument.oppholdUtland.oppholdsPeriode.getTom());
        }
        throw new RuntimeException("Finnes ikke noen Arbeidsperiode ellers oppholdsPeriode");
    }

    public static List<String> mappeLander(SoeknadDokument soeknadDokument) {

        List landkoder = new ArrayList();
        Optional<List<Land>> landListe = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsland);
        if(landListe.isPresent())
            landkoder.addAll(soeknadDokument.arbeidUtland.arbeidsland.stream().filter(land -> land !=null).map(Land::getKode).collect(Collectors.toList()));
        landListe = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsland);
        if(landListe.isPresent())
            landkoder.addAll(soeknadDokument.oppholdUtland.oppholdsland.stream().filter(land -> land !=null).map(Land::getKode).collect(Collectors.toList()));
        return landkoder;
    }
}
