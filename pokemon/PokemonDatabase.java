package pokemon;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;

import java.util.ArrayList;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PokemonDatabase {
	private Connection conn;  // Variable para establecer la conexion con la BD 

	
	public PokemonDatabase() {
		
	}

	// Funcion para establecer la conexion con la base de datos
	public boolean connect() {
		String serverAddress = "localhost:3306";
		String db = "pokemon";
		String user = "pokemon_user";
		String pass = "pokemon_pass";
		String url = "jdbc:mysql://" + serverAddress + "/" + db;
		
		if( conn == null ) { // Se verifica que no exista otra conexion 
			try {
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(url, user, pass);
				return true;
			} catch (ClassNotFoundException e) { // Excepcion para la carga del Driver
				System.out.println("Error al cargar el Driver: " + e.getMessage());
			} catch (SQLException e){ // Excepcion para el establecimiento de conexion
				System.out.println("Error al abrir la conexion: " + e.getMessage());
			}
		}
		
		return false;
	}

	// Funcion para cerrar la conexion con la base de datos
	public boolean disconnect() {
		if( conn != null ) { // Se verifica que realemte haya una conexion que cerrar
			try {
				conn.close();
				conn = null; // para poder abrir futuras conexiones
				return true;
			} catch (SQLException e ){
				System.out.println("Error al cerrar la conexion: " + e.getMessage());
			}
		}
		return false;
	}

	// Funcion para crear la tabla Aprende en la base de datos
	public boolean createTableAprende() {
		connect();
		String query = "CREATE TABLE aprende (" + 
					   "	n_pokedex INT, " +
					   "	id_ataque INT, " +
					   "	nivel INT, " +
					   "	PRIMARY KEY (n_pokedex, id_ataque)," +
					   "	FOREIGN KEY(n_pokedex) REFERENCES especie(n_pokedex)," +
					   "	FOREIGN KEY (id_ataque) REFERENCES ataque(id_ataque)" +
					   "		ON DELETE CASCADE ON UPDATE CASCADE" +
					   ");";
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(query);
			st.close();
			return true;
		} catch (SQLException e) { // Excepcion para errores durante la creacion de la tabla
			System.out.println("No se pudo crear la tabla aprende: " + e.getMessage());
		}
		return false;
	}

	// Funcion para la creacion de la tabla Conoce en la base de datos
	public boolean createTableConoce() {
		connect();
		String query = "CREATE TABLE IF NOT EXISTS  conoce (" + 
					   "	n_pokedex INT, " +
					   " 	n_encuentro INT, " +
					   " 	id_ataque INT, " +
					   "	PRIMARY KEY (n_pokedex, n_encuentro,id_ataque)," +
					   "	FOREIGN KEY(id_ataque) REFERENCES ataque(id_ataque)," +
					   "	FOREIGN KEY(n_pokedex, n_encuentro) REFERENCES ejemplar(n_pokedex, n_encuentro)" +					   
					   ");";
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(query);
			st.close();
			return true;
		} catch (SQLException e) { // Excepcion para errores durante la creacion de la tabla
			System.out.println("No se ha podido crear la tabla Conoce: " + e.getMessage());
		}	
		return false;
	
	}
	
	// Se cargan los datos en la tabla Aprende previamente creada
	public int loadAprende(String fileName) {
		connect();
		
		// Se cargan en el array objetos de la clase Aprende para luego ser ingresados la base de datos
		ArrayList<Aprende> listaAprende = Aprende.readData(fileName);
		int updates = 0;
		String query = "INSERT INTO aprende (n_pokedex, id_ataque, nivel) VALUES(?,?,?)";
		try { // Datos contenidos en el array se van ingresar en la tabla 
			PreparedStatement pst = conn.prepareStatement(query);		
			for(int i = 0; i < listaAprende.size(); i++){
				pst.setInt(1, listaAprende.get(i).getId_especie());
				pst.setInt(2, listaAprende.get(i).getId_ataque());
				pst.setInt(3, listaAprende.get(i).getNivel());
				updates = updates + pst.executeUpdate();
			}
			pst.close();
		} catch (SQLException e) { // Excepcion para errores en la carga de datos
			System.out.println("No se pudo insertar los valores en la tabla aprende: " + e.getMessage());
		}
		
		return updates;
	}

	// Se cargan los datos en la tabla Conoce previamente creada
	public int loadConoce(String fileName) {
		connect();

		// Se cargan en el array objetos de la clase Conoce para luego ser ingresados la base de datos
		ArrayList<Conoce> listaConoce = Conoce.readData(fileName);
		int updates = 0;
		
		try {
			conn.setAutoCommit(false); // Manejo manual del AutoCommit para las transacciones
			String query = "INSERT INTO conoce (n_pokedex, n_encuentro, id_ataque) VALUES(?,?,?)";
		
			try { // Los datos contenidos en el array se van ingresar en la tabla como una sola transaccion
				PreparedStatement pst = conn.prepareStatement(query);
				for(int i = 0; i < listaConoce.size(); i++){
					pst.setInt(1, listaConoce.get(i).getId_especie());
					pst.setInt(2, listaConoce.get(i).getN_encuentro());
					pst.setInt(3, listaConoce.get(i).getId_ataque());
					updates = updates + pst.executeUpdate();
				}
				conn.commit();
				pst.close();
			
			} catch (SQLException e){ 
				conn.rollback(); // En caso de Excepcion se devuelven los cambios realizados en la transaccion 
				System.out.println("No se pudo insertar los valores en la tabla conoce: " + e.getMessage());
			}
		} catch (SQLException e){ // Excepcion si no se pudo poner el autoCommit a false
			System.out.println("No se pudo realizar la transaccion: " + e.getMessage());
		}
		
		try { // Se procede a poner el AutoCommit nuevamente a true y en caso de no poder se notifica
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			System.out.println("Error al restablecer el autocommit " + e.getMessage());
		}
		
		return updates;
	}

	// Funcion para mostrar los pokedex que se tienen en la base de Datos
	public ArrayList<Especie> pokedex() {
	    connect();
	    
	    ArrayList<Especie> especies = new ArrayList<Especie>();
	    
	    try{
	    	Statement st = conn.createStatement();
	     	ResultSet rs = st.executeQuery("SELECT * FROM especie;");
	     
	     	while(rs.next()){ // Se crea cada objeto de la clase Especie con sus especificaciones
		      Especie especie = new Especie();
		      especie.setN_pokedex(rs.getInt("n_pokedex"));
		      especie.setNombre(rs.getString("nombre"));
		      especie.setDescripcion(rs.getString("descripcion"));
		      especie.setEvoluciona(rs.getInt("evoluciona"));
		      especies.add(especie); // Se almacenan los pokedex en un array para ser retornados de esta manera
		    }

	        rs.close();        
	        st.close();

	    } catch (SQLException e){
	     	System.out.println("Error de SQL al obtener los datos de las especies:" + e.getMessage());    
	     	return null;
	     }
	   
	    return especies;
	}

	// Funcion que retorna los ejemplares ordenados que estan almacenados en la base de datos
	public ArrayList<Ejemplar> getEjemplares() {
		connect();

		ArrayList<Ejemplar> ejemplares = new ArrayList<Ejemplar>();
		String query = "SELECT * FROM ejemplar ORDER BY n_pokedex ASC, n_encuentro ASC;";

		try{
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while(rs.next()){
				Ejemplar ejemplar = new Ejemplar();
				ejemplar.setN_pokedex(rs.getInt("n_pokedex"));
				ejemplar.setN_encuentro(rs.getInt("n_encuentro"));
				ejemplar.setApodo(rs.getString("apodo"));
				ejemplar.setSexo(rs.getString("sexo").charAt(0));
				ejemplar.setNivel(rs.getInt("nivel"));
				ejemplar.setInfectado(rs.getInt("infectado"));
				ejemplares.add(ejemplar);
			}

			rs.close();
			st.close();

		}catch (SQLException e) {
			System.out.println("Error al obtener los datos de la tabla ejemplares " + e.getMessage());
			return null;
		}

		return ejemplares;
	}

	// Funcion que expande un virus sobre los pokedex almacenados en un array
	public int coronapokerus(ArrayList<Ejemplar> ejemplares, int dias) {
		connect();
		Ejemplar ejemplar_aleatorio = new Ejemplar();
		String query = "UPDATE ejemplar SET infectado=? WHERE n_pokedex=?;";

		int n_contagiados = 0;
		int aux = 1;

		try{
			PreparedStatement pst = conn.prepareStatement(query);
			for(int i = 1; i <= dias; i++){
				try{
					conn.setAutoCommit(false);  // Manejo manual del AutoCommit para las transacciones
					while(aux > 0){
						ejemplar_aleatorio = Ejemplar.ejemplarRandom(ejemplares);
							if (ejemplar_aleatorio.getInfectado() == 0){
								pst.setInt(1, 1);
								pst.setInt(2, ejemplar_aleatorio.getN_pokedex());
								pst.executeUpdate();
								n_contagiados++;
							}
						aux--;
					}
					conn.commit();
					aux = n_contagiados;
				} catch(SQLException e) {
					conn.rollback(); // En caso de Excepcion se devuelven los cambios realizados en la transaccion 
					System.out.println("Error al realizar la propagacion: " + e.getMessage());
				}
			}
			pst.close();
		} catch(SQLException e) {
			System.out.println("Error al realizar la propagacion: " + e.getMessage());
		}
		
		try { // Se procede a poner el AutoCommit nuevamente a true y en caso de no poder se notifica
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			System.out.println("Error al restablecer el autocommit " + e.getMessage());
		}
		
		return n_contagiados;
	}
	
	// Funcion que obtiene la imagen cierto pokedex almacenada en la base de datos y la guarda en un ruta especifica
	public boolean getSprite(int n_pokedex, String filename) {
		connect();

		String query = "SELECT sprite FROM especie WHERE n_pokedex=?;";

		try {
			PreparedStatement pst = conn.prepareStatement(query);		
			pst.setInt(1, n_pokedex);
			ResultSet rs = pst.executeQuery();
			FileOutputStream photo = new FileOutputStream(filename);

			while(rs.next()){
				InputStream input = rs.getBinaryStream("sprite");
				byte[] buffer = new byte[1024]; // Se almacenan los bytes de la imagen
				try {
					while(input.read(buffer) > 0){
						photo.write(buffer);
					}
				} catch (IOException e) {
					System.out.println("Error al leer la imagen de la base de datos: " + e.getMessage());
				} catch(NullPointerException e) { // En caso de que se quiera leer un array de bytes vacios
		            System.out.println("Problemas al cargar la imagen: " + filename + " " + e.getMessage()); 
		        } 
			}
			photo.close();
			rs.close();
			pst.close();
			return true;
			
		} catch (SQLException e) {
			System.out.println("No se pudo obtener la imagen: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println("Ruta de guardado no encontrada: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Error al cerrar el fichero: " + e.getMessage());
		}

		return false;
	}

}
