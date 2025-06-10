package com.alura.literatura.principal;

import com.alura.literatura.dto.DatosLibro;
import com.alura.literatura.dto.DatosRespuesta;
import com.alura.literatura.model.Autor;
import com.alura.literatura.model.Libro;
import com.alura.literatura.repository.AutorRepository;
import com.alura.literatura.repository.LibroRepository;
import com.alura.literatura.service.ConsumoAPI;
import com.alura.literatura.service.ConvierteDatos;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Principal {

    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private LibroRepository libroRepository;
    private AutorRepository autorRepository;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar libro por título
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                                        
                    0 - Salir
                    """;
            System.out.println(menu);

            try {
                opcion = teclado.nextInt();
                teclado.nextLine();

                switch (opcion) {
                    case 1:
                        buscarLibroWeb();
                        break;
                    case 2:
                        mostrarLibrosRegistrados();
                        break;
                    case 3:
                        mostrarAutoresRegistrados();
                        break;
                    case 4:
                        mostrarAutoresVivosEnAnio();
                        break;
                    case 5:
                        mostrarLibrosPorIdioma();
                        break;


                    case 0:
                        System.out.println("Cerrando la aplicación...");
                        break;
                    default:
                        System.out.println("Opción inválida");
                }
            } catch (InputMismatchException e) {
                System.out.println("Error: Ingrese un número válido");
                teclado.nextLine();
            }
        }
    }

    private DatosLibro getDatosLibro() {
        System.out.println("Escribe el nombre del libro que deseas buscar:");
        var tituloLibro = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + "?search=" + tituloLibro.replace(" ", "+"));
        var datosBusqueda = conversor.obtenerDatos(json, DatosRespuesta.class);

        Optional<DatosLibro> libroBuscado = datosBusqueda.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
                .findFirst();

        if (libroBuscado.isPresent()) {
            return libroBuscado.get();
        } else {
            System.out.println("Libro no encontrado");
            return null;
        }
    }

    private void buscarLibroWeb() {
        DatosLibro datos = getDatosLibro();
        if (datos != null) {
            // Verificar si el libro ya existe
            Optional<Libro> libroExistente = libroRepository.findByTituloContainsIgnoreCase(datos.titulo());
            if (libroExistente.isPresent()) {
                System.out.println("El libro ya está registrado:");
                System.out.println(libroExistente.get());
                return;
            }

            // Crear o buscar autor
            Autor autor;
            if (!datos.autores().isEmpty()) {
                var datosAutor = datos.autores().get(0);
                Optional<Autor> autorExistente = autorRepository.findByNombre(datosAutor.nombre());
                if (autorExistente.isPresent()) {
                    autor = autorExistente.get();
                } else {
                    autor = new Autor(datosAutor);
                    autor = autorRepository.save(autor);
                }
            } else {
                System.out.println("No se encontró información del autor");
                return;
            }

            // Crear y guardar libro
            Libro libro = new Libro(datos, autor);
            libroRepository.save(libro);
            System.out.println("Libro guardado exitosamente:");
            System.out.println(libro);
        }
    }

    private void mostrarLibrosRegistrados() {
        List<Libro> libros = libroRepository.findAll();
        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados");
        } else {
            System.out.println("Libros registrados:");
            libros.forEach(System.out::println);
        }
    }

    private void mostrarAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAllOrderByNombre();
        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados");
        } else {
            System.out.println("Autores registrados:");
            autores.forEach(System.out::println);
        }
    }

    private void mostrarAutoresVivosEnAnio() {
        System.out.println("Ingrese el año vivo de autor(es) que desea buscar:");
        try {
            var anio = teclado.nextInt();
            teclado.nextLine();

            List<Autor> autoresVivos = autorRepository.findAutoresVivosEnAnio(anio);
            if (autoresVivos.isEmpty()) {
                System.out.println("No se encontraron autores vivos en el año " + anio);
            } else {
                System.out.println("Autores vivos en " + anio + ":");
                autoresVivos.forEach(System.out::println);
            }
        } catch (InputMismatchException e) {
            System.out.println("Error: Ingrese un año válido");
            teclado.nextLine();
        }
    }

    private void mostrarLibrosPorIdioma() {
        var menuIdioma = """
                Seleccione el idioma:
                1 - Español (es)
                2 - Inglés (en)
                3 - Francés (fr)
                4 - Portugués (pt)
                """;
        System.out.println(menuIdioma);

        try {
            var opcion = teclado.nextInt();
            teclado.nextLine();

            String idioma = switch (opcion) {
                case 1 -> "es";
                case 2 -> "en";
                case 3 -> "fr";
                case 4 -> "pt";
                default -> {
                    System.out.println("Opción inválida");
                    yield null;
                }
            };

            if (idioma != null) {
                List<Libro> librosPorIdioma = libroRepository.findByIdioma(idioma);
                if (librosPorIdioma.isEmpty()) {
                    System.out.println("No se encontraron libros en el idioma seleccionado");
                } else {
                    System.out.println("Libros en " + idioma + ":");
                    librosPorIdioma.forEach(System.out::println);
                }
            }
        } catch (InputMismatchException e) {
            System.out.println("Error: Ingrese una opción válida");
            teclado.nextLine();
        }
    }

    private void mostrarEstadisticasPorIdioma() {
        System.out.println("Estadísticas de libros por idioma:");

        String[] idiomas = {"es", "en", "fr", "pt"};
        String[] nombresIdiomas = {"Español", "Inglés", "Francés", "Portugués"};

        boolean hayLibros = false;

        for (int i = 0; i < idiomas.length; i++) {
            Long cantidad = libroRepository.countByIdioma(idiomas[i]);
            if (cantidad > 0) {
                System.out.println(nombresIdiomas[i] + ": " + cantidad + " libro(s)");
                hayLibros = true;
            }
        }

        if (!hayLibros) {
            System.out.println("No hay libros registrados en ningún idioma");
        }
    }
}