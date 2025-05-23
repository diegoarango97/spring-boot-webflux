package com.autoestudio.udemy.webflux.app.models.services;

import com.autoestudio.udemy.webflux.app.models.documents.Categoria;
import com.autoestudio.udemy.webflux.app.models.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductoService {
	
	public Flux<Producto> findAll();
	
	public Flux<Producto> findAllUpperCaseName();
	
	public Flux<Producto> findAllUpperCaseNameRepeat();


	public Mono<Producto> findById(String id);
	
	public Mono<Producto> save (Producto producto);
	
	public Mono<Boolean> delete(String id);
	
	public Flux<Categoria> findAllCategoria();
	
	public Mono<Categoria> findCategoriaById(String id);
	
	public Mono<Categoria> save (Categoria categoria);

}
