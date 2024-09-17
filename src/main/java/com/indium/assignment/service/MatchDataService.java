

package com.indium.assignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indium.assignment.entity.*;
import com.indium.assignment.repository.*;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchDataService {

    private static final Logger logger = LoggerFactory.getLogger(MatchDataService.class);
    private static final String TOPIC = "match-logs-topic";

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PowerplayRepository powerplayRepository;

    //private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private boolean matchExists(int matchNumber, LocalDate date) {
        return matchRepository.existsByMatchNumberAndDates(matchNumber, date);
    }
    @CacheEvict(value = {"matchesByPlayer", "cumulativeScoreByPlayer", "matchScoresByDate", "topBatsmen"}, allEntries = true)
    @Transactional
    public String uploadAndParse(MultipartFile file) throws IOException {
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());
        JsonNode infoNode = rootNode.path("info");

        int matchNumber = infoNode.path("event").path("match_number").asInt();
        LocalDate matchDate = LocalDate.parse(infoNode.path("dates").get(0).asText());

        if (matchExists(matchNumber, matchDate)) {
            logger.info("Match already exists: Match Number {} on {}", matchNumber, matchDate);
            return "Match already exists";
        }

        Match match = createMatch(infoNode, matchNumber, matchDate);
        matchRepository.save(match);

        List<Team> teams = parseTeamsData(rootNode, match);
        teamRepository.saveAll(teams);

        List<Player> allPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .collect(Collectors.toList());
        playerRepository.saveAll(allPlayers);

        List<Delivery> deliveries = parseDeliveriesData(rootNode, match);
        deliveryRepository.saveAll(deliveries);

        List<Powerplay> powerplays = parsePowerplaysData(rootNode, match);
        powerplayRepository.saveAll(powerplays);

        logger.info("Successfully parsed and saved match data for Match Number {} on {}", matchNumber, matchDate);
        return "Match data processed successfully";
    }

    private Match createMatch(JsonNode infoNode, int matchNumber, LocalDate matchDate) {
        Match match = new Match();
        match.setMatchNumber(matchNumber);
        match.setDates(matchDate);
        match.setCity(infoNode.path("city").asText());
        JsonNode outcomeNode = infoNode.path("outcome");
        match.setWinner(outcomeNode.path("winner").asText());
        match.setOutcomeByWickets(outcomeNode.path("by").path("wickets").asInt());
        match.setOvers(infoNode.path("overs").asInt());
        return match;
    }
    private List<Team> parseTeamsData(JsonNode rootNode, Match match) {
        logger.debug("Parsing teams and players data");
        List<Team> teams = new ArrayList<>();
        JsonNode playersNode = rootNode.path("info").path("players"); // Correct path for players

        if (playersNode.isObject()) {
            playersNode.fields().forEachRemaining(entry -> {
                String teamName = entry.getKey();
                JsonNode teamPlayers = entry.getValue();

                Team team = new Team();
                team.setTeamName(teamName);
                team.setMatch(match); // Set match reference

                List<Player> players = new ArrayList<>();
                if (teamPlayers.isArray()) {
                    for (JsonNode playerNode : teamPlayers) {
                        Player player = new Player();
                        player.setPlayerName(playerNode.asText());
                        player.setMatch(match); // Set match reference
                        player.setTeam(team);   // Set team reference
                        player.setTotalRuns(0);  // Initialize total runs to 0
                        players.add(player);
                    }
                }
                team.setPlayers(players);
                teams.add(team);
            });
        } else {
            logger.warn("Players node is not an object or is missing");
        }

        return teams;
    }

    private List<Delivery> parseDeliveriesData(JsonNode rootNode, Match match) {
        logger.debug("Parsing deliveries data");
        List<Delivery> deliveries = new ArrayList<>();
        JsonNode inningsNode = rootNode.path("innings");
        logger.debug("Innings node: {}", inningsNode);

        if (inningsNode.isArray()) {
            for (JsonNode inning : inningsNode) {
                JsonNode overs = inning.path("overs");
                for (JsonNode over : overs) {
                    int overNumber = over.path("over").asInt();
                    JsonNode deliveriesNode = over.path("deliveries");
                    for (JsonNode deliveryNode : deliveriesNode) {
                        Delivery delivery = new Delivery();
                        delivery.setOverNumber(overNumber);
                        delivery.setBowler(deliveryNode.path("bowler").asText());

                        // Extract and set batter's name
                        // Extract and set batter's name
                        String batterName = deliveryNode.path("batter").asText();
                        if (batterName == null || batterName.isEmpty()) {
                            logger.warn("Batter name is null or empty, skipping delivery.");
                            continue;  // Skip deliveries with missing or null batter names
                        }
                        JsonNode runsNode = deliveryNode.path("runs");
                        int batterRuns = runsNode.path("batter").asInt();

                        try {
                            Player batter = getOrCreatePlayer(batterName, batterRuns);
                            delivery.setPlayer(batter);  // Set the batter for this delivery
                            delivery.setBatter(batter.getPlayerName());
                            delivery.setRunsBatter(batterRuns);
                            delivery.setRunsExtras(runsNode.path("extras").asInt());
                            delivery.setRunsTotal(runsNode.path("total").asInt());
                        } catch (Exception e) {
                            logger.error("Failed to fetch or create player for batter: " + batterName, e);
                            continue;  // Skip this delivery if batter creation fails
                        }

                        // Extract run details
                        delivery.setRunsBatter(runsNode.path("batter").asInt());
                        delivery.setRunsExtras(runsNode.path("extras").asInt());
                        delivery.setRunsTotal(runsNode.path("total").asInt());

                        // Set the match for the delivery
                        delivery.setMatch(match);
                        deliveries.add(delivery);;
                    }
                }
            }
        } else {
            logger.warn("Innings node is not an array or is missing");
        }

        logger.debug("Deliveries parsed: count = {}", deliveries.size());
        return deliveries;
    }

    private List<Powerplay> parsePowerplaysData(JsonNode rootNode, Match match) {
        logger.debug("Parsing powerplays data");
        List<Powerplay> powerplays = new ArrayList<>();
        JsonNode inningsNode = rootNode.path("innings");
        logger.debug("Innings node: {}", inningsNode);

        if (inningsNode.isArray()) {
            for (JsonNode inning : inningsNode) {
                JsonNode powerplaysNode = inning.path("powerplays");
                if (powerplaysNode.isArray()) {
                    for (JsonNode powerplayNode : powerplaysNode) {
                        Powerplay powerplay = new Powerplay();
                        powerplay.setFromOver(powerplayNode.path("from").asDouble());
                        powerplay.setToOver(powerplayNode.path("to").asDouble());
                        powerplay.setType(powerplayNode.path("type").asText());
                        powerplay.setMatch(match);
                        powerplays.add(powerplay);
                        logger.debug("Powerplay parsed: {} - {}", powerplay.getFromOver(), powerplay.getToOver());
                    }
                }
            }
        } else {
            logger.warn("Innings node is not an array or is missing");
        }

        logger.debug("Powerplays parsed: count = {}", powerplays.size());
        return powerplays;
    }


    private Player getOrCreatePlayer(String playerName, int runs) {
        List<Player> players = playerRepository.findAllByPlayerName(playerName);

        Player player;
        if (players.isEmpty()) {
            // Create a new player if not found
            player = new Player();
            player.setPlayerName(playerName);
            player.setTotalRuns(runs); // Set initial runs
            return playerRepository.save(player);
        } else {
            // If player exists, update the runs
            player = players.get(0);
            player.setTotalRuns(player.getTotalRuns() + runs); // Add runs to total
            return playerRepository.save(player);
        }
    }
    @Cacheable(value = "matchesByPlayer", key = "#playerName")
    public Integer getMatchesByPlayerName(String playerName) {
        logger.info("Fetching matches for player: {}", playerName);
        sendLogToKafka("getMatchesByPlayerName", "playerName", playerName);
        //return matchRepository.findMatchesByPlayerName(playerName);
        Integer matches = matchRepository.countMatchesByPlayerName(playerName);
        return matches;
    }

    // @Transactional
    @Cacheable(value = "cumulativeScoreByPlayer", key = "#playerName", unless = "#result == null")
    public int getCumulativeScore(String playerName) {
        logger.info("Calculating cumulative score for player: {}", playerName);
        sendLogToKafka("getCumulativeScore", "playerName", playerName);
        return matchRepository.getCumulativeScoreByPlayer(playerName);
    }

    //@Transactional
    @Cacheable(value = "matchScoresByDate", key = "#dates" ,unless="#result == null")
    public List<Object[]> getMatchScores(LocalDate dates) {
        sendLogToKafka("getMatchScores", "dates", dates.toString());
        return matchRepository.getMatchScoresByDate(dates);
    }
    @Cacheable(value = "topBatsmen")
    public List<Map<String, Object>> getTopBatsmen(Pageable pageable) {
        sendLogToKafka("getTopBatsmen", "pageable", pageable.toString());
        Page<Object[]> topBatsmenPage = matchRepository.findTopBatsmen(pageable);

        // Process the result to return only the player name and scores
        return topBatsmenPage.getContent().stream()
                .map(row -> Map.of("playerName", row[0], "totalRuns", row[1])) // row[0] is player name, row[1] is total runs
                .collect(Collectors.toList());
    }
    private void sendLogToKafka(String methodName, String paramKey, String paramValue) {
        try {
            Map<String, Object> logMessage = new HashMap<>();
            logMessage.put("method", methodName);
            logMessage.put("timestamp", LocalDateTime.now().toString());
            logMessage.put("params", Map.of(paramKey, paramValue));

            String jsonLog = objectMapper.writeValueAsString(logMessage);

            // Send log to Kafka
            kafkaTemplate.send(new ProducerRecord<>(TOPIC, jsonLog));
            logger.info("Log sent to Kafka: {}", jsonLog);

        } catch (Exception e) {
            logger.error("Failed to send log to Kafka", e);
        }
    }
    }


