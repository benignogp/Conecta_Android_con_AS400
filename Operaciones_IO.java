package com.ventas.red;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.os.Environment;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileOutputStream;
import com.ibm.as400.access.IFSFileReader;
import com.ibm.as400.access.IFSFileWriter;

public class Operaciones_IO {

	public Operaciones_IO() {
	}

	// Lee un fichero del IFS y devuelve su contenido.
	public String leerFichero(String rutaFichero, String strServidor,
			String strUser, String strPassword) {
		StringBuilder lecturaFichero = new StringBuilder();
		AS400 conAS400 = new AS400(strServidor, strUser, strPassword);
		try {
			conAS400.connectService(AS400.FILE);
			IFSFile fichIFS = new IFSFile(conAS400, rutaFichero);
			BufferedReader brReader = new BufferedReader(new IFSFileReader(fichIFS, 1252));
			String linea;
			while ((linea = brReader.readLine()) != null) {
				lecturaFichero.append(linea);
			}
			brReader.close();
			conAS400.disconnectService(AS400.FILE);
			return lecturaFichero.toString();
		} catch (AS400SecurityException ex) {
			System.out.print("error seguridad");
			return null;
		} catch (IOException ex) {
			System.out.print("error seguridad");
			return null;
		}
	}

	// Obtiene la lista de imagenes jpg de una carpeta del IFS.
	public String[] listarFicherosIFS(String strServidor, String rutaIFS,
			String strUser, String strPassword) {
		AS400 conAS400 = new AS400(strServidor, strUser, strPassword);
		try {
			conAS400.connectService(AS400.FILE);
			IFSFile fichIFS = new IFSFile(conAS400, rutaIFS);
			return fichIFS.list("*.jpg");
		} catch (AS400SecurityException e) {
			System.out.println(e);
			return null;
		} catch (IOException e) {
			System.out.println(e);
			return null;
		}
	}

	// Retorna valor negativo si no logra tener acceso al fichero IFS o local.
	public int Copy_FichIFS_To_FichLocal(String rutaIFS, String ficheroLocal,
			String strServidor, String strUser, String strPassword) {
		File ruta_sd = Environment.getExternalStorageDirectory();
		AS400 conAS400 = new AS400(strServidor, strUser, strPassword);
		try {
			if ((rutaIFS == null) || (ruta_sd == null)) {
				return -2;
			}
			conAS400.connectService(AS400.FILE);
			IFSFile fichIFS = new IFSFile(conAS400, rutaIFS);
			if (!(fichIFS.exists())) {
				return -3;
			}
			File fichLocal = new File(ruta_sd.getAbsolutePath(), ficheroLocal);
			if (fichLocal.exists()) {
				fichLocal.delete();
			}
			if (!fichLocal.createNewFile()) {
				return -4;
			}
			InputStream in = new IFSFileInputStream(conAS400, rutaIFS, 1252);
			OutputStream out = new FileOutputStream(fichLocal);
			byte[] buffer = new byte[1024];
			int longCaracteres;
			while ((longCaracteres = in.read(buffer)) > 0) {
				out.write(buffer, 0, longCaracteres);
			}
			in.close();
			out.close();
			conAS400.disconnectService(AS400.FILE);
		} catch (Exception ex) {
			return -5;
		}
		return 0;
	}

	// Retorna valor negativo si no logra tener acceso al fichero IFS o local.
	public int Copy_FichLocal_To_FichIFS(String rutaIFS, String ficheroLocal,
			String strServidor, String strUser, String strPassword) {
		File ruta_sd = Environment.getExternalStorageDirectory();
		AS400 conAS400 = new AS400(strServidor, strUser, strPassword);
		try {
			if ((rutaIFS == null) || (ruta_sd == null)) {
				return -2;
			}
			File fichLocal = new File(ruta_sd.getAbsolutePath(), ficheroLocal);
			if (!(fichLocal.exists())) {
				return -3;
			}
			conAS400.connectService(AS400.FILE);
			IFSFile fichIFS = new IFSFile(conAS400, rutaIFS);
			if (fichIFS.exists()) {
				fichIFS.delete();
				if (!fichIFS.createNewFile()) {
					return -4;
				}
			} else {
				if (!fichIFS.createNewFile()) {
					return -4;
				}
			}
			InputStream in = new FileInputStream(fichLocal);
			InputStreamReader isr = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(isr);
			OutputStream out = new IFSFileOutputStream(conAS400, rutaIFS);
			String line;
			while ((line = br.readLine()) != null) {
				out.write(line.getBytes("LATIN2"));
			}
			out.close();
			br.close();
			isr.close();
			in.close();
			conAS400.disconnectService(AS400.FILE);
		} catch (Exception ex) {
			return -5;
		}
		return 0;
	}

	public boolean Copy_To_FichIFS(String rutaIFS, String contenidoFichero,
			String strServidor, String strUser, String strPassword) {
		AS400 conAS400 = new AS400(strServidor, strUser, strPassword);
		try {
			if (rutaIFS == null) {
				return false;
			}
			conAS400.connectService(AS400.FILE);
			IFSFile fichIFS = new IFSFile(conAS400, rutaIFS);
			if (fichIFS.exists()) {
				fichIFS.delete();
				if (!fichIFS.createNewFile()) {
					return false;
				}
			} else {
				if (!fichIFS.createNewFile()) {
					return false;
				}
			}
			fichIFS.setCCSID(1252);
			PrintWriter writer = new PrintWriter(new BufferedWriter(new IFSFileWriter(fichIFS)));
			// Escribe el texto en el archivo, convirtiendo caracteres.
			writer.print(contenidoFichero);
			// Cierre el archivo.
			writer.close();
		} catch (Exception ex) {
			System.out.print(ex);
			return false;
		}
		return true;
	}

