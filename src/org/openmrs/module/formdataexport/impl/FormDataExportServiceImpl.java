package org.openmrs.module.formdataexport.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.module.formdataexport.db.FormDataExportDAO;
import org.openmrs.reporting.export.DataExportReportObject;
import org.openmrs.reporting.export.SimpleColumn;
import org.openmrs.util.FormUtil;
import org.openmrs.util.OpenmrsUtil;

/**
 * Form-based export service.
 * 
 * @author Justin Miranda
 * @version 1.0
 */
public class FormDataExportServiceImpl implements FormDataExportService {

	private Log log = LogFactory.getLog(this.getClass());
	
	private FormDataExportDAO dao;
	
	private static String FORM_DATA_EXPORT_PREFIX = "FORM_DATA_EXPORT"; 

	private static String FORM_DATA_EXPORT_EXTENSION = ".csv"; 

	private static DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy");
	
	private static final String DEFAULT_QUOTE = "\"";

	private static final String DEFAULT_COLUMN_SEPARATOR = ",";

	private static final String DEFAULT_LINE_SEPARATOR = "\n";

	private static final int DEFAULT_CONCEPT_NAME_LENGTH = 20;
	
	private static final String CONCEPT_FIELD_TYPE = "Concept";

	private static final String UNDERSCORE = "_";
	
	private static final String EMPTY = "";

	
	public FormDataExportServiceImpl() { }
	

	
	/**
	 * 
	 * @return
	 */
	public FormDataExportDAO getDao() {
		return dao;
	}

	
	/**
	 * 
	 * @param dao
	 */
	public void setDao(FormDataExportDAO dao) {
		this.dao = dao;
	}


	/**
	 * Create a file used to store the exported data on the filesystem.
	 * 
	 * @param	formName	the name of the form 
	 */
	public static File createExportFile(String formName) {
        File dir = new File(OpenmrsUtil.getApplicationDataDirectory(), "dataExports");
        dir.mkdirs();
        String filename = FORM_DATA_EXPORT_PREFIX + "_" + formName.replace(" ", "_") + FORM_DATA_EXPORT_EXTENSION;
        //filename = (new StringBuilder()).append(filename).toString();
        File file = new File(dir, filename);
        return file;
    }



