package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface BehandlingsresultatRepository extends JpaRepository<Behandlingsresultat, Long> {
    @EntityGraph(attributePaths = {"avklartefakta"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAvklartefaktaById(Long behandlingID);

    @EntityGraph(attributePaths = {"kontrollresultater"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithKontrollresultaterById(Long behandlingID);

    @EntityGraph(attributePaths = {"anmodningsperioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAnmodningsperioderById(Long behandlingID);

    @EntityGraph(attributePaths = {"helseutgiftDekkesPerioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithHelseutgiftDekkesPerioderById(Long behandlingID);

    /**
     * SELECT ... FOR UPDATE: andre transaksjoner som henter samme behandlingsresultat med lås, venter til denne er ferdig.
     * flushMode COMMIT hindrer at spørringen flusher først. Etter en slik flush slettet ikke Hibernate perioder som var lagt
     * til i samme transaksjon, når lovvalgsperioder ble tømt etterpå (LovvalgsperiodeServiceIT).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    Optional<Behandlingsresultat> findForUpdateById(Long behandlingID);

    List<Behandlingsresultat> findAllByFakturaserieReferanse(String fakturaserieReferanse);

    @EntityGraph(attributePaths = {"lovvalgsperioder", "medlemskapsperioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithLovvalgOgMedlemskapsperioderById(Long behandlingID);

    @EntityGraph(attributePaths = {
        "lovvalgsperioder", "lovvalgsperioder.trygdeavgiftsperioder", "lovvalgsperioder.trygdeavgiftsperioder.grunnlagListe",
        "medlemskapsperioder", "medlemskapsperioder.trygdeavgiftsperioder", "medlemskapsperioder.trygdeavgiftsperioder.grunnlagListe",
        "helseutgiftDekkesPerioder", "helseutgiftDekkesPerioder.trygdeavgiftsperioder", "helseutgiftDekkesPerioder.trygdeavgiftsperioder.grunnlagListe"
    }, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithTrygdeavgiftsperioderAndGrunnlagById(Long behandlingID);

    @Query(
        """
            SELECT b
            FROM Behandlingsresultat b
            JOIN b.vedtakMetadata vm
            JOIN b.medlemskapsperioder mp
            WHERE YEAR(mp.fom) <= :year AND YEAR(mp.tom) >= :year
        """
    )
    List<Behandlingsresultat> findAllWithVedtakMetadataAndMedlemskapsperiodeOverlappingYear(int year);

    @Query(
        """
            SELECT b
            FROM Behandlingsresultat b
            JOIN b.behandling behandling
            JOIN behandling.fagsak fagsak
            JOIN fagsak.aktører aktør
            WHERE aktør.aktørId = :aktorId
        """
    )
    List<Behandlingsresultat> findAllByAktorId(String aktorId);

    @Query(
        """
            SELECT a.aar
            FROM Årsavregning a
            WHERE a.id = :behandlingsresultatId
        """
    )
    Optional<Integer> finnÅrsavregningAar(Long behandlingsresultatId);
}
