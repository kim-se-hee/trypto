package ksh.tryptobackend.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nickname_sequence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameSequenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nickname_sequence_id")
    private Long id;

    public static NicknameSequenceJpaEntity create() {
        return new NicknameSequenceJpaEntity();
    }
}
