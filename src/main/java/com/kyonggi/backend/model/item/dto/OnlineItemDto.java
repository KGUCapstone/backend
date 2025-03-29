package com.kyonggi.backend.model.item.dto;

import lombok.Data;

@Data
public class OnlineItemDto {
    private String title;
    private Integer price;
    private String link;
    private String image;
    private String mallName;
    private String brand;
    private String volume;
}