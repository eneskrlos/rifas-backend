package com.rifas.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.rifas.entity.Producto;

/**
 * ProductoRepository — Acceso a la tabla 'productos'.
 *
 * Usamos Page<Producto> en lugar de List<Producto> para los
 * listados porque en producción puede haber cientos de productos.
 * Pageable permite al cliente pedir: ?page=0&size=10&sort=nombre
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    
    // Solo productos activos paginados
    Page<Producto> findByActivoTrue(Pageable pageable);

    // Buscar por nombre (contiene, sin distinguir mayúsculas)
    Page<Producto> findByNombreContainingIgnoreCase(String name, Pageable pageable);

    // Buscar por nombre (contiene, sin distinguir mayúsculas)
    Page<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(
            String nombre, Pageable pageable);

    // Productos creados por un usuario específico
    List<Producto> findByCreadoPorIdAndActivoTrue(Integer usuarioId);

    /**
     * Busca productos que NO estén en ningún combo activo.
     * Útil para mostrar al manager qué productos están disponibles
     * para agregar a un nuevo combo.
     */
    @Query("""
        SELECT p FROM Producto p
        WHERE p.activo = true
        AND p.id NOT IN (
            SELECT cp.producto.id FROM ComboProducto cp
            WHERE cp.combo.activo = true
        )
    """)
    List<Producto> findProductosSinComboActivo();

    // Verificar si un nombre ya existe (para evitar duplicados)
    boolean existsByNombreIgnoreCase(String nombre);
}
