package com.moa.domain.product.entity;

import com.moa.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 판매자가 등록하는 상품
// 공동구매(GroupBuy)는 이 상품을 참조해 회차별로 열리고, 채팅방(ChatRoom)은 상품당 1개
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자, 외부 new 차단
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    // 판매자. 목록·상세 응답에서 쓰지 않아서 LAZY로 두고, 필요할 때만 조회
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 100)
    private String name;

    // 긴 설명이 들어갈 수 있어서 VARCHAR(255) 대신 TEXT
    @Column(columnDefinition = "TEXT")
    private String description;

    // 정가 (공동구매 도매가와 비교해 할인율을 보여주는 기준)
    @Column(nullable = false)
    private int listPrice;

    @Column(length = 500)
    private String imageUrl;

    // INSERT 시점에 Hibernate가 현재 시각을 넣어줌, 이후 수정 불가
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Product(User seller, String name, String description, int listPrice, String imageUrl) {
        this.seller = seller;
        this.name = name;
        this.description = description;
        this.listPrice = listPrice;
        this.imageUrl = imageUrl;
    }

    // 외부에서는 create()로만 생성
    public static Product create(User seller, String name, String description, int listPrice, String imageUrl) {
        return new Product(seller, name, description, listPrice, imageUrl);
    }
}