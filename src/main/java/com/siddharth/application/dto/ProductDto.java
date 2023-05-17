package com.siddharth.application.dto;

import com.siddharth.application.entity.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
    private Long productId;
    private String title;
    private String brand;
    private String category;
    private Long price;
    private Long stock;
    private String discountPercentage;
    private String rating;
    private String description;
    private String image;

    public ProductEntity toProductEntity() {
        return ProductEntity.builder().productId(productId).title(title).brand(brand).category(category).price(price).stock(stock)
                .discountPercentage(discountPercentage).rating(rating).description(description).image(image).build();
    }
}
