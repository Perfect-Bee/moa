package com.moa.domain.chat.entity;

import com.moa.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상품 채팅방. 상품 등록 시 같은 트랜잭션에서 함께 생성
// 공동구매 회차가 끝나도 상품과 채팅방은 유지된다
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true: 상품당 채팅방 1개를 DB 제약으로 보장
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    private ChatRoom(Product product) {
        this.product = product;
    }

    public static ChatRoom create(Product product) {
        return new ChatRoom(product);
    }
}