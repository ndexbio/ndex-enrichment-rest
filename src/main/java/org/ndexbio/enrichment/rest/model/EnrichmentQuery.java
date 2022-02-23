package org.ndexbio.enrichment.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Represents an Enrichment Query
 * @author churas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichmentQuery {
    
    private SortedSet<String> _geneList;
    private SortedSet<String> _databaseList;
    private Map<String, String> _geneAnnotationServices;


	/**
	 * Gets the gene list passed in from setGeneList()
	 * @return 
	 */
    public SortedSet<String> getGeneList() {
        return _geneList;
    }

	/**
	 * Sets a new gene list adding genes that are non
	 * empty strings converting them to upper case
	 * @param _geneList 
	 */
	public void setGeneList(SortedSet<String> _geneList) {
		if (_geneList != null && !_geneList.isEmpty()) {
			SortedSet<String> uppercased = new TreeSet<>();
			for (String gene : _geneList) {
				String newGene = gene.toUpperCase().trim();
				if (newGene.length() > 0) {
					uppercased.add(newGene);
				}
			}
			this._geneList = uppercased;
		}
    }

	/**
	 * Gets database list passed in via setDatabaseList()
	 * @return 
	 */
    public SortedSet<String> getDatabaseList() {
        return _databaseList;
    }

	/**
	 * Adds database list passed in removing any 0 length entries.
	 * Also all entries are lower cased
	 * @param _databaseList 
	 */
	public void setDatabaseList(SortedSet<String> _databaseList) {  
		if (_databaseList != null &&!_databaseList.isEmpty()) {
			SortedSet<String> lowercased = new TreeSet<>();
			for (String database : _databaseList) {
				String newDatabase = database.toLowerCase().trim();
				if (newDatabase.length() > 0) {
					lowercased.add(newDatabase);
				}
    		}
        	this._databaseList = lowercased;
		}
    }
    
        @Schema(description="Map of gene Annotation services")
	public Map<String, String> getGeneAnnotationServices(){
		return this._geneAnnotationServices;
	}
	
	public void setGeneAnnotationServices(Map<String, String> _geneAnnotationServices){
		this._geneAnnotationServices = _geneAnnotationServices;
	}

    
	/**
	 * Generates a hashcode by creating a String containing
	 * a comma delimited list of databases followed by : then
	 * a comma delimited list of genes. The String is then
	 * run through a MD5 MessageDigest with the resulting byte
	 * array converted to a BigInteger and then returned 
	 * as an int.
	 * @return 
	 */
    @Override
    public int hashCode() {
        try {
			MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] messageDigest = md.digest(getDigestString().getBytes());
	        BigInteger no = new BigInteger(1, messageDigest);
	        return no.intValue();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} 

    }
	
	private String getDigestString(){
		StringBuilder sb = new StringBuilder();
		if (this.getDatabaseList() != null){
			sb.append(this
    			.getDatabaseList()
    			.stream()
    			.collect(Collectors.joining(",")));
		}
		sb.append(":");
		if (this.getGeneList() != null){
    			sb.append(this
    			.getGeneList()
    			.stream()
    			.collect(Collectors.joining(",")));
		}
		return sb.toString();
	}
    
	/**
	 * Compares two EnrichmentQuery objects
	 * by first building a String representation 
	 * of each with following format:
	 * <comma delimited database list>:<comma delimited gene list>
	 * 
	 * The string above is then compared with equalsIgnoreCase()
	 * and its value is returned
	 * @param o
	 * @return false if String above do not match or if object passed in
	 *         is not of type EnrichmentQuery
	 */
    @Override
	public boolean equals(Object o) {
	//Check type
		if (!(o instanceof EnrichmentQuery)) {
			return false;
		}
		EnrichmentQuery eq = (EnrichmentQuery) o;
    	
		//Compare contents
		return this.getDigestString().equalsIgnoreCase(eq.getDigestString());
    }
}
