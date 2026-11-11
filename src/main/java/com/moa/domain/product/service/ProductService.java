package com.moa.domain.product.service;

import com.moa.common.exception.BusinessException;
import com.moa.domain.chat.entity.ChatRoom;
import com.moa.domain.chat.repository.ChatRoomRepository;
import com.moa.domain.product.dto.ProductCreateRequest;
import com.moa.domain.product.dto.ProductCreateResponse;
import com.moa.domain.product.entity.Product;
import com.moa.domain.product.repository.ProductRepository;
import com.moa.domain.user.entity.User;
import com.moa.domain.user.exception.UserErrorCode;
import com.moa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용, 저장하는 메서드만 따로 열어줌
public class ProductService {

    private final ProductRepository productRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    // 상품 등록 + 채팅방 생성
    // 한 트랜잭션이라 채팅방 저장이 실패하면 상품 저장도 롤백된다
    @Transactional
    public ProductCreateResponse createProduct(Long sellerId, ProductCreateRequest request) {
        // 1. 판매자 조회 (토큰은 유효하지만 회원이 없는 경우 404)
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 2. 상품 저장
        Product product = productRepository.save(Product.create(
                seller, request.name(), request.description(), request.listPrice(), request.imageUrl()));

        // 3. 상품 채팅방 저장
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(product));

        return new ProductCreateResponse(product.getId(), chatRoom.getId());
    }
}