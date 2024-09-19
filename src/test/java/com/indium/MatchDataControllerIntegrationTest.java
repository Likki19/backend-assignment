package com.indium;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MatchDataControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void testUploadJsonFile() throws IOException {
        String url = "http://localhost:" + port + "/api/cricket/upload";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Load the test file from the classpath
        ClassPathResource fileResource = new ClassPathResource("335997.json");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = testRestTemplate.postForEntity(url, requestEntity, String.class);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Match already exists in the database", response.getBody());
    }

    @Test
    public void testGetMatchesByPlayer() {
        String playerName = "K Goel";
        String url = "http://localhost:" + port + "/api/cricket/matches/player/" + playerName;

        Integer response = testRestTemplate.getForObject(url, Integer.class);
        assertEquals(1, response);  // Expected number of matches played by V Kohli
    }

    @Test
    public void testGetCumulativeScore() {
        String playerName = "K Goel";
        String url = "http://localhost:" + port + "/api/cricket/player/" + playerName + "/cumulativeScore";

        Integer response = testRestTemplate.getForObject(url, Integer.class);
        assertEquals(26, response);  // Expected cumulative score of V Kohli
    }

    @Test
    public void testGetMatchScoresByDate() {
        String date = "2008-04-19";
        String url = "http://localhost:" + port + "/api/cricket/matches/scores/" + date;

        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);
        String expectedResponse = "Scores for matches on 2008-04-19: Match ID: 3, Score: 261";
        // You can adjust the expectedResponse to match the actual response format.

        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    public void testGetTopBatsmen() throws Exception {
        String url = "http://localhost:" + port + "/api/cricket/topbatsmen";

        // Perform the actual request
        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        // Convert response body to JSON array
        JSONArray actualResponseArray = new JSONArray(response.getBody());

        // Define the expected response
        JSONArray expectedResponseArray = new JSONArray(
                "[\n" +
                        "    {\"totalRuns\":164,\"playerName\":\"ML Hayden\"},\n" +
                        "    {\"totalRuns\":138,\"playerName\":\"MS Dhoni\"},\n" +
                        "    {\"totalRuns\":106,\"playerName\":\"V Sehwag\"},\n" +
                        "    {\"totalRuns\":97,\"playerName\":\"SR Watson\"},\n" +
                        "    {\"totalRuns\":81,\"playerName\":\"SK Raina\"},\n" +
                        "    {\"totalRuns\":77,\"playerName\":\"S Dhawan\"},\n" +
                        "    {\"totalRuns\":70,\"playerName\":\"G Gambhir\"},\n" +
                        "    {\"totalRuns\":66,\"playerName\":\"RG Sharma\"},\n" +
                        "    {\"totalRuns\":65,\"playerName\":\"RA Jadeja\"},\n" +
                        "    {\"totalRuns\":57,\"playerName\":\"Yuvraj Singh\"},\n" +
                        "    {\"totalRuns\":56,\"playerName\":\"PA Patel\"},\n" +
                        "    {\"totalRuns\":53,\"playerName\":\"LRPL Taylor\"},\n" +
                        "    {\"totalRuns\":52,\"playerName\":\"MEK Hussey\"},\n" +
                        "    {\"totalRuns\":50,\"playerName\":\"W Jaffer\"},\n" +
                        "    {\"totalRuns\":46,\"playerName\":\"LR Shukla\"},\n" +
                        "    {\"totalRuns\":45,\"playerName\":\"AM Nayar\"},\n" +
                        "    {\"totalRuns\":44,\"playerName\":\"A Symonds\"},\n" +
                        "    {\"totalRuns\":43,\"playerName\":\"RV Uthappa\"},\n" +
                        "    {\"totalRuns\":38,\"playerName\":\"DJ Hussey\"},\n" +
                        "    {\"totalRuns\":37,\"playerName\":\"WP Saha\"}\n" +
                        "]"
        );
        }
    }


