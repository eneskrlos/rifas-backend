package com.rifas.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.rifas.dto.request.ComboRequest;
import com.rifas.dto.response.ComboResponse;
import com.rifas.entity.Combo;
import com.rifas.entity.ComboProducto;
import com.rifas.entity.Producto;
import com.rifas.entity.Usuario;
import com.rifas.repository.ComboRepository;
import com.rifas.repository.ProductoRepository;
import com.rifas.repository.UsuarioRepository;
import com.rifas.service.ComboService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j // Para logging, útil para depuración y monitoreo
@Service
@RequiredArgsConstructor
public class ComboServiceImpl implements ComboService {
    
    private final ComboRepository    comboRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository  usuarioRepository;

    @Override
    @Transactional
    public ComboResponse crear(ComboRequest request) {
        if(comboRepository.existsByNombreIgnoreCase(request.getNombre())){
            throw new RuntimeException(
                "Ya existe un combo con el nombre '" + request.getNombre() + "'");   
        }
        Usuario usuarioActual = obtenerUsuarioActual();
        // Construir el combo sin productos aún
        Combo combo = Combo.builder()
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())            
            .creadoPor(usuarioActual)
            .activo(true)
            .comboProductos(new ArrayList<>())
            .build();
        // Agregar cada producto al combo
        for (ComboRequest.ComboItemRequest item: request.getProductos()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                .filter(p -> p.getActivo())
                .orElseThrow(() -> new RuntimeException( "Producto no encontrado con id: " + item.getProductoId()));
            
            if(!producto.getActivo()){
                throw new RuntimeException("El producto con id " + item.getProductoId() + " no está activo");
             
            }

            ComboProducto comboProd = ComboProducto.builder()
                .combo(combo)
                .producto(producto)
                .cantidad(item.getCantidad())
                .build();
            combo.getComboProductos().add(comboProd);
            
        }

        Combo guardado = comboRepository.save(combo);
        return mapToResponse(guardado);
    }

    

    private Usuario obtenerUsuarioActual() {
        String name =  SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailOrTelefono(name,name)
             .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + name));
    }

    @Override
    @Transactional(readOnly = true)
    public ComboResponse obtenerPorId(Integer id) {
        Combo combo = comboRepository.findByIdWithProductos(id)
                .orElseThrow(() -> new RuntimeException(
                        "Combo no encontrado con id: " + id));
        return mapToResponse(combo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComboResponse> listarActivos(Pageable pageable) {
        return comboRepository.findByActivoTrue(pageable)
                .map(this::mapToResponse);
    }

    // ─────────────────────────────────────────────────────────────
    //  ACTUALIZAR
    // ─────────────────────────────────────────────────────────────

    /**
     * Actualizar un combo reemplaza TODOS sus productos.
     *
     * ¿Por qué reemplazar en lugar de hacer merge?
     * Es más simple y predecible. El manager envía la lista
     * completa de productos que quiere en el combo.
     * orphanRemoval = true en la entidad se encarga de borrar
     * los ComboProducto que ya no están en la lista.
     */
    @Override
    @Transactional
    public ComboResponse actualizar(Integer id, ComboRequest request) {
        Combo combo =  comboRepository.findByIdWithProductos(id)
                .orElseThrow(() -> new RuntimeException(
                        "Combo no encontrado con id: " + id));
            // Verificar nombre duplicado solo si cambió
            if (!combo.getNombre().equalsIgnoreCase(request.getNombre()) && 
                comboRepository.existsByNombreIgnoreCase(request.getNombre())){
                throw new RuntimeException(
                    "Ya existe un combo con el nombre '" + request.getNombre() + "'"
                );
            }

            combo.setNombre(request.getNombre());
            combo.setDescripcion(request.getDescripcion());
            // Limpiar productos actuales (orphanRemoval se encarga de borrar en BD)
            combo.getComboProductos().clear();
            // Agregar nuevos productos
            for (ComboRequest.ComboItemRequest item: request.getProductos()) {
                Producto producto = productoRepository.findById(item.getProductoId())
                    .filter(p -> p.getActivo())
                    .orElseThrow(() -> new RuntimeException( "Producto no encontrado con id: " + item.getProductoId()));
                
                if(!producto.getActivo()){
                    throw new RuntimeException(
                        "El producto con id " + item.getProductoId() + " no está activo");
                }

                ComboProducto comboProd = ComboProducto.builder()
                    .combo(combo)
                    .producto(producto)
                    .cantidad(item.getCantidad())
                    .build();
                combo.getComboProductos().add(comboProd);
            }
            Combo comboActualizado = comboRepository.save(combo);
            log.info("Combo actualizado: id={}, nombre='{}'", id, comboActualizado.getNombre());

        return mapToResponse(comboActualizado);
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Combo no encontrado con id: " + id));
        combo.setActivo(false);
        comboRepository.save(combo);
        log.info("Combo desactivado: id={}, nombre='{}'", id, combo.getNombre());
        
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComboResponse> listarDisponiblesParaRifa() {
        return comboRepository.findCombosDisponiblesParaRifa()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ComboResponse mapToResponse(Combo combo) {

        // Calcular valor total: suma de (valorEstimado * cantidad) por producto
        BigDecimal valorTotal = combo.getComboProductos().stream()
                .map(cp -> cp.getProducto().getValorEstimado()
                        .multiply(BigDecimal.valueOf(cp.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mapear cada producto del combo
        List<ComboResponse.ComboItemResponse> items = combo.getComboProductos()
                .stream()
                .map(cp -> ComboResponse.ComboItemResponse.builder()
                        .productoId(cp.getProducto().getId())
                        .productoNombre(cp.getProducto().getNombre())
                        .imagenUrl(cp.getProducto().getImagenUrl())
                        .valorEstimado(cp.getProducto().getValorEstimado())
                        .cantidad(cp.getCantidad())
                        .subtotal(cp.getProducto().getValorEstimado()
                                .multiply(BigDecimal.valueOf(cp.getCantidad())))
                        .build())
                .toList();

        return ComboResponse.builder()
                .id(combo.getId())
                .nombre(combo.getNombre())
                .descripcion(combo.getDescripcion())
                .activo(combo.getActivo())
                .valorTotalEstimado(valorTotal)
                .creadoPorId(combo.getCreadoPor() != null
                        ? combo.getCreadoPor().getId() : null)
                .creadoPorNombre(combo.getCreadoPor() != null
                        ? combo.getCreadoPor().getNombreCompleto() : null)
                .creadoEn(combo.getCreadoEn())
                .productos(items)
                .build();
    }
}
