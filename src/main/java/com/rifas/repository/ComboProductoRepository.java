package com.rifas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rifas.entity.ComboProducto;

/**
 * ComboProductoRepository — Acceso a la tabla pivote 'combo_productos'.
 *
 * Usado para agregar/quitar productos de un combo
 * y verificar si un producto ya está en un combo específico.
 */
@Repository
public interface ComboProductoRepository extends JpaRepository<ComboProducto, Integer> {
    
    Optional<ComboProducto> findByComboIdAndProductoId(
            Integer comboId, Integer productoId);

    boolean existsByComboIdAndProductoId(
            Integer comboId, Integer productoId);

    void deleteByComboIdAndProductoId(
            Integer comboId, Integer productoId);
    
}
