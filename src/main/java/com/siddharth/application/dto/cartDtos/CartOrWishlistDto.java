package com.siddharth.application.dto.cartDtos;

import com.siddharth.application.entity.cartEntities.CartOrWishlistEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartOrWishlistDto {
    private Long userId;
    private Long productId;
    private Long quantity;
    private String cartState;

    public CartOrWishlistEntity toCartOrWishlistEntity() {
        return CartOrWishlistEntity.builder().userId(userId).productId(productId).quantity(quantity)
                .cartState(cartState).build();
    }
}