	/**
	 * Convenience method to add most common export columns.
	 * 
	 * @param export
	 */
	public void addStaticColumns(DataExportReportObject export) { 
		
		if ( export != null ) { 
			export.getColumns().add(new SimpleColumn("PATIENT_ID", "$!{fn.patientId}"));		
			export.getColumns().add(new SimpleColumn("IMB_ID", "$!{fn.getPatientIdentifier('IMB ID')}"));		
			export.getColumns().add(new SimpleColumn("GENDER", "$!{fn.getPatientAttr('Person', 'gender')}"));		
			export.getColumns().add(new SimpleColumn("AGE", "$!{fn.calculateAge($fn.getPatientAttr('Person', 'birthdate'))}"));		
			export.getColumns().add(new SimpleColumn("BIRTHDATE", "$!{fn.formatDate('short', $fn.getPatientAttr('Person', 'birthdate'))}"));		
			export.getColumns().add(new SimpleColumn("LOCATION", "$!{fn.getPatientAttr('PatientIdentifier', 'location').getName()}"));		
			export.getColumns().add(new SimpleColumn("ENCOUNTER_DATE", "$!{fn.formatDate('short', $!{fn.getLastEncounterAttr([''], 'encounterDatetime')})}"));		
		}
	}
	
	
	/**
	 * 
	 * @param form
	 * @param cohortKey
	 * @param extras
	 * @return
	 */
	public File exportEncounterData(Form form, String cohortKey, String [] extras) { 
		
		Cohort cohort = new Cohort();
		
		/*
		if (!cohortKey.isEmpty()) {     			
			log.info("Using cohort " + cohortKey);
			CohortDefinition cohortDefinition = Context.getCohortService().getCohortDefinition(cohortKey);
			cohort = Context.getCohortService().evaluate(cohortDefinition, null);
		} 
		*/
		//else {
		
		// Fixed NPE 
		List<EncounterType> encounterTypes = new ArrayList<EncounterType>();					
		encounterTypes.add(form.getEncounterType());
		log.info("Using patients with encounters");
		cohort = Context.getPatientSetService().
			getPatientsHavingEncounters(encounterTypes, null, form, null, null, null, null);	    			
		//} 

		log.info("COHORT = " + cohort);
		
		// If cohort is still null, use the cohort of all patients in the system
		if (cohort == null || cohort.isEmpty()) { 
			CohortDefinition allPatientCohort = Context.getCohortService().getAllPatientsCohortDefinition();				
			cohort = Context.getCohortService().evaluate(allPatientCohort, null);			
		}					
		
		
		return exportEncounterData(form, cohort, extras);
	}
	
	
	/**
	 * Export form encounter data for the given form and patients.
	 *  
	 * @param 	form		the form to be exported	
	 * @param	patients	the patients to include in the export (included only to filter out encounters)
	 * @param 	extras		the extra columns to include in the export (for each form field)
	 * @return	the export data file
	 */
	public File exportEncounterData(Form form, Cohort cohort, String [] extras) { 

		File exportFile = null;
		
		if (form == null) { 
			throw new APIException("Form does not exist");
		}
		
		try { 

			// Export buffer to hold all columns/data to be exported
			StringBuffer exportBuffer = new StringBuffer();
			
			// Add the given form to list
			List<Form> forms = new ArrayList<Form>();			
			forms.add(form);
						
			// Get all encounters for the given form, filter by the given patients (null = use all)
			List<Encounter> encounters = 
				dao.getEncountersByForm(cohort, forms);
			
			// Create a map to keep track of the data 
			Map<Encounter, Map<Concept, List<Obs>>> dataMap = new HashMap<Encounter, Map<Concept, List<Obs>>>();
			
			// Keeps track of the maximum number of columns to create for each concept
			Map<Concept, Integer> fieldMap = new HashMap<Concept, Integer>();
				
			// Create the dataset maps
			populateMaps(dataMap, fieldMap, encounters);

			// Write the column headers to the export buffer
			writeColumnHeaders(form, fieldMap, exportBuffer);		

			// Write the column data to the export buffer
			writeColumnData(form, dataMap, fieldMap, exportBuffer);

			// Create the file to be used to export the data
	    	exportFile = createExportFile(form.getName());		
	    	
	    	// Write out the export buffer to the export file
		    Writer exportOutput = new BufferedWriter( new FileWriter(exportFile) );
		    exportOutput.write( exportBuffer.toString() );			

	    	// Close output 
	    	exportOutput.close();
	    	
		} catch (Exception e) { 
			log.error("An error occured while export form data ", e);
		}
		return exportFile;

	}
	
	
	/**
	 * 
	 * @param dataMap
	 * @param fieldMap
	 * @param encounters
	 */
	public void populateMaps(Map<Encounter, Map<Concept, List<Obs>>> dataMap, 
								Map<Concept, Integer> fieldMap, 
								List<Encounter> encounters) { 
		// Iterate over all encounters
		for (Encounter encounter : encounters) { 
			
			// Get all of the observations for a given encounter
			Map<Concept, List<Obs>> obsListMap = dataMap.get(encounter);
			if (obsListMap == null) obsListMap = new HashMap<Concept, List<Obs>>();
			
			// If the encounter has observations, we process them  
			if ( encounter.getObs() != null && !encounter.getObs().isEmpty()) { 
			
				// Iterate through the list of observations for this encounter
				for (Obs obs : encounter.getObs()) {
					
					
					if (obs != null && !obs.isVoided()) { 
						/*
						log.info("\tObs " + obs + " " + 
								obs.getConcept().getName() + "(" + 
								obs.getConcept().getConceptId() + ") = " + 
								obs.getValueAsString(Locale.US));
						*/
						// Get the concept and it's observations
						Concept concept = obs.getConcept();				
						List<Obs> obsList = obsListMap.get(concept);
						
						// If we haven't encountered this concept before, we create a new observation list
						if (obsList == null) obsList = new LinkedList<Obs>();
					
						// Add the current observation to the list
						obsList.add(obs);	
						
						// Add the current concept's obs list to the map
						obsListMap.put(obs.getConcept(), obsList);
						
						// Since we might have multiple obs values for one concept in a single 
						// encounter, we need keep track of the max
						// number of observations for a single concept over the entire data set
						//
						// This is used when displaying columns for the a concept that may have 
						// multiple observations within a single row.  In order to display all 
						// values, we need all rows to display the same number of columns even
						// if most rows have less than the max.  In the following example, the
						// row with ID = 1 has 3 obs values, so for each row we need to display
						// 3 columns for this particular concept.
						// 
						//
						// 			ID 		COL_1	COL_2	COL_3
						//			1 		129.0	185.0	248.0
						//			2		492.0
						//			3		124.0	482.0		
						//
						Integer currentObsListSize = obsList.size();
						Integer maximumObsListSize = (fieldMap.get(concept)!=null) ? fieldMap.get(concept) : 0;					
						if (currentObsListSize > maximumObsListSize) {
							//log.info(" - MaxSize for " + concept.getName() + " = " + currentObsListSize);
							fieldMap.put(concept, currentObsListSize);
						}				
					}
				}
			}
			// Include encounters with no observations
			dataMap.put(encounter, obsListMap);
		}				
		
	}
	
