package entity; // 본인의 패키지명으로 확인!

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(name = "company_name")
    private String companyName;

    @Column
    private String department;

    @Column
    private String role;
}