package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);

    // SCRUM-49: busqueda del administrador por correo, nombre o apellido.
    Page<Usuario> findByEmailContainingIgnoreCaseOrNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
            String email, String nombre, String apellido, Pageable pageable);
}
