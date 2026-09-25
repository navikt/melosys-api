package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FagsakRepository extends CrudRepository<Fagsak, String> {

    List<Fagsak> findAllBySaksnummerIn(Collection<String> saksnumre);

    Optional<Fagsak> findBySaksnummer(String saksnummer);

    Optional<Fagsak> findByGsakSaksnummer(Long gsakSaksnummer);

    @Query("select f from Fagsak f, Aktoer a where a.fagsak = f and a.rolle = :rolle  and a.aktørId = :id")
    List<Fagsak> findByRolleAndAktør(@Param("rolle") Aktoersroller rolle, @Param("id") String aktørID);

    @Query("select f from Fagsak f, Aktoer a where a.fagsak = f and a.rolle = :rolle  and a.orgnr = :id")
    List<Fagsak> findByRolleAndOrgnr(@Param("rolle") Aktoersroller rolle, @Param("id") String orgnr);

    // TODO MELOSYS-8309: Engangsretting. Fjern når sakene er rettet i prod.
    /** Saker fra digital søknad med sakstype {@code sakstype} og en aktiv behandling med tema utenfor {@code gyldigeTemaer}. */
    @Query("select distinct f.saksnummer from Fagsak f join f.behandlinger b " +
        "where f.type = :sakstype and b.status not in :inaktiveStatuser and b.tema not in :gyldigeTemaer " +
        "and exists (select m.skjemaId from SkjemaSakMapping m where m.fagsak = f)")
    List<String> finnDigitalSoknadSaksnumreMedAktivBehandlingUtenforTemaer(@Param("sakstype") Sakstyper sakstype,
                                                                          @Param("inaktiveStatuser") Collection<Behandlingsstatus> inaktiveStatuser,
                                                                          @Param("gyldigeTemaer") Collection<Behandlingstema> gyldigeTemaer);

    @NativeQuery("SELECT saksnummer_seq.nextval FROM dual")
    Long hentNesteSekvensVerdi();
}
