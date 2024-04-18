package de.ichsagnurweb.coopmap;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.io.Serializable;

@Entity
public class Marker implements Serializable {

	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	private Long id;
	private String name;
	private String mapId;
	private String description;
	private Double lon;
	private Double lat;

	//for JPA
	public Marker() {
	}

	public Marker(String name, Double lon, Double lat) {
		this.lat = lat;
		this.lon = lon;
		this.name = name;
	}

	public Marker(String mapId, String name, Double lon, Double lat) {
		this.mapId = mapId;
		this.lat = lat;
		this.lon = lon;
		this.name = name;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getMapId() {
		return mapId;
	}

	public Long getId() {
		return id;
	}

	public Double getLon() {
		return lon;
	}

	public Double getLat() {
		return lat;
	}

	public java.lang.String getName() {
		return name;
	}

	public java.lang.String getDescription() {
		return description;
	}
}
