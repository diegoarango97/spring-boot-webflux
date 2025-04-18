package com.autoestudio.udemy.webflux.app.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

import com.autoestudio.udemy.webflux.app.models.documents.Categoria;
import com.autoestudio.udemy.webflux.app.models.documents.Producto;
import com.autoestudio.udemy.webflux.app.models.services.ProductoService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@SessionAttributes("producto")
@Controller
public class ProductoController {

	
	@Autowired
	private ProductoService productoService;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

	@Value("${configuracion.cargarimagenes.ruta}")
	private String rutaCargaArchivos;
	
	@ModelAttribute("categorias")
	public Flux<Categoria> categorias (){
		return productoService.findAllCategoria();
	}
	
	
	@GetMapping("/ver-foto/img/{nombreFoto:.+}")
	public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException{
		Path ruta = Path.of(rutaCargaArchivos).resolve(nombreFoto).toAbsolutePath();
		Resource imagen = new UrlResource(ruta.toUri());
		
		return Mono.just(
				ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, 
						"attachment; filename=\""+ imagen.getFilename() + "\"")
				.body(imagen)
				);
	}
	
	
	@GetMapping("/ver/{id}")
	public Mono<String> verDetalle(Model model, @PathVariable String id) {

		return productoService.findById(id).doOnNext(producto -> {
			model.addAttribute("producto", producto);
			model.addAttribute("titulo", "Detalle Producto");
		}).switchIfEmpty(Mono.just(new Producto())).flatMap(producto -> {
			if (producto.getId() == null) {
				return Mono.error(new InterruptedException("No existe el producto"));
			}
			return Mono.just(producto);
		}).then(Mono.just("ver"))
		.onErrorResume(error-> {
			 return Mono.just("redirect:/listar?error=No+existe+el+producto+a+eliminar.");
		});
	}
	

	@GetMapping({"/listar","/"})
	public Mono<String> listar(Model model) {
		 Flux<Producto> productos = productoService.findAllUpperCaseName();
		 
		 productos.subscribe(producto -> log.info(producto.toString()));
		 
		 model.addAttribute("productos", productos);
		 model.addAttribute("titulo", "Listado de productos");
		return Mono.just("listar");
	}
	
	
	@GetMapping("/form")
	public Mono<String> crear(Model model){
		
		 model.addAttribute("producto", new Producto());
		 model.addAttribute("titulo", "Nuevo producto");
		 model.addAttribute("boton", "Crear");


		return Mono.just("form");
	}

	/**
	 * Con esta segunda forma, hay un inconveniente, al asignar los valores del model en el doOnNext esto se procesa en otro hilo, 
	 * por lo tanto no se guarda en la sesion, es decir que no sirve la anotacion del controlador @SessionAttributes,
	 * para poder visualizar el id del producto en el formulario debe ser por medio de un campo hidden.
	 * 
	 * 
	 * parece que esto ya lo resolvieron y ya si permite guardar los datos en la sesion
	 * */
	@GetMapping("/form-forma2/{id}")
	public Mono<String> editarForma2(@PathVariable String id, Model model){
		 return productoService.findById(id)
				 .doOnNext(producto-> {
					 log.info(producto.toString());
					 model.addAttribute("titulo", "Editar producto producto forma 2");
					 model.addAttribute("producto", producto);
					 model.addAttribute("boton", "editar");
				 })
				 .defaultIfEmpty(new Producto())
				 .flatMap(producto -> {
					 if (producto.getId() == null) {
						 return Mono.error(new InterruptedException("No existe el producto"));
					 }
					 return Mono.just(producto);
				 })
				 .then(Mono.just("form"))
				 .onErrorResume( error -> Mono.just("redirect:/listar?error=no+exite+el+producto"));
	
		
	}
	
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model){
		 Mono<Producto> productoMono = productoService.findById(id)
				 .doOnNext(producto-> log.info(producto.toString()))
				 .defaultIfEmpty(new Producto());
		 model.addAttribute("titulo", "Editar");
		 model.addAttribute("producto", productoMono);
		 model.addAttribute("boton", "editar");
		return Mono.just("form");
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(@Valid Producto producto, BindingResult bindingResult, Model model,
			@RequestPart FilePart file, SessionStatus session) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("titulo", "Error en el formulario de producto");
			model.addAttribute("boton", "Guardar");
			return Mono.just("form");
		}

		session.setComplete();

		Mono<Categoria> categoriaMono = productoService.findCategoriaById(producto.getCategoria().getId());

		return categoriaMono.flatMap(categoria -> {
			if (!file.filename().isEmpty()) {

				producto.setFoto(
						UUID.randomUUID() + "-" + file.filename().replace(" ", "").replace(":", "").replace("\\", ""));
			}
			producto.setCategoria(categoria);
			return productoService.save(producto);
		}).doOnNext(productoGuardado -> {
			log.info("Categoria guardada: " + producto.getCategoria().toString());
			log.info("Producto guardado: " + producto.toString());
		}).flatMap(productoGuardado-> {
			if(!file.filename().isEmpty()) {
				return file.transferTo(new File ( rutaCargaArchivos + productoGuardado.getFoto()));
			}
			return Mono.empty();
		})
				
				.thenReturn("redirect:/listar?success=Producto+guardado+con+éxito.");
	}

	@GetMapping("/eliminar/{id}")
	public Mono<String> eliminar(@PathVariable String id){
		 return productoService.delete(id)
				 .flatMap(result ->{
					 if (result.booleanValue()) {
						 return Mono.just("redirect:/listar?success=Producto+eliminado+con+éxito.");
					 }
					 return Mono.just("redirect:/listar?error=No+existe+el+producto+a+eliminar.");
				 });
				 
	}
	
	@GetMapping({"/listar-full"})
	public String listarFull(Model model) {
		 Flux<Producto> productos = productoService.findAllUpperCaseNameRepeat();
		 		 
		 model.addAttribute("productos", productos);
		 model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}
	
	@GetMapping({"/listar-chunked"})
	public String listarChunked(Model model) {
		 Flux<Producto> productos = productoService.findAllUpperCaseNameRepeat();
		 
		 model.addAttribute("productos", productos);
		 model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}
	
	@GetMapping("/listar-datadriver")
	public String listarDataDriver(Model model) {
		 Flux<Producto> productos = productoService.findAllUpperCaseName()
				 .delayElements(Duration.ofSeconds(1));
		 
		 productos.subscribe(producto -> log.info(producto.toString()));
		 
		 model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 1));
		 model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}

}
