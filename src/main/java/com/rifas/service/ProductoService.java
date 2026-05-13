package com.rifas.service;

import com.rifas.dto.request.ProductoRequest;
import com.rifas.dto.response.ProductoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductoService {
    ProductoResponse crearProducto(ProductoRequest request);
    ProductoResponse obtenerPorId(Integer id);
    Page<ProductoResponse> listarActivos(Pageable pageable);
    Page<ProductoResponse> buscarPorNombre(String nombre, Pageable pageable);
    ProductoResponse    actualizarProducto(Integer id, ProductoRequest request);
    void                desactivarProducto(Integer id);
    List<ProductoResponse> listarSinComboActivo();
}
