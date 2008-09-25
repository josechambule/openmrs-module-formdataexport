package org.openmrs.module.formdataexport.test;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.cohort.CohortDefinitionItemHolder;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class FormDataExportTest extends BaseModuleContextSensitiveTest {

	private Log log = LogFactory.getLog(this.getClass());
	
	
	@Override
	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();		
		authenticate();
	}

	
	/**
     * @see org.openmrs.BaseContextSensitiveTest#useInMemoryDatabase()
     */
    @Override
    public Boolean useInMemoryDatabase() {
	    // TODO Auto-generated method stub
	    return false;
    }
    
    
    /**
     * 
     * @throws Exception
     */
    public void testCohort() throws Exception { 
    	
    	// Iterate over all cohort definitions
    	List<CohortDefinitionItemHolder> cohortDefinitions = 
    		Context.getCohortService().getAllCohortDefinitions();
    	
    	log.info("# cohort definitions: " + cohortDefinitions.size());
    	for (CohortDefinitionItemHolder cohortItem : cohortDefinitions) {     		

    		log.info("==================  " + cohortItem.getKey() + " = " + cohortItem.getName() + " ==================");
    		
    		CohortDefinition definition = Context.getCohortService().getCohortDefinition(cohortItem.getKey());
    		log.info("Cohort definition: " + definition.getClass());

    		Cohort cohort = Context.getCohortService().evaluate(definition, null);
    		log.info("Cohort: " + cohort.getName() + " has " + cohort.getSize() + " patients");
    		
    	}
    	
    	CohortDefinition allPatientsCohortDefinition = Context.getCohortService().getAllPatientsCohortDefinition();
    	Cohort allPatientsCohortFromDefinition = Context.getCohortService().evaluate(allPatientsCohortDefinition, null);
    	log.info("All patients cohort (from definition): " + allPatientsCohortFromDefinition.getName() + " " + allPatientsCohortFromDefinition.getSize());
    	
    	Cohort allPatientsCohortFromPatientSetService = Context.getPatientSetService().getAllPatients();   	
    	log.info("All patients cohort (direct): " + allPatientsCohortFromPatientSetService.getName() + " " + allPatientsCohortFromPatientSetService.getSize());
    	    	
    	
    	assert(allPatientsCohortFromPatientSetService.getSize() == allPatientsCohortFromDefinition.getSize());
    	
    }
	
    
    /**
     * Test the form data export service.
     * 
     * @throws Exception
     */
	public void testFormDataExport() throws Exception { 
		log.info("test form data export");
		
		String [] extraColumns = null; //{ "obsDatetime" };
		String cohortToken = "1:org.openmrs.cohort.StaticCohortDefinition";    	
    	Form form = Context.getFormService().getForm(new Integer(29));
		
    	File exportFile = 
    		((FormDataExportService) 
    				Context.getService(FormDataExportService.class)).exportEncounterData(form, cohortToken, extraColumns);

		log.info("Export data to " + exportFile.getAbsolutePath());
	}

	
	/*
	public void testConceptHits() throws Exception { 
		
		FormDataExportService service = 
			(FormDataExportService) Context.getService(FormDataExportService.class);	

		Form form = Context.getFormService().getForm(new Integer(42));
		
		Map<Concept, Integer> hitsByConcept = service.getHitsByConcept(form);
		
		System.out.println("\n\n");
		for(Concept concept : hitsByConcept.keySet()) { 
			Integer hits = hitsByConcept.get(concept);
			System.out.println("Concept " + concept.getName().getName() + " has " + hits);
		}
		System.out.println("There are " + hitsByConcept.keySet().size() + " unique concepts ");
		
	}
	*/
	
	
	/*
	public void testPostProcessExport() throws Exception { 		
		
		
		Form form = Context.getFormService().getForm(42);
		
		form.getName();
		String tsvFilename = 
			"C:\\Documents and Settings\\Justin Miranda\\Application Data\\OpenMRS\\dataExports\\FORM_EXPORT_en_us";		
		
		String csvFilename = 
			FormDataExportUtil.changeExtension(tsvFilename, ".csv");		
		
		FormDataExportUtil.convertFormat(tsvFilename, "\t", csvFilename, ",");
		log.info("Saved file to: " + csvFilename);
	}
	*/
	
	/*
	public void testComputeAge() throws Exception { 
		
		int age = 0;
		Date fromDate = null;
		
		// Define birth date
		Calendar calendar = Calendar.getInstance();		
		calendar.set(Calendar.YEAR, 1978);
		calendar.set(Calendar.DAY_OF_MONTH, 8);
		calendar.set(Calendar.MONTH, 2);
		Date birthDate = calendar.getTime();
		
		// Simply birthday calculation (on birthday)
		calendar.set(Calendar.YEAR, 2007);
		fromDate = calendar.getTime();		
		age = FormDataExportUtil.calculateYearsBetween(fromDate, birthDate);		
		assert(age == 29);

		// Day before birthday
		calendar.set(Calendar.YEAR, 2007);
		calendar.set(Calendar.DAY_OF_MONTH, 7);
		calendar.set(Calendar.DAY_OF_MONTH, 2);
		fromDate = calendar.getTime();		
		age = FormDataExportUtil.calculateYearsBetween(fromDate, birthDate);		
		assert(age == 28);		
	}
	*/
	
	

	
	
	/*
	public void testRenameFile() throws Exception { 	
		String tsvFilename = 
			"C:\\Documents and Settings\\Justin Miranda\\Application Data\\OpenMRS\\dataExports\\FORM_EXPORT_en_us";
		
		String csvFilename = FormDataExportUtil.changeExtension(tsvFilename, "csv");		
		System.out.println("filename: " + csvFilename);
	}
	*/
	
	/*
	public void testGetForms() throws Exception {
		
		
		
		try { 
			List<Form> forms = Context.getFormService().getForms();
			System.out.println("All forms: " + forms);
			forms.clear();
			
			Form form = Context.getFormService().getForm(2);					
			DataExportReportObject export = new DataExportReportObject();
			export.setSeparator(",");
			export.setUseQuotes(new Boolean(true));
			export.setName("FORM_EXPORT_" + form.getName());
			if ( form != null ) { 
				System.out.println(
						"*********************************************\n" +
						"Form " + form.getName() + "\n" +
						"*********************************************");				
				SimpleColumn patientId = new SimpleColumn();
				patientId.setColumnName("PATIENT_ID");
				patientId.setReturnValue("$!{fn.patientId}");
				export.getColumns().add(patientId);
				
				SimpleColumn gender = new SimpleColumn();
				gender.setColumnName("GENDER");
				gender.setReturnValue("$!{fn.getPatientAttr('Person', 'gender')}");
				export.getColumns().add(gender);
	
				SimpleColumn location = new SimpleColumn();
				location.setColumnName("LOCATION");
				location.setReturnValue("$!{fn.getPatientAttr('PatientIdentifier', 'location').getName()}");
				export.getColumns().add(location);
	
				SimpleColumn lastEncounterDate = new SimpleColumn();
				lastEncounterDate.setColumnName("LATEST_ENCOUNTER_DATE");
				lastEncounterDate.setReturnValue("$!{fn.formatDate('short', $!{fn.getLastEncounterAttr([''], 'encounterDatetime')})}");
				export.getColumns().add(lastEncounterDate);
				
				// Order the fields by their field number and field part
				Comparator<FormField> comparator = new Comparator<FormField>() {					 
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
				};				
				List<FormField> formFields = new LinkedList<FormField>();
				formFields.addAll(form.getFormFields());
				Collections.sort(formFields, comparator);
				
				
				// Create an occurrence mapping in the 
				Map<Concept, Integer> conceptOccurrence = new HashMap<Concept, Integer>();
				if ( form.getFormFields() != null ) { 
					for (FormField formField : form.getFormFields()) { 
						if (formField.getField() != null) { 
							Concept concept = formField.getField().getConcept();
							if (concept != null) { 
								Integer occurs = conceptOccurrence.get(concept);
								if (occurs == null) occurs = new Integer(0);
								occurs++;
								conceptOccurrence.put(concept, occurs);
							}
						}
					}
				}
				
				if (formFields != null) {
				
					for (FormField formField : formFields) { 
						
						String [] extras = { "obsDatetime" };
						Concept concept = formField.getField().getConcept();
						
						StringBuffer columnName = new StringBuffer();
						if (formField.getFieldNumber() != null ) 
							columnName.append(formField.getFieldNumber());
						if (formField.getFieldPart() != null ) 
							columnName.append(formField.getFieldPart());
											
						if (concept != null) { 
							
							
							Integer occurs = conceptOccurrence.get(concept); 
							
							
							if (columnName.length()==0){ 
								columnName.append("CONCEPT").append(concept.getConceptId());
								columnName.append("_").append(formField.getFormFieldId());
								
							}
							
							
							System.out.println("Column name: " + columnName.toString());
							
							
							// Add the first level of concepts in a concept set
							Collection<ConceptSet> conceptSets = concept.getConceptSets();
							if (conceptSets != null && !conceptSets.isEmpty()) { 
								for (ConceptSet conceptSet : conceptSets) { 

									
									if ( occurs != null) {
										ConceptColumn column = new ConceptColumn();
										Concept childConcept = conceptSet.getConcept();
																	
										column.setColumnName(columnName.toString());
										
										//column.setColumnName(childConcept.getName().getName());
										column.setConceptId(childConcept.getConceptId());
										column.setExtras(extras);				
									
										if (occurs > 1) { 
											column.setModifier("mostRecentNum");
											column.setModifierNum(occurs);	
										} 
										else { 
											column.setModifier("any");										
										}
										export.getColumns().add(column);
									} 
									
									

									
									
								}
							} 
							// Otherwise just add the concept that goes along with it
							else { 
								if ( occurs != null) {

									ConceptColumn column = new ConceptColumn();
									column.setColumnName(columnName.toString());
									//column.setColumnName(concept.getName().getName());
									column.setConceptId(concept.getConceptId());
									column.setExtras(extras);	
								
									if (occurs > 1) { 
										column.setModifier("mostRecentNum");
										column.setModifierNum(occurs);	
									} 
									else { 
										column.setModifier("any");										
									}
									export.getColumns().add(column);	
								} 
								
															
							}
				
							// Don't display it again
							conceptOccurrence.put(concept, null);
						}					
					}
				}
			}
			
			// Get patients that 
			PatientSet patients = 
				Context.getPatientSetService().getPatientsHavingEncounters(form.getEncounterType(), null, form, null, null, null, null);
			
			//Map<Integer, Encounter> encounters = Context.getPatientSetService().
			//	getEncountersByType(patients, form.getEncounterType());
			
			
			//System.out.println("Patients: " + patients);
			
			//System.out.println("Template: " + export.generateTemplate());
			
			DataExportUtil.generateExport(export, patients, ",");
			File exportFile = DataExportUtil.getGeneratedFile(export);
			//System.out.println("Export File " + exportFile);		
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}	
		*/
}