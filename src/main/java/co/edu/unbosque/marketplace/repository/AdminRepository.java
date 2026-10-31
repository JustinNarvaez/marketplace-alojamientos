package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
}
