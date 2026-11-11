package com.moa.domain.chat.repository;

import com.moa.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 상품 상세: 상품의 채팅방 ID 조회
    Optional<ChatRoom> findByProductId(Long productId);
}