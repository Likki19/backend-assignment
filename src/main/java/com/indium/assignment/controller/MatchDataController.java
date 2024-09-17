
package com.indium.assignment.controller;

import com.indium.assignment.service.MatchDataService;
import com.indium.assignment.entity.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cricket")
public class MatchDataController {

    @Autowired
    private MatchDataService matchDataService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMatchData(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        try {
            String result = matchDataService.uploadAndParse(file);
            if (result.equals("Match already exists")) {
                return ResponseEntity.status(HttpStatus.OK).body("Match already exists in the database");
            } else {
                return ResponseEntity.status(HttpStatus.OK).body("File uploaded and data saved successfully");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());

        }

    }

    @GetMapping("/matches/player/{playerName}")
    public Integer getMatchesByPlayer(@PathVariable String playerName) {
        return matchDataService.getMatchesByPlayerName(playerName);
    }

    @GetMapping("/player/{playerName}/cumulativeScore")
    public Integer getCumulativeScore(@PathVariable String playerName) {
        return matchDataService.getCumulativeScore(playerName);
    }
    /*
    @GetMapping("/matches/scores/dates") //?dates=2008-04-24T00:00:00
    public List<Object[]> getMatchScores(@PathVariable("dates") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dates) {
        return matchDataService.getMatchScores(dates);
    }
    */

    @GetMapping("/matches/scores/{dates}")
    public ResponseEntity<String> getMatchScores(@PathVariable LocalDate dates) {
        List<Object[]> scores = matchDataService.getMatchScores(dates);

        // Convert scores to a String format
        String response = scores.stream()
                .map(score -> "Match ID: " + score[0] + ", Score: " + score[1])
                .collect(Collectors.joining(", ", "Scores for matches on " + dates + ": ", ""));

        return ResponseEntity.ok(response);
    }


    @GetMapping("/topbatsmen")
    public List<Map<String, Object>> getTopBatsmen(Pageable pageable) {
        return matchDataService.getTopBatsmen(pageable);
    }
}