	public void escribeFicheroSDCARD(String contenidoFichero, String strFichero) {
		boolean sdDisponible = false;
		boolean sdAccesoEscritura = false;
		// Comprobamos el estado de la memoria externa (tarjeta SD)
		String estado = Environment.getExternalStorageState();
		if (estado.equals(Environment.MEDIA_MOUNTED)) {
			sdDisponible = true;
			sdAccesoEscritura = true;
		} else if (estado.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			sdDisponible = true;
			sdAccesoEscritura = false;
		} else {
			sdDisponible = false;
			sdAccesoEscritura = false;
		}
		try {
			if (sdDisponible | sdAccesoEscritura) {
				File ruta_sd = Environment.getExternalStorageDirectory();
				File f = new File(ruta_sd.getAbsolutePath(), strFichero);
				if (f.exists()) {
					f.delete();
					f.createNewFile();
				} else {
					f.createNewFile();
				}
				OutputStreamWriter fout = new OutputStreamWriter(new FileOutputStream(f), "WINDOWS-1252");
				fout.write(contenidoFichero);
				fout.close();
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	// Copia ficheros de un directorio IFS a la tarjeta de un android
	public void copiarFichIFStoTarjetaMemoria(String servidor, String strUser,
			String strPassword, String rutaFichFuente, String rutaDestino,
			String nombreFichero) {
		IFSFileInputStream fuente = null;
		FileOutputStream destino = null;
		boolean sdDisponible = false;
		boolean sdAccesoEscritura = false;
		byte[] buffer = new byte[1024 * 64];
		AS400 conAS400 = new AS400(servidor, strUser, strPassword);
		try {
			conAS400.connectService(AS400.FILE);
			fuente = new IFSFileInputStream(conAS400, rutaFichFuente + nombreFichero, IFSFileInputStream.SHARE_WRITERS);
			// Comprobamos el estado de la memoria externa (tarjeta SD)
			String estado = Environment.getExternalStorageState();
			if (estado.equals(Environment.MEDIA_MOUNTED)) {
				sdDisponible = true;
				sdAccesoEscritura = true;
			} else if (estado.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
				sdDisponible = true;
				sdAccesoEscritura = false;
			} else {
				sdDisponible = false;
				sdAccesoEscritura = false;
			}
			try {
				if (sdDisponible | sdAccesoEscritura) {
					File ruta_sd = Environment.getExternalStorageDirectory();
					File directorio = new File(ruta_sd.getAbsolutePath() + rutaDestino);
					if (!directorio.exists()) {
						directorio.mkdir();
					}
					File f = new File(ruta_sd.getAbsolutePath() + rutaDestino, nombreFichero);
					if (f.exists()) {
						f.delete();
						f.createNewFile();
					} else {
						f.createNewFile();
					}
					destino = new FileOutputStream(f);
					int bytesRead = fuente.read(buffer);
					while (bytesRead > 0) {
						destino.write(buffer, 0, bytesRead);
						bytesRead = fuente.read(buffer);
					}
				}
			} catch (Exception ex) {
				System.out.println(ex);
			}

		} catch (AS400SecurityException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
		conAS400.disconnectService(AS400.FILE);
	}

	public boolean existeConexion(String servidor, String strUser,
			String strPassword) {
		boolean estaConectado;
		AS400 conAS400 = new AS400(servidor, strUser, strPassword);
		try {
			conAS400.connectService(AS400.FILE);
			estaConectado = conAS400.isConnected();
			conAS400.disconnectService(AS400.FILE);
			return estaConectado;
		} catch (Exception e) {
			conAS400.disconnectService(AS400.FILE);
			return false;
		}
	}

	public void copiarFicheroSdcard(String fileOrigen, String rutaOrigen,
			String fileDestino, String rutaDestino) {

		FileOutputStream out;
		File file;
		InputStream is;
		try {
			is = new FileInputStream(rutaOrigen + fileOrigen);
			File directorio = new File(rutaDestino);
			if (!directorio.exists()) {
				directorio.mkdir();
			}
			file = new File(rutaDestino + fileDestino);
			if (file.exists()) {
				file.delete();
				file.createNewFile();
			} else {
				file.createNewFile();
			}
			out = new FileOutputStream(file);
			byte buf[] = new byte[1024 * 4];
			do {
				int numread;

				numread = is.read(buf);
				if (numread <= 0)
					break;
				out.write(buf, 0, numread);
			} while (true);
			is.close();
			out.close();

		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public String obtenerRutaSDCard() {
		boolean sdDisponible = false;
		boolean sdAccesoEscritura = false;
		File ruta_sd;
		String estado = Environment.getExternalStorageState();
		if (estado.equals(Environment.MEDIA_MOUNTED)) {
			sdDisponible = true;
			sdAccesoEscritura = true;
		} else if (estado.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			sdDisponible = true;
			sdAccesoEscritura = false;
		} else {
			sdDisponible = false;
			sdAccesoEscritura = false;
		}
		if (sdDisponible | sdAccesoEscritura) {
			ruta_sd = Environment.getExternalStorageDirectory();
			return ruta_sd.getAbsolutePath();
		} else {
			return "";
		}
	}

}
