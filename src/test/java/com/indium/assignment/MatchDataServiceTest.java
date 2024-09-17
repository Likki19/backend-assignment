
package com.indium.assignment;

import com.indium.assignment.entity.Match;
import com.indium.assignment.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indium.assignment.service.MatchDataService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.test.web.servlet.MockMvc;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MatchDataServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PowerplayRepository powerplayRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;
    /*
    @Test
    void testUploadAndParse_SuccessfulUpload() throws IOException {
        // Arrange
        String jsonContent = "{\"info\":{\"event\":{\"match_number\":1},\"dates\":[\"2023-05-01\"],\"city\":\"TestCity\",\"outcome\":{\"winner\":\"Team A\",\"by\":{\"wickets\":5}},\"overs\":20},\"innings\":[]}";
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", jsonContent.getBytes());

        // Create a real JsonNode structure instead of mocking
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode rootNode = realMapper.readTree(jsonContent);

        // Mock the objectMapper to return our real JsonNode
        when(objectMapper.readTree(any(byte[].class))).thenReturn(rootNode);

        // Mock repository methods
        when(matchRepository.existsByMatchNumberAndDates(anyInt(), any(LocalDate.class))).thenReturn(false);
        when(matchRepository.save(any(Match.class))).thenReturn(new Match());
        when(teamRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(playerRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(deliveryRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(powerplayRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

        // Act
        String result = matchDataService.uploadAndParse(file);

        // Assert
        assertEquals("Match data processed successfully", result);
        verify(matchRepository).save(any(Match.class));
        verify(teamRepository).saveAll(anyList());
        verify(playerRepository).saveAll(anyList());
        verify(deliveryRepository).saveAll(anyList());
        verify(powerplayRepository).saveAll(anyList());
    }

     */

    @Autowired
    private MockMvc mockMvc;

    @InjectMocks
    private MatchDataService matchDataService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /*
    @Test
    void testUploadAndParse_MatchAlreadyExists() throws IOException {
        // Arrange
        String jsonContent = "{\"info\":{\"event\":{\"match_number\":1},\"dates\":[\"2023-05-01\"]}}";
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", jsonContent.getBytes());

        JsonNode rootNode = mock(JsonNode.class);
        JsonNode infoNode = mock(JsonNode.class);
        JsonNode eventNode = mock(JsonNode.class);
        JsonNode datesNode = mock(JsonNode.class);

        when(objectMapper.readTree(any(byte[].class))).thenReturn(rootNode);
        when(rootNode.path("info")).thenReturn(infoNode);
        when(infoNode.path("event")).thenReturn(eventNode);
        when(eventNode.path("match_number")).thenReturn(mock(JsonNode.class));
        when(eventNode.path("match_number").asInt()).thenReturn(1);
        when(infoNode.path("dates")).thenReturn(datesNode);
        when(datesNode.get(0)).thenReturn(mock(JsonNode.class));
        when(datesNode.get(0).asText()).thenReturn("2023-05-01");

        when(matchRepository.existsByMatchNumberAndDates(anyInt(), any(LocalDate.class))).thenReturn(true);

        // Act
        String result = matchDataService.uploadAndParse(file);

        // Assert
        assertEquals("Match already exists", result);
        verify(matchRepository, never()).save(any(Match.class));
    }
    */

    @Test
    public void testGetMatchesByPlayerName() {
        String playerName = "John Doe";
        Integer matches = 100;
        when(matchRepository.countMatchesByPlayerName(playerName)).thenReturn(matches);

        Integer result = matchDataService.getMatchesByPlayerName(playerName);

        assertEquals(matches, result);
        verify(matchRepository, times(1)).countMatchesByPlayerName(playerName);
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    public void testGetCumulativeScore() {
        String playerName = "John Doe";
        Integer cumulativeScore = 100;
        when(matchRepository.getCumulativeScoreByPlayer(playerName)).thenReturn(cumulativeScore);

        Integer result = matchDataService.getCumulativeScore(playerName);

        assertEquals(cumulativeScore, result);
        verify(matchRepository, times(1)).getCumulativeScoreByPlayer(playerName);
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    public void testGetMatchScores() {
        LocalDate date = LocalDate.now();
        List<Object[]> scores = Collections.singletonList(new Object[]{});
        when(matchRepository.getMatchScoresByDate(date)).thenReturn(scores);

        List<Object[]> result = matchDataService.getMatchScores(date);

        assertEquals(scores, result);
        verify(matchRepository, times(1)).getMatchScoresByDate(date);
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }
    /*
    @Test
    public void testGetTopBatsmen() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> topBatsmen = new PageImpl<>(Collections.singletonList(new Object[]{}));
        when(matchRepository.findTopBatsmen(pageable)).thenReturn(topBatsmen);

        Page<Object[]> result = matchDataService.getTopBatsmen(pageable);

        assertEquals(topBatsmen, result);
        verify(matchRepository, times(1)).findTopBatsmen(pageable);
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

     */
}
