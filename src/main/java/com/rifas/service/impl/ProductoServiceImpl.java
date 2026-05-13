package com.rifas.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rifas.dto.request.ProductoRequest;
import com.rifas.dto.response.ProductoResponse;
import com.rifas.entity.Producto;
import com.rifas.entity.Usuario;
import com.rifas.repository.ProductoRepository;
import com.rifas.repository.UsuarioRepository;
import com.rifas.service.ProductoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j // Para logging, útil para depuración y monitoreo
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final UsuarioRepository  usuarioRepository;

    @Override
    @Transactional
    public ProductoResponse actualizarProducto(Integer id, ProductoRequest request) {
        
        Producto producto = buscarPorIdOLanzarError(id);
        // Verificar nombre duplicado solo si cambió
        if (!producto.getNombre().equalsIgnoreCase(request.getNombre()) && 
            productoRepository.existsByNombreIgnoreCase(request.getNombre())){
            throw new RuntimeException(
                "Ya existe un producto con el nombre '" + request.getNombre() + "'");   
        }

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setValorEstimado(request.getValorEstimado());
        Producto productoActualizado = productoRepository.save(producto);
        log.info("Producto actualizado: id={}, nombre='{}'", id, productoActualizado.getNombre());
        
        return mapToResponse(productoActualizado);        
    }

    private Producto buscarPorIdOLanzarError(Integer id) {
        return productoRepository.findById(id)
            .filter(Producto::getActivo)
            .orElseThrow(() -> new RuntimeException(
                "Producto no encontrado con id: " + id
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponse> buscarPorNombre(String nombre, Pageable pageable) {
        return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre, pageable)
            .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProductoResponse crearProducto(ProductoRequest request) {
        
        if(productoRepository.existsByNombreIgnoreCase(request.getNombre())) {
            // Si ya existe un producto con ese nombre, lanzamos una excepción.
            // En una aplicación real, podríamos crear una excepción personalizada como
            // ProductoAlreadyExistsException para manejar esto de forma más elegante.
            throw new RuntimeException(
                "Ya existe un producto con el nombre '" + request.getNombre() + "'");
        }

        Usuario usuarioActual = obtenerUsuarioActual(); // Método para obtener el usuario logueado

        Producto nuevoProducto = Producto.builder()
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .imagenUrl(request.getImagenUrl())
            .valorEstimado(request.getValorEstimado())
            .activo(true) // Nuevo producto siempre activo por defecto
            .creadoPor(usuarioActual)
            .build();
        Producto productoGuardado = productoRepository.save(nuevoProducto);
        log.info("Producto creado: '{}' por usuario: {}", productoGuardado.getNombre(),
                usuarioActual.getUsername());
        return mapToResponse(productoGuardado); // Método para convertir Producto a ProductoResponse
    }

    /**
     * Mapeo de Entidad → DTO de respuesta.
     *
     * ¿Por qué no usamos MapStruct aquí?
     * Para este proyecto lo hacemos manual para que veas
     * exactamente qué campos se exponen. MapStruct automatiza
     * esto pero oculta el mapeo. Una vez que entiendas el patrón,
     * puedes agregar MapStruct para reducir código repetitivo.
     */
    private ProductoResponse mapToResponse(Producto p) {
        return ProductoResponse.builder()
            .id(p.getId())
            .nombre(p.getNombre())
            .descripcion(p.getDescripcion())
            .imagenUrl(p.getImagenUrl())
            .valorEstimado(p.getValorEstimado())
            .activo(p.getActivo())
            .creadoPorId(p.getCreadoPor() != null
                    ? p.getCreadoPor().getId() : null)
            .creadoPorNombre(p.getCreadoPor() != null
                    ? p.getCreadoPor().getNombreCompleto() : null)
            .creadoEn(p.getCreadoEn())
            .build();
    }

    private Usuario obtenerUsuarioActual() {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailOrTelefono(usuario, usuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

     /**
     * Usamos "soft delete": no borramos el registro de la BD,
     * solo lo marcamos como inactivo (activo = false).
     *
     * ¿Por qué no DELETE físico?
     * Porque un producto puede estar referenciado en combos históricos
     * o rifas ya ejecutadas. Borrarlo físicamente rompería la integridad
     * de esos registros. Con soft delete los datos históricos quedan intactos.
     */
    @Override
    @Transactional
    public void desactivarProducto(Integer id) {
        Producto producto = buscarPorIdOLanzarError(id);
        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Producto desactivado: id={}, nombre='{}'", id, producto.getNombre());
    }

    @Override
    @Transactional(readOnly = true) // Solo lectura, mejora rendimiento
    public Page<ProductoResponse> listarActivos(Pageable pageable) {
        return productoRepository.findByActivoTrue(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true) // Solo lectura, mejora rendimiento
    public List<ProductoResponse> listarSinComboActivo() {
        return productoRepository.findProductosSinComboActivo()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true) // Solo lectura, mejora rendimiento
    public ProductoResponse obtenerPorId(Integer id) {
        return mapToResponse(buscarPorIdOLanzarError(id));
    }
    
    
}
