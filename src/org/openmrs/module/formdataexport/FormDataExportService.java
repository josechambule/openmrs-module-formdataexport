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

	
	/**
	 * Exports the encounter data with columns from the given form and rows constrained by the given cohort.
	 * 
	 * @param	form	represents the columns (form fields) to be exported
	 * @parma	cohort	represents the rows (patients) to be exported
	 * @param	extras	the extra columns to include for each column
	 */
	@Transactional(readOnly=true)
	public File exportEncounterData(Form form, Cohort cohort, String [] extras);
	
		
	/**
	 * 
	 * @param form
	 * @param cohortKey
	 * @param extras
	 * @return
	 */
	@Transactional(readOnly=true)
	public File exportEncounterData(Form form, String cohortKey, String [] extras);
	
	
	/**
	 * Returns a cohort of patients that have an encounter associated with the given form.
	 * 
	 * @param	form	the form 
	 */
	//public Cohort getPatientsHavingEncounters(Form form);
	
	
	/**
	 * Returns a count of the number of forms that have been submitted for the given cohort.
	 * 
	 * @param cohort
	 * @return
	 */
	@Transactional(readOnly=true)
	public Map<Form, Integer> countForms(Cohort cohort);
	
}
