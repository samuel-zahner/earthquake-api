package com.rest_api.app.entity;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "earthquake_requests")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarthquakeRequest {

    @Id
    @GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String starttime;
    private String endtime;
    private String minmagnitude;
    private String latitude;
    private String longitude;
    private String maxradiuskm;
    private String orderby;

    private java.sql.Timestamp requestTime;
    private String responseStatus;

    @OneToMany(mappedBy = "request", cascade = jakarta.persistence.CascadeType.ALL, fetch = jakarta.persistence.FetchType.LAZY)
    private List<EarthquakeEvent> earthquakeEvents;
}
