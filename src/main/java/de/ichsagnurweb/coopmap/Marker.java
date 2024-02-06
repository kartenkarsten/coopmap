package de.ichsagnurweb.coopmap;

import java.io.Serializable;

public class Marker implements Serializable {

	private Integer id;
	private String name;
	private String description;
	private Double lon;
	private Double lat;

	public Marker() {
	}


	public Marker(String name, Double lon, Double lat) {
		this.lat = lat;
		this.lon = lon;
		this.name = name;
	}

	public void setId(java.lang.Integer id) {
		this.id = id;
	}

	public java.lang.Integer getId() {
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
