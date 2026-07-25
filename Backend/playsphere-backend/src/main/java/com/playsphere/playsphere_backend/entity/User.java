package com.playsphere.playsphere_backend.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="user")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String name;
	
	 @Column(unique = true, nullable = false)
	 private String email;
	 
	 @Column(nullable = false)
	 private String password;

	 @NotNull(message = "Role is required")
	 @Enumerated(EnumType.STRING)
	 private Role role;
	 
	 @CreatedDate
	 @Column(
	        updatable = false
	    )
	 private LocalDateTime createdAt;


	 @LastModifiedDate
	 private LocalDateTime updatedAt;

}