	/**
	 * Write out the form column headers for the export.
	 * 
	 * @param form
	 * @param exportBuffer
	 */
	public void writeColumnHeaders(Form form, Map<Concept, Integer> fieldMap, StringBuffer exportBuffer) { 
		
		// Write out static columns
		exportBuffer.
			append(DEFAULT_QUOTE).append("ENCOUNTER ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
			append(DEFAULT_QUOTE).append("ENCOUNTER DATE").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
			append(DEFAULT_QUOTE).append("PATIENT ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
			append(DEFAULT_QUOTE).append("IMB ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);

		
		TreeMap<Integer, TreeSet<FormField>> formStructure = 
			FormUtil.getFormStructure(form);
		
		StringBuffer fieldNumber = new StringBuffer();
		
		// Iterate over high level nodes
		for(Integer id : formStructure.keySet()) { 

			FormField parent = Context.getFormService().getFormField(id);
			
			fieldNumber.setLength(0);
			
			if (parent != null) { 
				log.info("Parent form field: " + parent.getFormFieldId() + " " + parent.getFieldNumber() );
			} 
			else {
				log.info("Parent id: " + id);
			}
			

			
			
			// Iterate over the actual questions
			TreeSet<FormField> formFields = formStructure.get(id);
				
			if (formFields != null && !formFields.isEmpty()) { 
			
				for(FormField formField : formFields) { 				
					
					Field field = formField.getField();
					
					// Only process concept form fields
					if (CONCEPT_FIELD_TYPE.equals(field.getFieldType().getName())) { 
												
						// Create a column name based on the concept
						Concept concept = field.getConcept();
						
						// Check if concept exists (it should)
						if (concept != null) { 
							
							
							
							int numberOfOccurrences = 
								(fieldMap.get(concept)!=null)?fieldMap.get(concept):1;		
								
							for (int occurrence=1; occurrence<=numberOfOccurrences; occurrence++) {							
								
								
								// If concept is a concept set, we'll ignore it because we'll see it later
								if ( concept.getConceptSets()!= null && !concept.getConceptSets().isEmpty()) { 

									/*
									for (ConceptSet cs : formField.getField().getConcept().getConceptSets()) { 									
										log.info("\t - Concept Set Field: " + field.getFieldId() + " - " + 
														cs.getConcept().getConceptId() + " " + field.getName());
										addColumnHeader(exportBuffer, fieldNumber.toString(), cs.getConcept(), i, false);		
									}
									*/	
								} 
								else { 
									// Otherwise, just create a new column for the concept
									String fieldName = createColumnHeader(formField, occurrence);
									log.info("\tConcept Field: " + fieldName);
									addColumnHeader(exportBuffer, fieldName, false);
								}
							}						
						}
					}
				}
			}
		}
		exportBuffer.append("\n");
		log.info("Column Buffer: " + exportBuffer.toString());		
	}
	
	
	
	/**
	 * Creates a column header for the given form field, as well as the number of times it appears in the form.
	 * @param formField
	 * @param occurrence
	 * @return
	 */
	public String createColumnHeader(FormField formField, int occurrence) { 
		
		StringBuffer buffer = new StringBuffer();

		// Try to get the field number from the current field
		String fieldNumber = getFieldNumber(formField);		
		
		// If there is no field number then we try to get the field number for the parent
		if (fieldNumber == null || EMPTY.equals(fieldNumber)) { 
			if (formField.getParent() != null) {
				fieldNumber = getFieldNumber(formField.getParent());
			}
		}
		buffer.append(fieldNumber);
		
	
		Concept concept = formField.getField().getConcept();
		
		// By default set field name, but try to use concept short name
		String fieldName = "";		
		if (concept.getName().getShortName()!=null&&!EMPTY.equals(concept.getName().getShortName())) {
			fieldName = concept.getName().getShortName();
		} 
		else { 
			fieldName = concept.getName().getName();
		}		
		
		// Replace unwanted characters and change case to upper  
		fieldName = fieldName.replaceAll("\\s", "_");		
		fieldName = fieldName.replaceAll("-", "_");		
		fieldName = fieldName.toUpperCase();
		
		
		// Now we make sure that the field fits in the 
		if (fieldName.length() > DEFAULT_CONCEPT_NAME_LENGTH) {
			buffer.append(fieldName.substring(0, DEFAULT_CONCEPT_NAME_LENGTH));
		}
		else { 
			buffer.append(fieldName);				
		}		
		buffer.append("_").append(concept.getConceptId());

		// If the concept occurs in the form more than once, then we need to distinguish it's columns
		if (occurrence > 1) { 
			buffer.append("_").append(occurrence); 
		}				

				
		return buffer.toString();
	}
	
	
	/**
	 * Gets the field number of the given form field.
	 * 
	 * @param formField
	 * @return
	 */
	private String getFieldNumber(FormField formField) { 
		StringBuffer buffer = new StringBuffer();
		// Get the field number and part
		if (formField != null) { 
			if (formField.getFieldNumber()!=null&&!EMPTY.equals(formField.getFieldNumber())) { 
				buffer.append(formField.getFieldNumber());
				if (formField.getFieldPart()!=null&&!EMPTY.equals(formField.getFieldPart())) { 
					buffer.append(formField.getFieldPart());
				}
				buffer.append(UNDERSCORE);
			} 	
		}		
		return buffer.toString();		
	}
	
	
	/**
	 * Write out a single column header to the export buffer.
	 * 
	 * @param buffer
	 * @param fieldNumber
	 * @param concept
	 * @param isLast
	 */
	public void addColumnHeader(StringBuffer exportBuffer, String columnHeader, boolean isLast) { 
				
		// Add quotes around the column header
		exportBuffer.append(DEFAULT_QUOTE);		
		exportBuffer.append(columnHeader);
		exportBuffer.append(DEFAULT_QUOTE);

		exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
		
		exportBuffer.append(DEFAULT_QUOTE);		
		exportBuffer.append(columnHeader + "_DATE");
		exportBuffer.append(DEFAULT_QUOTE);
		
		// Add comma if this isn't the last comma
		if (!isLast) exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
		
	}		

	/**
	 * Write all columns of data.  Columns are separated by commas, rows are separated by newlines.
	 * 
	 * @param form
	 * @param dataMap
	 * @param fieldMap
	 * @param exportBuffer
	 */
	public void writeColumnData(
			Form form, 
			Map<Encounter, Map<Concept, List<Obs>>> dataMap,
			Map<Concept, Integer> fieldMap, 
			StringBuffer exportBuffer) { 
		
		// Write out all of the rows
		for (Encounter row : dataMap.keySet()) { 			
			
			// Add static column data 
			Patient patient = row.getPatient();
			PatientIdentifier identifier = patient.getPatientIdentifier("IMB ID");
			exportBuffer.append(DEFAULT_QUOTE).append(row.getEncounterId()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);			
			exportBuffer.append(DEFAULT_QUOTE).append(DATE_FORMATTER.format(row.getEncounterDatetime())).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
			exportBuffer.append(DEFAULT_QUOTE).append(patient.getPatientId()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);			
			exportBuffer.append(DEFAULT_QUOTE).append(identifier!=null?identifier.getIdentifier():"").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
			
			
			Map<Concept, List<Obs>> columnData = dataMap.get(row);			
					
			TreeMap<Integer, TreeSet<FormField>> formStructure = 
				FormUtil.getFormStructure(form);
			
			// Iterate over high level nodes
			for(Integer id : formStructure.keySet()) { 

				// Iterate over the actual questions
				TreeSet<FormField> formFields = formStructure.get(id);
				
				// Iterate through the fields in the form and add the concepts to the export		
				for (FormField formField : formFields) { 
	
					if ("Concept".equals(formField.getField().getFieldType().getName())) { 

						Concept concept = formField.getField().getConcept();

						
						if (concept != null) { 

							int numberOfOccurrences = (fieldMap.get(concept)!=null)?fieldMap.get(concept):1;		
		
							for (int i=1; i<=numberOfOccurrences; i++) { 
							
								
								
								// If we encounter a concept set, we ignore it because we run into it later
								if (concept.getConceptSets() != null && !concept.getConceptSets().isEmpty()) { 
									/*
									// Then we iterate over concepts in concept set and display values
									for (ConceptSet cs : formField.getField().getConcept().getConceptSets()) { 
													

										exportBuffer.
											append(DEFAULT_QUOTE);
																					
										// If the observation list is not empty, we'll write the first observation we find in the list 
										List<Obs> obsList = columnData.get(cs.getConcept());
										if (obsList != null && !obsList.isEmpty()) {	
											exportBuffer.append(obsList.get(0).getValueAsString(Locale.US));

											// NOTE - we need to remove observations from this list as we write them to the export buffer
											obsList.remove(0);										
										} 
										
										exportBuffer.											
											append(DEFAULT_QUOTE).
											append(DEFAULT_COLUMN_SEPARATOR);
									}*/
								} 
								else {
								
								
	
									// If the observation list is not empty, display the first value 
									Obs obs = null;
									
									List<Obs> obsList = columnData.get(concept);
									if (obsList != null && !obsList.isEmpty()) {																	
										obs = obsList.get(0);
										// NOTE - we need to remove observations from this list as we write 
										// them to the export buffer
										//
										// TODO Figure out why we have to do this?
										obsList.remove(0);
									}
									
									
									// Write out the observation value  
									exportBuffer.append(DEFAULT_QUOTE);
									exportBuffer.append((obs != null)?obs.getValueAsString(Locale.US):"");
									exportBuffer.append(DEFAULT_QUOTE);		
									
									exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
									
									// Write out observation date
									exportBuffer.append(DEFAULT_QUOTE);		
									exportBuffer.append((obs!=null)?OpenmrsUtil.getDateFormat().format(obs.getObsDatetime()):"");									
									exportBuffer.append(DEFAULT_QUOTE);

									exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
								
								}															
							}
						}
					}
				}
			}
			exportBuffer.append("\n");
		}		
	}


	
	/* (non-Javadoc)
	 * @see org.openmrs.module.formdataexport.FormDataExportService#countForms(org.openmrs.Cohort)
	 */
	public Map<Form, Integer> countForms(Cohort patients) {
		return dao.countForms(patients);
	}



	
	
	
	/**
	 * Write out the form column headers for the export.
	 * 
	 * @param form
	 * @param exportBuffer
	public void writeFormColumnData(Form form, StringBuffer exportBuffer) { 
		
		TreeMap<Integer, TreeSet<FormField>> formStructure = 
			FormUtil.getFormStructure(form);
		
		StringBuffer fieldNumber = new StringBuffer();
		
		// Iterate over high level nodes
		for(Integer id : formStructure.keySet()) { 

			// Iterate over the actual questions
			TreeSet<FormField> formFields = formStructure.get(id);
				
			for(FormField formField : formFields) { 				
				Field field = formField.getField();
				
				if ("Concept".equals(field.getFieldType().getName())) { 
					
					// Get the field number and part
					if (formField.getFieldNumber()!=null) { 
						fieldNumber.append(formField.getFieldNumber());
						if (formField.getFieldPart()!=null) 
							fieldNumber.append(formField.getFieldPart());
					}
					
					// Check if concept exists (it should)
					if (field.getConcept() != null) { 

						// Create a column name based on the concept
						Concept concept = field.getConcept();
						addColumnHeader(exportBuffer, fieldNumber.toString(), concept, false);
						
						if ( concept.getConceptSets()!= null) { 
							for (ConceptSet cs : formField.getField().getConcept().getConceptSets()) { 
								addColumnHeader(exportBuffer, fieldNumber.toString(), cs.getConcept(), false);

							}							
						} 
					}
				}
				fieldNumber.setLength(0);
				
			}
			
		}
		
		log.info("Column Buffer: " + exportBuffer.toString());		
	}
		 */

	
	
	/**
	 * Adds columns to the based on concepts related to the specified form.
	 * 
	 * @param export
	 * @param form
	 * @param extras
	public void addFormColumns(DataExportReportObject export, Form form, String [] extras) { 
		
		// Calculate the number of times each concept appears in the form
		Map<Concept, Integer> hitsByConcept = getHitsByConcept(form);
		
		//Order the fields by their field number and field part
		//Comparator<FormField> comparator = new FormFieldComparator();
		List<FormField> formFields = new LinkedList<FormField>();
		formFields.addAll(form.getFormFields());
		Collections.sort(formFields);			

		StringBuffer columnName = new StringBuffer();

		// Iterate through the fields in the form and add the concepts to the export		
		if ( formFields != null ) { 
			for (FormField formField : formFields) { 
				Concept concept = formField.getField().getConcept();
								
				if (concept != null) { 

					// Figure out how many times this concept occurs for this form
					Integer hits = hitsByConcept.get(concept); 
					
					// Reset column name buffer
					columnName.setLength(0);

					// Get column name
					
					
					
					// Add the first level of concepts in a concept set (sub-questions)
					// Append the column names with concept ID to differentiate since the column name
					// will start with the column name of the top-level question
					Collection<ConceptSet> conceptSets = concept.getConceptSets();
					if (conceptSets != null && !conceptSets.isEmpty()) { 
						StringBuffer buffer = new StringBuffer();
						for (ConceptSet conceptSet : conceptSets) {
							buffer.setLength(0);							
							buffer.append(getColumnName(formField));
							buffer.append(columnName.toString()).
								append("_").
								append(conceptSet.getConcept().getConceptId());
							
							ConceptColumn column = 
								createColumn(buffer.toString(), conceptSet.getConcept(), hits, extras);
							
							export.getColumns().add(column);
							
						}
					} 
					// Otherwise just add the concept that goes along with it
					else { 
						
						columnName.append(getColumnName(formField));
						
						ConceptColumn column = 
							createColumn(columnName.toString(), concept, hits, extras);
						
						export.getColumns().add(column);
					} 
					hitsByConcept.remove(concept);
				}

			}
		}
	}
	*/


	
	/**
	 * Create a column based on the concept representing the question that is being asked.  
	 * 
	 * @param name
	 * @param concept
	 * @param occurs
	 * @param extras
	 * @return
	public ConceptColumn createColumn(String name, Concept concept, Integer occurs, String [] extras) { 
		ConceptColumn column = null;
		System.out.println("Name: " + name + ", Concept: " + concept + ", Occurs: " + occurs);
								
			column = new ConceptColumn();
			column.setColumnName(name.toString());
			column.setConceptId(concept.getConceptId());
			column.setExtras(extras);				
	
			if (occurs != null && occurs > 1) { 				
				column.setModifier(DataExportReportObject.MODIFIER_LAST_NUM);
				column.setModifierNum(occurs);	
			} 
			else { 
				column.setModifier(DataExportReportObject.MODIFIER_LAST);										
			}
		return column;
	}
	 */
		
	
	
	
	/**
	 * Creates a map of concepts with the number of times they appear in a given form.
	 * 
	 * TODO Currently only supports high level concepts.
	 * 
	 * @param 	form	the form to process
	 * @return	a map that keeps track of the number of times a concept occurs in a form
	public Map<Concept, Integer> getHitsByConcept(Form form) { 
		Map<Concept, Integer> map = new HashMap<Concept, Integer>();

		System.out.println("There are " + form.getFormFields().size() + " fields on this form");
		for (FormField formField : form.getFormFields()) {
					
			
			Concept concept = formField.getField().getConcept();
			if (concept != null) { 				
				System.out.println("Concept " + concept.getName().getShortName());
				if (concept.getConceptSets() != null && !concept.getConceptSets().isEmpty()) { 
					for (ConceptSet set : concept.getConceptSets()) { 						
						Concept child = set.getConcept();
						System.out.println("\tConcept " + child.getName().getShortName());
						Integer hits = map.get(child);
						if (hits == null) hits = new Integer(0);
						hits++;
						map.put(concept, hits);					
					}					
				} 
				else { 
					Integer hits = map.get(concept);
					if (hits == null) hits = new Integer(0);
					hits++;
					map.put(concept, hits);
				}
			}
		}
		return map;
	}
	 */

	
	/**
	 * 
	 * @param formField
	 * @return
	public String getColumnName(FormField formField) { 
		
		StringBuffer buffer = new StringBuffer();

		// Add field number and field part if they exist
		if (formField.getFieldNumber() != null ) {
			buffer.append(formField.getFieldNumber());						
			if (formField.getFieldPart() != null ) 
				buffer.append(formField.getFieldPart());							
			buffer.append(" ");
		}
		
		try { 
			String shortName = 
				formField.getField().getConcept().getName().getShortName();


			if (shortName != null && !shortName.isEmpty() ) 
				buffer.append(shortName);
			else  
				buffer.append("Concept ").append(formField.getField().getConcept().getConceptId());
							
		}
		catch (Exception e) { 
			log.info("Concept short name is null, using field number/field part or concept name");			
		}

		
		return buffer.toString();
		
	}
	 */	
	
	
	/*
	public File exportFormData(Form form, PatientSet patients, String [] extraColumns) throws Exception { 
		
		//Form form = Context.getFormService().getForm(formId);
		
		
		if ( form == null ) {
			throw new Exception("Form cannot be null");
		} 
		else {

			// Create new export
			DataExportReportObject export = new DataExportReportObject();
			export.setName(FORM_DATA_EXPORT_PREFIX + "_" + form.getName());

			// Populate export with columns
			addStaticColumns(export);
			
			// Populate export with columns from the form
			addFormColumns(export, form, extraColumns);
			
			// If patients not passed in, set the cohort to all patients having encounters
			if (patients == null || patients.getSize() <= 0)				
				patients = getPatientsHavingEncounters(form);

			// If patients is still null, set cohort to all patients
			if (patients == null || patients.getSize() <= 0)
				patients = Context.getPatientSetService().getAllPatients();			
			
			log.info("Patients: " + patients);
			
			// Export the data as a TSV file
			DataExportUtil.generateExport(export, patients);
			File tempExportFile = DataExportUtil.getGeneratedFile(export);
			
			// Convert the file to CSV
			String tsv = tempExportFile.getCanonicalPath();
			String csv = FormDataExportUtil.changeExtension(tsv, "csv");
			FormDataExportUtil.convertFormat(tsv, "\t", csv, ",");
			
			return new File(csv);
			
			
		}		
		
	}
	*/	
	

}

/**
 * 
 */
class FormFieldComparator implements Comparator<FormField> { 
	public int compare(FormField ff1, FormField ff2) {
		int fieldNumber1 = ff1.getFieldNumber()!=null ? ff1.getFieldNumber() : 0;
		int fieldNumber2 = ff2.getFieldNumber()!=null ? ff2.getFieldNumber() : 0;
		int compare = fieldNumber1 - fieldNumber2;
		if (compare == 0) { 
			String fieldPart1 = ff1.getFieldPart()!=null ? ff1.getFieldPart() : "";
			String fieldPart2 = ff2.getFieldPart()!=null ? ff2.getFieldPart() : "";
			compare = fieldPart1.compareTo(fieldPart2);
		}
		return  compare;
	}			 
}			
