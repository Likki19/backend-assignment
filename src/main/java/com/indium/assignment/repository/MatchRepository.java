package com.indium.assignment.repository;

import com.indium.assignment.entity.Match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Integer> {
 Optional<Match> findByMatchNumber(int matchNumber);

 // @Query("SELECT m FROM Match m JOIN Player p ON m.matchNumber = p.matchNumber WHERE p.playerName = :playerName")
 @Query("SELECT count(distinct m) FROM Match m JOIN m.players p WHERE p.playerName = :playerName")
 Integer countMatchesByPlayerName(@Param("playerName") String playerName);

 @Query("SELECT SUM(d.runsBatter) FROM Delivery d WHERE d.batter = :playerName")
 Integer getCumulativeScoreByPlayer(@Param("playerName") String playerName);


 @Query("SELECT d.match.matchNumber, SUM(d.runsTotal) FROM Delivery d WHERE d.match.dates = :matchDate GROUP BY d.match.matchNumber")
 List<Object[]> getMatchScoresByDate(@Param("matchDate") LocalDate dates);
 // Custom query methods (if any) can be added here

 @Query("SELECT d.batter, SUM(d.runsBatter) as totalRuns FROM Delivery d GROUP BY d.batter ORDER BY totalRuns ASC")
 Page<Object[]> findTopBatsmen(Pageable pageable);

 boolean existsByMatchNumberAndDates(int matchNumber, LocalDate date);
 }

