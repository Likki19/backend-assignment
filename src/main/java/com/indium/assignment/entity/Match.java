package com.indium.assignment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;

import java.util.List;
@Entity
@Getter
@Setter
@ToString(exclude = {"teams", "players", "deliveries", "officials", "powerplays"})
@Table(name = "matches")
public class Match implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private int matchNumber;

    private String city;
    private LocalDate dates;
    private String winner;
    private Integer outcomeByWickets;
    private Integer overs;

    // List of teams in the match
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Team> teams;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Player> players;

    // List of deliveries in the match
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Delivery> deliveries;

    // List of powerplays in the match
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<Powerplay> powerplays;

    @JsonIgnoreProperties("match")
    public List<Team> getTeams() {
        return teams;
    }

    @JsonIgnoreProperties("match")
    public List<Player> getPlayers() {
        return players;
    }
}
