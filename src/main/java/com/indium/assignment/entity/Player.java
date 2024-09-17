package com.indium.assignment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;

import java.io.Serializable;
@Entity
@Getter
@Setter
@ToString(exclude = {"match", "team"})
@Table(name = "players")
@JsonIgnoreProperties({"match", "team"})
public class Player implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer playerId;  // Changed to Integer for consistency

    private String playerName;

    @ManyToOne
    @JoinColumn(name = "match_number", referencedColumnName = "matchNumber")
    private Match match;

    private Integer totalRuns;  // Changed to Integer for consistency

    @ManyToOne
    @JoinColumn(name = "team_id")  // This should match the column name in Team entity
    @JsonBackReference
    private Team team;
}

