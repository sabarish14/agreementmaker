package am.extension.collaborationClient.restful;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import am.app.ontology.ontologyParser.OntologyDefinition;
import am.app.ontology.ontologyParser.OntologyDefinition.OntologyLanguage;
import am.app.ontology.ontologyParser.OntologyDefinition.OntologySyntax;
import am.extension.collaborationClient.api.CollaborationAPI;
import am.extension.collaborationClient.api.CollaborationCandidateMapping;
import am.extension.collaborationClient.api.CollaborationFeedback;
import am.extension.collaborationClient.api.CollaborationTask;
import am.extension.collaborationClient.api.CollaborationUser;

public class RESTfulCollaborationServer implements CollaborationAPI {

	private static final String SEP = "/";
	
	private static final String REGISTER = "register";
	
	private static final String LISTTASKS = "listTasks";
	
	private String baseURI;
	
	public static final ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * @param baseURI The base URI of the server.
	 */
	public RESTfulCollaborationServer(String baseURI) {
		this.baseURI = baseURI;
	}
	
	@Override
	public CollaborationUser register() {
		String queryURI = baseURI + SEP + REGISTER;
		
		URL uri;
		try {
			uri = new URL(queryURI);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		
		URLConnection connection;
		try {
			connection = uri.openConnection();
			connection.setRequestProperty("Accept", "application/json");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			InputStream s = connection.getInputStream();
			/*StringBuilder content = new StringBuilder();
			int i = -1;
			while( (i = s.read()) != -1 ) content.append((char)i);
			System.out.println("Read from server:\n" + content.toString());*/
			
			return mapper.readValue(s, RESTfulUser.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<CollaborationTask> getTaskList() {
		String queryURI = baseURI + SEP + LISTTASKS;
		
		URL uri;
		try {
			uri = new URL(queryURI);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		
		URLConnection connection;
		try {
			connection = uri.openConnection();
			connection.setRequestProperty("Accept", "application/json");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			InputStream s = connection.getInputStream();
			/*StringBuilder content = new StringBuilder();
			int i = -1;
			while( (i = s.read()) != -1 ) content.append((char)i);
			System.out.println("Read from server:\n" + content.toString());*/
			
			return mapper.readValue(s, new TypeReference<List<RESTfulTask>>() {});
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public CollaborationCandidateMapping getCandidateMapping(
			CollaborationUser client) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putFeedback(CollaborationUser client,
			CollaborationFeedback feedback) {
		// TODO Auto-generated method stub
		
	}

	
	public static File downloadOWLFile(String url) {
		URL uri;
		try {
			uri = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		
		URLConnection connection;
		try {
			connection = uri.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		InputStream s = null;
		OutputStream o = null;
		try {
			s = connection.getInputStream();
			
			File tempFile = File.createTempFile("ont", ".owl");
			tempFile.deleteOnExit();
			o = new FileOutputStream(tempFile);
			
			int read = 0;
			byte[] bytes = new byte[1024];
	 
			while ((read = s.read(bytes)) != -1) {
				o.write(bytes, 0, read);
			}
			
			return tempFile;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (o != null) {
				try {
					o.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	 
			}
		}
	}
	
	@Override
	public OntologyDefinition getOntologyDefinition(String ontologyURL) {
		File ontFile = RESTfulCollaborationServer.downloadOWLFile(ontologyURL);
		OntologyDefinition o = new OntologyDefinition(true, ontFile.getAbsolutePath(), OntologyLanguage.OWL, OntologySyntax.RDFXML);
		return o;
	}
	
}
