package com.banca.gestionale_banca.rubrica.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.banca.gestionale_banca.user.model.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contacts_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(nullable = false, length = 34)
    private String iban;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
