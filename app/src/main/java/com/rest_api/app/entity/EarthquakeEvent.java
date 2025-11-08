package com.rest_api.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "earthquake_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarthquakeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    private String earthquakeGlobalId;

    private Double magnitude;
    private String magType;
    private String place;

    private java.sql.Timestamp time;
    private java.sql.Timestamp updated;

    private Boolean tsunami;
    private String status;
    private String alert;

    private Integer significance;
    private String network;
    private String code;
    private String types;

    private Double longitude;
    private Double latitude;
    private Double depth;

    private String url;
    private String detailUrl;
    private String title;
}
