package com.autoestudio.udemy.webflux.app;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.autoestudio.udemy.webflux.app.models.dao.ProductoDao;
import com.autoestudio.udemy.webflux.app.models.documents.Producto;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner{
	
	@Autowired
	private ProductoDao productoDao;
	
	private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class); 

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		productoDao.deleteAll().subscribe();
		Flux.just(
				new Producto("Xbox", 500.0),
				new Producto("USB", 3.0),
				new Producto("Audifonos",100.0),
				new Producto("Iphone", 2000.0)
				)
		.flatMap(producto->{ 
			producto.setFechaCreacion(new Date());
			return productoDao.save(producto);
		})
		.subscribe(producto-> log.info("Inser: " + producto.toString()));
		
		
		
	}

}
