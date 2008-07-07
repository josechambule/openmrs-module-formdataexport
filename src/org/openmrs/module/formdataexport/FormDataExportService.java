package org.openmrs.module.formdataexport;

import java.io.File;
import java.util.Map;

import org.openmrs.Cohort;
import org.openmrs.Form;
import org.springframework.transaction.annotation.Transactional;

/**
 * Form data export service.
 * 
 * @author Justin Miranda
 * @version 1.0
 */
@Transactional
public interface FormDataExportService {

	//public File exportFormData(Form form, Cohort cohort, String[] extras) throws Exception;

	//public File exportFormData(Form form, PatientSet patients, String[] extras) throws Exception;

	@Transactional(readOnly=true)
	public File exportEncounterData(Form form, Cohort cohort, String [] extras);
	
	//public Map<Concept, Integer> getHitsByConcept(Form form) throws Exception;
		
	// TODO Removed "select cohort" feature in order to simplify the module (need to restore)
	//public PatientSet getPatientsHavingEncounters(Form form);
	
	@Transactional(readOnly=true)
	public Map<Form, Integer> countForms(Cohort cohort);
	
}
