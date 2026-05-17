package entity; // 본인의 패키지명으로 확인!

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @Column(name = "related_role")
    private String relatedRole;

    @Column(name = "department_name")
    private String departmentName;

    @Column
    private String role;
}