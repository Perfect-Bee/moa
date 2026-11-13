package com.moa.domain.cart.service;

import com.moa.common.exception.BusinessException;
import com.moa.domain.cart.dto.*;
import com.moa.domain.cart.entity.Cart;
import com.moa.domain.cart.entity.CartItem;
import com.moa.domain.cart.exception.CartErrorCode;
import com.moa.domain.cart.repository.CartItemRepository;
import com.moa.domain.cart.repository.CartRepository;
import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final GroupBuyRepository groupBuyRepository;

    // 장바구니 조회
    public CartResponse getCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        List<CartItem> items = cartItemRepository.findAllByCartIdOrderByIdAsc(cart.getId());
        return CartResponse.of(cart, items, LocalDateTime.now());
    }

    // 장바구니 담기 (이미 담긴 공동구매면 수량 합산)
    // 남은 수량은 1차 검증만, 최종 검증은 주문 시점(Redis Lua + 조건부 UPDATE)
    @Transactional
    public CartItemAddResponse addItem(Long userId, CartItemAddRequest request) {
        Cart cart = getCartByUserId(userId);

        // 1. 공동구매 존재(404) → 참여 가능 상태(409)
        GroupBuy groupBuy = groupBuyRepository.findById(request.groupBuyId())
                .orElseThrow(() -> new BusinessException(GroupBuyErrorCode.GROUP_BUY_NOT_FOUND));
        if (!groupBuy.isOrderable(LocalDateTime.now())) {
            throw new BusinessException(GroupBuyErrorCode.GROUP_BUY_NOT_OPEN);
        }

        // 2. 이미 담긴 항목이면 합산한 수량으로 검사 후 변경 (200)
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndGroupBuyId(cart.getId(), groupBuy.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int mergedQuantity = item.getQuantity() + request.quantity();
            validateRemaining(groupBuy, mergedQuantity);
            item.changeQuantity(mergedQuantity); // 변경 감지로 UPDATE
            return CartItemAddResponse.of(item, false);
        }

        // 3. 새 항목 (201)
        validateRemaining(groupBuy, request.quantity());
        CartItem item = cartItemRepository.save(CartItem.create(cart, groupBuy, request.quantity()));
        return CartItemAddResponse.of(item, true);
    }

    // 장바구니 수량 변경 (요청 값으로 덮어씀, 멱등)
    @Transactional
    public CartItemUpdateResponse updateQuantity(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        CartItem item = getOwnedItem(userId, cartItemId);
        validateRemaining(item.getGroupBuy(), request.quantity());
        item.changeQuantity(request.quantity());
        return CartItemUpdateResponse.from(item);
    }

    // 장바구니 항목 삭제
    @Transactional
    public void deleteItem(Long userId, Long cartItemId) {
        CartItem item = getOwnedItem(userId, cartItemId);
        cartItemRepository.delete(item);
    }

    private Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));
    }

    // 항목 존재(404) → 본인 장바구니 항목인지(403)
    // item.getCart()는 LAZY 프록시라 getId()는 추가 쿼리 없음
    private CartItem getOwnedItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));
        Cart cart = getCartByUserId(userId);
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BusinessException(CartErrorCode.NOT_CART_OWNER);
        }
        return item;
    }

    // 남은 수량(max - current, DB 기준) 이내인지 1차 검사
    private void validateRemaining(GroupBuy groupBuy, int quantity) {
        if (quantity > groupBuy.getRemainingQuantity()) {
            throw new BusinessException(GroupBuyErrorCode.EXCEEDS_REMAINING_QUANTITY);
        }
    }
}