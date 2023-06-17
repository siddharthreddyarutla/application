package com.siddharth.application.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import com.siddharth.application.dto.orderDtos.OrderDetailsDto;
import com.siddharth.application.dto.orderDtos.OrdersDto;
import com.siddharth.application.entity.orderEntities.OrderDetailsEntity;
import com.siddharth.application.entity.orderEntities.OrdersEntity;
import com.siddharth.application.entity.productEntities.ProductEntity;
import com.siddharth.application.entity.productEntities.ProductInfoEntity;
import com.siddharth.application.repository.orderRepositories.OrderDetailsRepository;
import com.siddharth.application.repository.orderRepositories.OrdersRepository;
import com.siddharth.application.repository.productRepositories.ProductInfoRepository;
import com.siddharth.application.repository.productRepositories.ProductRepository;
import com.siddharth.application.repository.userRepositories.UserAddressRepository;
import com.siddharth.application.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.siddharth.application.constants.Constants.DELIVERY_CHARGES;
import static com.siddharth.application.constants.Constants.MINIMUM_DELIVERY_AMOUNT;
import static com.siddharth.application.constants.OrderConstants.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderDetailsRepository orderDetailsRepository;

    @Autowired
    UserAddressRepository userAddressRepository;

    @Autowired
    ProductInfoRepository productInfoRepository;

    @Autowired
    OrdersRepository ordersRepository;


    @Override
    public List<OrdersDto> orderProductItems(Long userId, List<Long> productIdList,List<Long> productQuantityList,
                                             Long shippingAddressId, Long billingAddressId, String paymentMode) {
        if (userId != null && shippingAddressId != null && !productIdList.isEmpty()) {
            List<OrdersEntity> ordersEntityList = new ArrayList<>();
            List<OrdersDto> ordersDtoList = new ArrayList<>();
            for (int id = 0; id < productIdList.size(); id++) {
                Long productId = productIdList.get(id);
                Long quantity = productQuantityList.get(id);

                OrdersEntity ordersEntity = new OrdersEntity();
                ordersEntity.setUserId(userId);
                ordersEntity.setAddressId(shippingAddressId);
                ordersEntity.setProductId(productId);
                ordersEntity.setQuantity(quantity);

                ProductInfoEntity productInfoEntity = productInfoRepository.findByProductId(productId);
                if (ObjectUtils.isNotEmpty(productInfoEntity)) {
                    LocalDate deliveryDate = productInfoEntity.getDeliveryDate();
                    ordersEntity.setDeliveryDate(deliveryDate);
                }
                ordersEntity.setOrderPlacedDate(LocalDate.now());
                ordersEntity.setOrderState(DRAFT);
                if (ObjectUtils.isNotEmpty(ordersEntity)) {
                    ordersEntityList.add(ordersEntity);
                }
            }

            if (!ordersEntityList.isEmpty()) {
                for (OrdersEntity ordersEntity : ordersEntityList) {
                    ordersRepository.save(ordersEntity);
                    Long orderId = ordersEntity.getOrderId();
                    saveOrderDetails(orderId,productIdList,productQuantityList, billingAddressId, shippingAddressId, paymentMode);
                    ordersDtoList.add(ordersEntity.toOrdersDto());
                }
            }
            return ordersDtoList;
        }
        return null;
    }

    private void saveOrderDetails(Long orderId, List<Long> productIdList,List<Long> productQuantityList, Long billingAddressId,
                                  Long shippingAddressId, String paymentMode) {
        if (!productIdList.isEmpty()) {
            OrderDetailsDto orderDetailsDto = new OrderDetailsDto();
            Long totalItems = 0L;
            Long deliveryCharges = 0L;
            Double totalAmount = 0D;
            Double orderAmount = 0D;
            Long taxCharges = 0L;
            for (int id = 0; id < productIdList.size(); id++) {
                Long productId = productIdList.get(id);
                Long quantity = productQuantityList.get(id);

                ProductEntity productEntity = productRepository.findByProductId(productId);
                totalItems = totalItems + 1;
                totalAmount = totalAmount + Double.valueOf(productEntity.getPrice()) * quantity;
            }

            JSONObject jsonObject = new JSONObject();

            jsonObject.put("productIds", productIdList);
            String jsonString = jsonObject.toString();
            orderDetailsDto.setProductIds(jsonString);

            orderDetailsDto.setOrderId(orderId);
            orderDetailsDto.setShippingAddressId(shippingAddressId);
            orderDetailsDto.setBillingAddressId(billingAddressId);
            orderDetailsDto.setPaymentMethod(paymentMode);
            orderDetailsDto.setTotalItems(totalItems);
            if (totalAmount < MINIMUM_DELIVERY_AMOUNT) {
                orderAmount = totalAmount + DELIVERY_CHARGES;
                deliveryCharges = DELIVERY_CHARGES;
            } else {
                orderAmount = totalAmount;
            }
            orderDetailsDto.setDeliveryCharges(deliveryCharges);
            orderDetailsDto.setTaxCharges(taxCharges);
            orderDetailsDto.setTotalAmount(totalAmount);
            orderDetailsDto.setOrderAmount(orderAmount);
            OrderDetailsEntity orderDetailsEntity = orderDetailsDto.toOrderDetailsEntity();
            orderDetailsRepository.save(orderDetailsEntity);
        }
    }

    @Override
    public String removeOrdersAndOrderDetailsOnCancelBeforeOrderIsPlaced(List<Long> orderIds) {
        if (!orderIds.isEmpty()) {
            for (Long orderId : orderIds) {
                List<OrdersEntity> ordersEntityList = ordersRepository.findByOrderId(orderId);
                if (!ordersEntityList.isEmpty()) {
                    for (OrdersEntity ordersEntity : ordersEntityList) {
                        ordersRepository.delete(ordersEntity);
                    }
                } else {
                    return ORDER_DETAILS_NOT_FOUND;
                }
            }
        }
        return DELETED_ORDERS;
    }

    @Override
    public List<OrdersDto> editOrderStateInMyOrders(List<Long> orderIds, String orderState) {
        if (!orderIds.isEmpty()) {
            List<OrdersDto> ordersDtoList = new ArrayList<>();
            for (Long orderId : orderIds) {
                List<OrdersEntity> ordersEntityList = ordersRepository.findByOrderId(orderId);
                if (!ordersEntityList.isEmpty()) {
                    for (OrdersEntity ordersEntity : ordersEntityList) {
                        ordersEntity.setOrderState(orderState);
                        ordersRepository.save(ordersEntity);
                        ordersDtoList.add(ordersEntity.toOrdersDto());
                    }
                }
            }
            return ordersDtoList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<OrdersDto> getMyOrdersByUserId(Long userId) {
        if (userId != null) {
            List<OrdersEntity> ordersEntityList = ordersRepository.findByUserId(userId);
            List<OrdersDto> ordersDtoList = new ArrayList<>();

            for (OrdersEntity ordersEntity : ordersEntityList) {
                if (!ordersEntity.getOrderState().equals(DRAFT)) {
                    ordersDtoList.add(ordersEntity.toOrdersDto());
                }
            }
            return ordersDtoList;
        }
        return null;
    }

    @Override
    public List<OrderDetailsDto> getMyOrderDetailsByOrderId(Long orderId) {
        if (orderId != null) {
            List<OrderDetailsEntity> orderDetailsEntityList = orderDetailsRepository.findByOrderId(orderId);
            List<OrderDetailsDto> orderDetailsDtoList = new ArrayList<>();

            if (!orderDetailsEntityList.isEmpty()) {
                for (OrderDetailsEntity orderDetailsEntity : orderDetailsEntityList) {
                    orderDetailsEntity.getProductIds().toString();
                    orderDetailsDtoList.add(orderDetailsEntity.toOrderDetailsDto());
                }
            }
            return orderDetailsDtoList;
        }
        return new ArrayList<>();
    }
}
