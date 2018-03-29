package no.nav.melosys.service.oppgave.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.oppgave.dto.KodeverdiDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.dto.SaksType;

public class OppgaverDomainTilOppgaveogSakDtoConverterUtil {

    public static SaksType mappeSaksTypeOgBehandling(Fagsak fagsak) {
        SaksType saksType = new SaksType();
        saksType.setStatus(new KodeverdiDto(fagsak.getStatus().getKode(), fagsak.getStatus().getBeskrivelse()));

        List<Behandling> aktivBehandlinger = fagsak.getBehandlinger().stream().filter(behandling -> !behandling.getStatus().getKode().equals(BehandlingStatus.AVSLUTTET)).collect(Collectors.toList());
        if (aktivBehandlinger.size() > 1) {
            throw new RuntimeException("Finnes mer en to aktiv behandlinger");
        } else {
            saksType.setBehandlingDto(new KodeverdiDto(aktivBehandlinger.get(0).getStatus().getKode(), aktivBehandlinger.get(0).getStatus().getBeskrivelse()));
        }

        saksType.setFagSakType(new KodeverdiDto(fagsak.getType().getKode(), fagsak.getType().getBeskrivelse()));
        return saksType;
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

        List<String> landkoder = new ArrayList();
        Optional<List<Land>> landListe = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknadDokument.arbeidUtland.arbeidsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));

        landListe = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknadDokument.oppholdUtland.oppholdsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));

        return landkoder;
    }
}
